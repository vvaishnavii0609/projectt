package com.demo.hdfcnetbankingsimulator.gateway.service;

import com.demo.hdfcnetbankingsimulator.common.EpiConstants;
import com.demo.hdfcnetbankingsimulator.common.util.ChecksumUtil;
import com.demo.hdfcnetbankingsimulator.gateway.dto.CallbackPayload;
import com.demo.hdfcnetbankingsimulator.gateway.dto.CheckoutPaymentRequest;
import com.demo.hdfcnetbankingsimulator.gateway.dto.HdfcPaymentRequest;
import com.demo.hdfcnetbankingsimulator.gateway.dto.PaymentInitiationResponse;
import com.demo.hdfcnetbankingsimulator.gateway.dto.VerificationResponse;
import com.demo.hdfcnetbankingsimulator.gateway.model.PaymentTransaction;
import com.demo.hdfcnetbankingsimulator.gateway.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GatewayService {

    private static final DateTimeFormatter EPI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Set<String> TERMINAL_STATUSES = Set.of("SUCCESS", "FAILURE");
    private static final String BANK_BASE_URL = "http://localhost:9292";

    @Autowired
    private PaymentTransactionRepository repository;

    @Autowired
    private BankClient bankClient;

    public GatewayService() {
    }

//    public GatewayService(PaymentTransactionRepository repository, BankClient bankClient) {
//        this.repository = repository;
//        this.bankClient = bankClient;
//    }

    public PaymentInitiationResponse initiatePayment(CheckoutPaymentRequest request) {

        validateRequest(request);

        String merchantRefNo = "REF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String transactionId = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        String date = LocalDateTime.now().format(EPI_DATE_FORMAT);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(request.getOrderId());
        transaction.setTransactionId(transactionId);
        transaction.setClientCode(EpiConstants.CLIENT_CODE);
        transaction.setMerchantCode(EpiConstants.MERCHANT_CODE);
        transaction.setMerchantRefNo(merchantRefNo);
        transaction.setDate(date);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency("INR");
        transaction.setTxnScAmount("0.00");
        transaction.setSuccessStaticFlag("N");
        transaction.setFailureStaticFlag("N");
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setBankId(request.getBankId());
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        repository.save(transaction);
        System.out.println("[GATEWAY] SAVED payment_transaction orderId=" + request.getOrderId()
                + " transactionId=" + transactionId + " merchantRefNo=" + merchantRefNo + " status=PENDING");

        HdfcPaymentRequest hdfcRequest = new HdfcPaymentRequest();
        hdfcRequest.setClientCode(EpiConstants.CLIENT_CODE);
        hdfcRequest.setMerchantCode(EpiConstants.MERCHANT_CODE);
        hdfcRequest.setTxnCurrency("INR");
        hdfcRequest.setTxnAmount(request.getAmount());
        hdfcRequest.setTxnScAmount("0.00");
        hdfcRequest.setMerchantRefNo(merchantRefNo);
        hdfcRequest.setSuccessStaticFlag("N");
        hdfcRequest.setFailureStaticFlag("N");
        hdfcRequest.setDate(date);
        hdfcRequest.setRef1(request.getOrderId());


        hdfcRequest.setRef2("");
        hdfcRequest.setRef3("");
        hdfcRequest.setRef4("");
        hdfcRequest.setRef5("");
        hdfcRequest.setRef6("");
        hdfcRequest.setRef7("");
        hdfcRequest.setRef8("");
        hdfcRequest.setRef9("");
        hdfcRequest.setRef10("");
        hdfcRequest.setRef11("");
        hdfcRequest.setDate1("");
        hdfcRequest.setDate2("");
        hdfcRequest.setDisplayDetails("");
        hdfcRequest.setDetails1("");
        hdfcRequest.setDetails2("");
        hdfcRequest.setDetails3("");

        hdfcRequest.setDynamicUrl(BANK_BASE_URL + "/gateway/callback");

        hdfcRequest.setCheckSum(ChecksumUtil.computeForwardChecksum(hdfcRequest, EpiConstants.CHECKSUM_KEY));

        String redirectUrl = buildBankUrl(hdfcRequest);

        return new PaymentInitiationResponse(
                transactionId, request.getOrderId(), request.getAmount(), "INR", "PENDING", redirectUrl);
    }

    private String buildBankUrl(HdfcPaymentRequest r) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(BANK_BASE_URL + "/netbanking/merchant");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("ClientCode", r.getClientCode());
        params.put("MerchantCode", r.getMerchantCode());
        params.put("TxnCurrency", r.getTxnCurrency());
        params.put("TxnAmount", r.getTxnAmount());
        params.put("TxnScAmount", r.getTxnScAmount());
        params.put("MerchantRefNo", r.getMerchantRefNo());
        params.put("SuccessStaticFlag", r.getSuccessStaticFlag());
        params.put("FailureStaticFlag", r.getFailureStaticFlag());
        params.put("Date", r.getDate());
        params.put("Ref1", r.getRef1());
        params.put("CheckSum", r.getCheckSum());
        params.put("DynamicUrl", r.getDynamicUrl());
        params.forEach(b::queryParam);
        return b.build().encode().toUriString();
    }

    /** "Bank Backend -> Gateway Backend" callback receiver. Never the final word -
     *  immediately fires the S2S Verify call and treats THAT as authoritative.
     *  Also where duplicate-callback idempotency is enforced. */
    public String handleBankCallback(CallbackPayload payload) {

        PaymentTransaction transaction = repository.findByMerchantRefNo(payload.getMerchantRefNo()).orElse(null);
        if (transaction == null) {
            System.out.println("[GATEWAY] no PaymentTransaction found for merchantRefNo="
                    + payload.getMerchantRefNo() + " - ignoring callback");
            return "UNKNOWN_TRANSACTION";
        }
        if (TERMINAL_STATUSES.contains(transaction.getStatus())) {
            System.out.println("[GATEWAY] IDEMPOTENCY GUARD: " + transaction.getMerchantRefNo()
                    + " is already " + transaction.getStatus() + " - this callback is ignored");
            return "ALREADY_PROCESSED:" + transaction.getStatus();
        }
        return reconcileWithBank(transaction);
    }

    /** "Payment Gateway -> Merchant: Final Payment Status", with an on-demand Verify
     *  if nothing conclusive has arrived yet (this is what recovers a dropped callback). */
    public PaymentTransaction getStatus(String orderId) {
        PaymentTransaction transaction = repository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!TERMINAL_STATUSES.contains(transaction.getStatus())) {
            reconcileWithBank(transaction);
        }
        return transaction;
    }

    /** Kept for direct lookups (e.g. tests, admin/debug use) - not on the live browser
     *  path anymore now that the bank redirects straight to the merchant via Ref1=orderId. */
    public PaymentTransaction findByMerchantRefNo(String merchantRefNo) {
        return repository.findByMerchantRefNo(merchantRefNo).orElse(null);
    }

    /**
     * The actual S2S "Status Inquiry" call, shared by both the callback handler and
     * on-demand checks. Implements everything your own theory doc's step 25/29-6 asks for:
     *   1. Recompute and validate the response checksum (catches in-transit tampering)
     *   2. Confirm the echoed ClientCode/MerchantRefNo actually match this transaction
     *   3. Confirm the echoed TxnAmount matches what WE originally sent (catches
     *      amount-tampering even if the checksum itself is internally consistent)
     *   4. Only THEN translate flgSuccess into a final status
     * Any failure of 1-3 means we do NOT trust this response - the transaction is left
     * as-is (retryable later) rather than being marked FAILURE from bad data.
     */
    private String reconcileWithBank(PaymentTransaction transaction) {
        try {
            VerificationResponse verify = bankClient.verify(transaction);

            if (verify == null || verify.getFlgSuccess() == null || verify.getFlgSuccess().isBlank()) {
                return "PENDING_VERIFICATION"; // bank has no record (yet) - try again later
            }

            // --- (1) checksum integrity: recompute over the fields the bank ACTUALLY echoed ---
            String expectedChecksum = ChecksumUtil.computeReturnChecksum(
                    verify.getClientCode(), verify.getMerchantCode(), verify.getTxnCurrency(), verify.getTxnAmount(),
                    verify.getTxnScAmount(), verify.getMerchantRefNo(), verify.getSuccessStaticFlag(),
                    verify.getFailureStaticFlag(), verify.getDate(), verify.getBankRefNo(), verify.getMessage(),
                    EpiConstants.CHECKSUM_KEY);

            if (verify.getCheckSum() == null || !expectedChecksum.equals(verify.getCheckSum())) {
                System.out.println("[GatewayService] VERIFY CHECKSUM MISMATCH for " + transaction.getMerchantRefNo()
                        + " - expected " + expectedChecksum + " but got " + verify.getCheckSum());
                transaction.setMessage("Verification response failed checksum validation - not trusted");
                transaction.setUpdatedAt(LocalDateTime.now());
                repository.save(transaction);
                return "VERIFY_CHECKSUM_MISMATCH";
            }

            // --- (2) identity: is this actually a response about OUR transaction? ---
            if (!transaction.getMerchantRefNo().equals(verify.getMerchantRefNo())
                    || !transaction.getClientCode().equals(verify.getClientCode())) {
                System.out.println("[GatewayService] VERIFY IDENTITY MISMATCH for " + transaction.getMerchantRefNo());
                transaction.setMessage("Verification response identity mismatch - not trusted");
                transaction.setUpdatedAt(LocalDateTime.now());
                repository.save(transaction);
                return "VERIFY_IDENTITY_MISMATCH";
            }

            // --- (3) amount: does the bank's confirmed amount match what we sent? ---
            BigDecimal expectedAmount = new BigDecimal(transaction.getAmount());
            BigDecimal actualAmount = new BigDecimal(verify.getTxnAmount());
            if (expectedAmount.compareTo(actualAmount) != 0) {
                System.out.println("[GatewayService] AMOUNT MISMATCH for " + transaction.getMerchantRefNo()
                        + " - expected " + expectedAmount + " but bank confirmed " + actualAmount);
                transaction.setStatus("FAILURE");
                transaction.setBankRefNo("0");
                transaction.setMessage("Amount mismatch: expected " + expectedAmount + " but bank confirmed " + actualAmount);
                transaction.setUpdatedAt(LocalDateTime.now());
                repository.save(transaction);
                return "PROCESSED:AMOUNT_MISMATCH";
            }

            // --- (4) all checks passed - translate the bank's verdict into our final status ---
            String status = switch (verify.getFlgSuccess()) {
                case "S" -> "SUCCESS";
                case "F" -> "FAILURE";
                case "P" -> "PENDING";
                default -> "UNKNOWN";
            };

            transaction.setStatus(status);
            transaction.setBankRefNo(verify.getBankRefNo());
            transaction.setMessage(verify.getMessage());
            transaction.setUpdatedAt(LocalDateTime.now());
            repository.save(transaction);
            System.out.println("[GATEWAY] SAVED payment_transaction orderId=" + transaction.getOrderId()
                    + " transactionId=" + transaction.getTransactionId() + " -> status=" + status);

            return "PROCESSED:" + status;
        } catch (Exception e) {
            System.out.println("[GatewayService] verify call failed: " + e.getMessage());
            return "VERIFY_FAILED";
        }
    }

    private void validateRequest(CheckoutPaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (request.getAmount() == null || request.getAmount().isBlank()) {
            throw new IllegalArgumentException("Amount is required");
        }
        try {
            double amt = Double.parseDouble(request.getAmount());
            if (amt <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid transaction amount");
        }
        if (request.getPaymentMethod() == null || !request.getPaymentMethod().equalsIgnoreCase("netbanking")) {
            throw new IllegalArgumentException("Only Net Banking is supported");
        }
        if (request.getBankId() == null || !request.getBankId().equalsIgnoreCase("HDFC")) {
            throw new IllegalArgumentException("Only HDFC Bank is supported");
        }
    }
}
