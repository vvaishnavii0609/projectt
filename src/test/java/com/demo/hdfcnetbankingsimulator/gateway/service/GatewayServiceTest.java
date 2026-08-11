package com.demo.hdfcnetbankingsimulator.gateway.service;

import com.demo.hdfcnetbankingsimulator.common.EpiConstants;
import com.demo.hdfcnetbankingsimulator.common.util.ChecksumUtil;
import com.demo.hdfcnetbankingsimulator.gateway.dto.CallbackPayload;
import com.demo.hdfcnetbankingsimulator.gateway.dto.CheckoutPaymentRequest;
import com.demo.hdfcnetbankingsimulator.gateway.dto.VerificationResponse;
import com.demo.hdfcnetbankingsimulator.gateway.model.PaymentTransaction;
import com.demo.hdfcnetbankingsimulator.gateway.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GatewayServiceTest {

    private PaymentTransactionRepository repository;
    private BankClient bankClient;
    private GatewayService gatewayService;

    @BeforeEach
    void setUp() {
        repository = mock(PaymentTransactionRepository.class);
        bankClient = mock(BankClient.class);
        when(repository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        gatewayService = new GatewayService(repository, bankClient);
    }

    private PaymentTransaction pendingTransaction() {
        PaymentTransaction t = new PaymentTransaction();
        t.setOrderId("ORD1");
        t.setClientCode(EpiConstants.CLIENT_CODE);
        t.setMerchantCode(EpiConstants.MERCHANT_CODE);
        t.setMerchantRefNo("REF1");
        t.setDate("25/05/2018 12:00:00");
        t.setAmount("1.79");
        t.setCurrency("INR");
        t.setTxnScAmount("0.00");
        t.setSuccessStaticFlag("N");
        t.setFailureStaticFlag("N");
        t.setStatus("REDIRECTED_TO_BANK");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    /** Builds a VerificationResponse the way the real bank would, with a genuinely
     *  correct self-consistent checksum - the "everything is fine" baseline. */
    private VerificationResponse honestVerifyResponse(PaymentTransaction t, String flgSuccess, String bankRefNo, String message) {
        VerificationResponse r = new VerificationResponse();
        r.setClientCode(t.getClientCode());
        r.setMerchantCode(t.getMerchantCode());
        r.setMerchantRefNo(t.getMerchantRefNo());
        r.setDate(t.getDate());
        r.setTxnAmount(t.getAmount());
        r.setTxnCurrency(t.getCurrency());
        r.setTxnScAmount(t.getTxnScAmount());
        r.setSuccessStaticFlag(t.getSuccessStaticFlag());
        r.setFailureStaticFlag(t.getFailureStaticFlag());
        r.setFlgSuccess(flgSuccess);
        r.setBankRefNo(bankRefNo);
        r.setMessage(message);
        r.setCheckSum(ChecksumUtil.computeReturnChecksum(
                r.getClientCode(), r.getMerchantCode(), r.getTxnCurrency(), r.getTxnAmount(), r.getTxnScAmount(),
                r.getMerchantRefNo(), r.getSuccessStaticFlag(), r.getFailureStaticFlag(), r.getDate(),
                r.getBankRefNo(), r.getMessage(), EpiConstants.CHECKSUM_KEY));
        return r;
    }

    // ---------------------------------------------------------------
    // initiatePayment() - request validation
    // ---------------------------------------------------------------

    @Test
    void initiatePayment_rejectsZeroAmount() {
        CheckoutPaymentRequest req = new CheckoutPaymentRequest();
        req.setOrderId("ORD1"); req.setAmount("0"); req.setPaymentMethod("netbanking"); req.setBankId("HDF");
        assertThrows(IllegalArgumentException.class, () -> gatewayService.initiatePayment(req));
    }

    @Test
    void initiatePayment_rejectsNonNetbankingMethod() {
        CheckoutPaymentRequest req = new CheckoutPaymentRequest();
        req.setOrderId("ORD1"); req.setAmount("1.79"); req.setPaymentMethod("upi"); req.setBankId("HDF");
        assertThrows(IllegalArgumentException.class, () -> gatewayService.initiatePayment(req));
    }

    @Test
    void initiatePayment_rejectsNonHdfcBank() {
        CheckoutPaymentRequest req = new CheckoutPaymentRequest();
        req.setOrderId("ORD1"); req.setAmount("1.79"); req.setPaymentMethod("netbanking"); req.setBankId("ICICI");
        assertThrows(IllegalArgumentException.class, () -> gatewayService.initiatePayment(req));
    }

    @Test
    void initiatePayment_forAValidRequest_returnsAUrlCarryingOrderIdAsRef1() {
        CheckoutPaymentRequest req = new CheckoutPaymentRequest();
        req.setOrderId("ORD999"); req.setAmount("1.79"); req.setPaymentMethod("netbanking"); req.setBankId("HDFC");

        var response = gatewayService.initiatePayment(req);
        String url = response.getRedirectUrl();

        assertEquals("PENDING", response.getStatus());
        assertEquals("ORD999", response.getOrderId());
        assertNotNull(response.getTransactionId());
        assertTrue(url.contains("/netbanking/merchant"));
        assertTrue(url.contains("Ref1=ORD999"), "Ref1 must carry the orderId so the bank can redirect straight back");
        assertTrue(url.contains("CheckSum="));
        verify(repository).save(any(PaymentTransaction.class));
    }

    // ---------------------------------------------------------------
    // handleBankCallback() -> reconcileWithBank(): the actual trust boundary
    // ---------------------------------------------------------------

    @Test
    void callback_forUnknownTransaction_isIgnoredSafely() {
        when(repository.findByMerchantRefNo("GHOST")).thenReturn(Optional.empty());

        CallbackPayload payload = new CallbackPayload();
        payload.setMerchantRefNo("GHOST");

        assertEquals("UNKNOWN_TRANSACTION", gatewayService.handleBankCallback(payload));
        verifyNoInteractions(bankClient);
    }

    @Test
    void callback_forAlreadyTerminalTransaction_isIdempotent_doesNotCallVerifyAgain() {
        PaymentTransaction t = pendingTransaction();
        t.setStatus("SUCCESS"); // already finalized
        when(repository.findByMerchantRefNo("REF1")).thenReturn(Optional.of(t));

        CallbackPayload payload = new CallbackPayload();
        payload.setMerchantRefNo("REF1");

        String result = gatewayService.handleBankCallback(payload);

        assertEquals("ALREADY_PROCESSED:SUCCESS", result);
        verifyNoInteractions(bankClient); // this is the actual idempotency guarantee
    }

    @Test
    void callback_forPendingTransaction_triggersVerify_andTrustsAnHonestSuccessResponse() {
        PaymentTransaction t = pendingTransaction();
        when(repository.findByMerchantRefNo("REF1")).thenReturn(Optional.of(t));
        when(bankClient.verify(t)).thenReturn(honestVerifyResponse(t, "S", "HDFC12345", ""));

        CallbackPayload payload = new CallbackPayload();
        payload.setMerchantRefNo("REF1");

        String result = gatewayService.handleBankCallback(payload);

        assertEquals("PROCESSED:SUCCESS", result);
        assertEquals("SUCCESS", t.getStatus());
        assertEquals("HDFC12345", t.getBankRefNo());
    }

    @Test
    void verify_withBadChecksum_isRejected_transactionNotFinalized() {
        PaymentTransaction t = pendingTransaction();
        when(repository.findByMerchantRefNo("REF1")).thenReturn(Optional.of(t));

        VerificationResponse tampered = honestVerifyResponse(t, "S", "HDFC12345", "");
        tampered.setCheckSum("0" + tampered.getCheckSum()); // corrupt it
        when(bankClient.verify(t)).thenReturn(tampered);

        CallbackPayload payload = new CallbackPayload();
        payload.setMerchantRefNo("REF1");

        String result = gatewayService.handleBankCallback(payload);

        assertEquals("VERIFY_CHECKSUM_MISMATCH", result);
        assertNotEquals("SUCCESS", t.getStatus(), "a checksum-failed response must never be trusted as SUCCESS");
    }

    @Test
    void verify_withMismatchedAmount_isCaught_transactionMarkedFailure() {
        PaymentTransaction t = pendingTransaction(); // Gateway believes amount is 1.79

        when(repository.findByMerchantRefNo("REF1")).thenReturn(Optional.of(t));
        // Bank honestly (self-consistent checksum) reports a DIFFERENT amount
        VerificationResponse response = honestVerifyResponse(t, "S", "HDFC12345", "");
        response.setTxnAmount("999.99");
        response.setCheckSum(ChecksumUtil.computeReturnChecksum(
                response.getClientCode(), response.getMerchantCode(), response.getTxnCurrency(), "999.99",
                response.getTxnScAmount(), response.getMerchantRefNo(), response.getSuccessStaticFlag(),
                response.getFailureStaticFlag(), response.getDate(), response.getBankRefNo(),
                response.getMessage(), EpiConstants.CHECKSUM_KEY));
        when(bankClient.verify(t)).thenReturn(response);

        CallbackPayload payload = new CallbackPayload();
        payload.setMerchantRefNo("REF1");

        String result = gatewayService.handleBankCallback(payload);

        assertEquals("PROCESSED:AMOUNT_MISMATCH", result);
        assertEquals("FAILURE", t.getStatus(), "a confirmed amount mismatch must never resolve to SUCCESS");
    }

    @Test
    void verify_reportingBlankFlgSuccess_leavesTransactionPending_forLaterRetry() {
        PaymentTransaction t = pendingTransaction();
        when(repository.findByMerchantRefNo("REF1")).thenReturn(Optional.of(t));

        VerificationResponse notFound = new VerificationResponse();
        notFound.setFlgSuccess(""); // bank has no record yet
        when(bankClient.verify(t)).thenReturn(notFound);

        CallbackPayload payload = new CallbackPayload();
        payload.setMerchantRefNo("REF1");

        String result = gatewayService.handleBankCallback(payload);

        assertEquals("PENDING_VERIFICATION", result);
        assertEquals("REDIRECTED_TO_BANK", t.getStatus(), "status must be untouched, ready to retry");
    }

    @Test
    void verify_callThrows_isHandledGracefully_doesNotCrashTheCallbackHandler() {
        PaymentTransaction t = pendingTransaction();
        when(repository.findByMerchantRefNo("REF1")).thenReturn(Optional.of(t));
        when(bankClient.verify(t)).thenThrow(new RuntimeException("connection refused"));

        CallbackPayload payload = new CallbackPayload();
        payload.setMerchantRefNo("REF1");

        assertEquals("VERIFY_FAILED", gatewayService.handleBankCallback(payload));
    }

    // ---------------------------------------------------------------
    // getStatus() - on-demand verify recovers a dropped callback
    // ---------------------------------------------------------------

    @Test
    void getStatus_forNonTerminalTransaction_triggersAnOnDemandVerify() {
        PaymentTransaction t = pendingTransaction(); // never got a callback (DROP scenario)
        when(repository.findByOrderId("ORD1")).thenReturn(Optional.of(t));
        when(bankClient.verify(t)).thenReturn(honestVerifyResponse(t, "S", "HDFC999", ""));

        PaymentTransaction result = gatewayService.getStatus("ORD1");

        assertEquals("SUCCESS", result.getStatus());
        verify(bankClient).verify(t);
    }

    @Test
    void getStatus_forAlreadyTerminalTransaction_doesNotCallVerifyAgain() {
        PaymentTransaction t = pendingTransaction();
        t.setStatus("FAILURE");
        when(repository.findByOrderId("ORD1")).thenReturn(Optional.of(t));

        gatewayService.getStatus("ORD1");

        verifyNoInteractions(bankClient);
    }
}
