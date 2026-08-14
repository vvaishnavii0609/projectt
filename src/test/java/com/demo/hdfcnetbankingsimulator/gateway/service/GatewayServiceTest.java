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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private PaymentTransactionRepository repository;
    @Mock
    private BankClient bankClient;
    @InjectMocks
    private GatewayService gatewayService;


    @Test
    void initiatePayment_createsPendingTransaction() {

        CheckoutPaymentRequest request = new CheckoutPaymentRequest();

        request.setOrderId("ORD123");
        request.setAmount("100.00");
        request.setPaymentMethod("netbanking");
        request.setBankId("HDFC");

        var response = gatewayService.initiatePayment(request);

        assertEquals("ORD123", response.getOrderId());
        assertEquals("100.00", response.getAmount());
        assertEquals("PENDING", response.getStatus());

        verify(repository).save(any(PaymentTransaction.class));
    }

    @Test
    void initiatePayment_wrongPaymentMethod_isRejected() {

        CheckoutPaymentRequest request = new CheckoutPaymentRequest();

        request.setOrderId("ORD123");
        request.setAmount("100.00");
        request.setPaymentMethod("UPI");
        request.setBankId("HDFC");

        assertThrows(
                IllegalArgumentException.class,
                () -> gatewayService.initiatePayment(request)
        );
    }

    @Test
    void callback_success_updatesTransactionToSuccess() {

        PaymentTransaction t = pending();

        when(repository.findByMerchantRefNo("REF123"))
                .thenReturn(Optional.of(t));

        when(bankClient.verify(t))
                .thenReturn(verifyResponse(t, "S", "HDFC123", ""));

        CallbackPayload payload = new CallbackPayload();
        payload.setMerchantRefNo("REF123");

        String result = gatewayService.handleBankCallback(payload);

        assertEquals("PROCESSED:SUCCESS", result);
        assertEquals("SUCCESS", t.getStatus());

        verify(bankClient).verify(t);
    }

    @Test
    void callback_unknownTransaction_isIgnored() {

        when(repository.findByMerchantRefNo("GHOST"))
                .thenReturn(Optional.empty());

        CallbackPayload payload = new CallbackPayload();
        payload.setMerchantRefNo("GHOST");

        String result = gatewayService.handleBankCallback(payload);

        assertEquals("UNKNOWN_TRANSACTION", result);

        verifyNoInteractions(bankClient);
    }

    @Test
    void getStatus_successfulTransaction_doesNotVerifyAgain() {

        PaymentTransaction t = pending();
        t.setStatus("SUCCESS");

        when(repository.findByOrderId("ORD123"))
                .thenReturn(Optional.of(t));

        PaymentTransaction result =
                gatewayService.getStatus("ORD123");

        assertEquals("SUCCESS", result.getStatus());

        verifyNoInteractions(bankClient);
    }

@Test
 void getstatusOrderNotFoundthrowsException()
{
    when (repository.findByOrderId("ORD999")).thenReturn(Optional.empty());

    assertThrows(RuntimeException.class, ()-> gatewayService.getStatus("ORD999"));

    verify(repository).findByOrderId("ORD999");
    verifyNoInteractions(bankClient);

}
    @Test
    void findByMerchantRefNo_transactionNotFound_returnsNull() {
        when(repository.findByMerchantRefNo("REF999")).thenReturn(Optional.empty());
        PaymentTransaction result = gatewayService.findByMerchantRefNo("REF999");

        assertNull(result);

        verify(repository).findByMerchantRefNo("REF999");
        verifyNoInteractions(bankClient);

    }

    // Helper method
    private PaymentTransaction pending() {

        PaymentTransaction t = new PaymentTransaction();

        t.setOrderId("ORD123");
        t.setTransactionId("TXN123");
        t.setClientCode(EpiConstants.CLIENT_CODE);
        t.setMerchantCode(EpiConstants.MERCHANT_CODE);
        t.setMerchantRefNo("REF123");
        t.setDate("12/08/2026 10:00:00");
        t.setAmount("100.00");
        t.setCurrency("INR");
        t.setTxnScAmount("0.00");
        t.setSuccessStaticFlag("N");
        t.setFailureStaticFlag("N");
        t.setStatus("REDIRECTED_TO_BANK");

        return t;
    }


    // Helper method
    private VerificationResponse verifyResponse(
            PaymentTransaction t,
            String status,
            String bankRefNo,
            String message) {

        VerificationResponse r = new VerificationResponse();

        r.setClientCode(t.getClientCode());
        r.setMerchantCode(t.getMerchantCode());
        r.setMerchantRefNo(t.getMerchantRefNo());
        r.setDate(t.getDate());
        r.setTxnAmount(t.getAmount());
        r.setTxnCurrency(t.getCurrency());
        r.setTxnScAmount(t.getTxnScAmount());

        r.setFlgSuccess(status);
        r.setBankRefNo(bankRefNo);
        r.setMessage(message);

        r.setCheckSum(
                ChecksumUtil.computeReturnChecksum(
                        r.getClientCode(),
                        r.getMerchantCode(),
                        r.getTxnCurrency(),
                        r.getTxnAmount(),
                        r.getTxnScAmount(),
                        r.getMerchantRefNo(),
                        r.getSuccessStaticFlag(),
                        r.getFailureStaticFlag(),
                        r.getDate(),
                        r.getBankRefNo(),
                        r.getMessage(),
                        EpiConstants.CHECKSUM_KEY
                )
        );

        return r;
    }
}