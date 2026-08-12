package com.demo.hdfcnetbankingsimulator.bank.service;

import com.demo.hdfcnetbankingsimulator.bank.dto.VerificationResponse;
import com.demo.hdfcnetbankingsimulator.bank.enums.SimulationScenario;
import com.demo.hdfcnetbankingsimulator.bank.enums.TransactionStatus;
import com.demo.hdfcnetbankingsimulator.bank.model.BankTransaction;
import com.demo.hdfcnetbankingsimulator.bank.repository.BankTransactionRepository;
import com.demo.hdfcnetbankingsimulator.common.EpiConstants;
import com.demo.hdfcnetbankingsimulator.common.util.ChecksumUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@Service
public class BankService {

    private final BankTransactionRepository repository;

    public BankService(BankTransactionRepository repository) {
        this.repository = repository;
    }

    public String validate(Map<String, String> params) {
        String[] mandatory = {"ClientCode", "MerchantCode", "TxnCurrency", "TxnAmount", "MerchantRefNo", "Date", "CheckSum"};
        for (String m : mandatory) {
            String v = params.get(m);
            if (v == null || v.isBlank()) return "Mandatory parameter missing: " + m;
        }
        if (!EpiConstants.CLIENT_CODE.equals(params.get("ClientCode"))
                || !EpiConstants.MERCHANT_CODE.equals(params.get("MerchantCode"))) {
            return "Merchant not found";
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(params.get("TxnAmount"));
        } catch (NumberFormatException e) {
            return "Invalid transaction amount";
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return "Invalid transaction amount";
        if (!"INR".equalsIgnoreCase(params.get("TxnCurrency"))) return "Unsupported currency";

        String expectedChecksum = ChecksumUtil.computeForwardChecksum(params, EpiConstants.CHECKSUM_KEY);
        if (!expectedChecksum.equals(params.get("CheckSum"))) return "Checksum validation failed";

        if (successAlreadyExistsFor(params.get("ClientCode"), params.get("MerchantRefNo"), params.get("Date"))) {
            return "Duplicate transaction";
        }
        return null;
    }

    private boolean successAlreadyExistsFor(String clientCode, String merchantRefNo, String date) {
        return repository.findByMerchantRefNo(merchantRefNo)
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> clientCode.equals(t.getClientCode()) && date.equals(t.getTransactionDate()))
                .isPresent();
    }

    public BankTransaction accept(Map<String, String> params) {
        BankTransaction t = new BankTransaction();
        t.setClientCode(params.get("ClientCode"));
        t.setMerchantCode(params.get("MerchantCode"));
        t.setTxnCurrency(params.get("TxnCurrency"));
        t.setTxnAmount(params.get("TxnAmount"));
        t.setTxnScAmount(params.getOrDefault("TxnScAmount", "0"));
        t.setMerchantRefNo(params.get("MerchantRefNo"));
        t.setSuccessStaticFlag(params.getOrDefault("SuccessStaticFlag", "N"));
        t.setFailureStaticFlag(params.getOrDefault("FailureStaticFlag", "N"));
        t.setTransactionDate(params.get("Date"));
        // Ref1 carries the merchant's orderId (Ch.2 Note 2: pass-through UDF, echoed untouched)
        t.setRef1(params.getOrDefault("Ref1", ""));
        t.setDynamicUrl(params.get("DynamicUrl"));
        t.setChecksum(params.get("CheckSum"));
        t.setTransactionId("TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        t.setStatus(TransactionStatus.INITIATED); // accepted, waiting for a scenario to be chosen
        t.setBankRefNo("0");
        t.setMessage("");
        t.setCallbackCount(0);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        BankTransaction saved = repository.save(t);
        System.out.println("[BANK] SAVED bank_transaction merchantRefNo=" + saved.getMerchantRefNo()
                + " transactionId=" + saved.getTransactionId() + " status=" + saved.getStatus());
        return saved;
    }

    public BankTransaction find(String merchantRefNo) {
        return repository.findByMerchantRefNo(merchantRefNo).orElse(null);
    }

    public BankTransaction processScenario(String merchantRefNo, SimulationScenario scenario) {
        BankTransaction t = repository.findByMerchantRefNo(merchantRefNo)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        t.setScenario(scenario);

        switch (scenario) {
            case SUCCESS, DUPLICATE -> {
                t.setStatus(TransactionStatus.SUCCESS);
                t.setBankRefNo(generateBankRefNo());
                t.setMessage("");
            }
            case DELAY -> {
                t.setStatus(TransactionStatus.PENDING);
                t.setBankRefNo("0");
                t.setMessage("Bank response delayed - transaction pending confirmation");
            }
            case DROP -> {
                t.setStatus(TransactionStatus.PENDING);
                t.setBankRefNo("0");
                t.setMessage("Callback not delivered - transaction pending, awaiting manual verification");
            }
            case TAMPER_AMOUNT -> {
                t.setStatus(TransactionStatus.SUCCESS);
                t.setBankRefNo(generateBankRefNo());
                t.setMessage("");
                t.setTamperAmountInResponse(true);
            }
            case TAMPER_CHECKSUM -> {
                t.setStatus(TransactionStatus.SUCCESS);
                t.setBankRefNo(generateBankRefNo());
                t.setMessage("");
                t.setTamperChecksumInResponse(true);
            }
            case FAILURE_INSUFFICIENT_FUNDS -> fail(t, "Insufficient Funds");
            case FAILURE_AUTH -> fail(t, "Customer authentication failed");
            case FAILURE_ACCOUNT_BLOCKED -> fail(t, "Account is blocked");
            case FAILURE_INVALID_ACCOUNT -> fail(t, "Invalid or non-eligible account");
            case FAILURE_TXN_LIMIT_EXCEEDED -> fail(t, "Daily transaction limit exceeded");
            case FAILURE_BANK_ERROR -> fail(t, "Bank internal error, please try again");
            case FAILURE_BANK_MAINTENANCE -> fail(t, "NetBanking temporarily unavailable due to maintenance");
            case PENDING -> {
                t.setStatus(TransactionStatus.PENDING);
                t.setBankRefNo("0");
                t.setMessage("Transaction pending confirmation");
            }
            case SESSION_TIMEOUT -> {
                t.setStatus(TransactionStatus.SESSION_EXPIRED);
                t.setBankRefNo("0");
                t.setMessage("Session Expired. Please login again to complete the transaction.");
            }
        }
        t.setUpdatedAt(LocalDateTime.now());
        BankTransaction saved = repository.save(t);
        System.out.println("[BANK] UPDATED bank_transaction merchantRefNo=" + saved.getMerchantRefNo()
                + " -> status=" + saved.getStatus() + " bankRefNo=" + saved.getBankRefNo()
                + " message=\"" + saved.getMessage() + "\"");
        return saved;
    }

    private void fail(BankTransaction t, String reason) {
        t.setStatus(TransactionStatus.FAILURE);
        t.setBankRefNo("0");
        t.setMessage(reason);
    }

    private String generateBankRefNo() {
        return "HDFC" + System.currentTimeMillis();
    }

    // ---------------------------------------------------------------------
    // Ch.8: Verify Transaction (S2S)
    // ---------------------------------------------------------------------

    /**
     * Ch.8 "Verify Transaction". Two distinct failure shapes, per Ch.8-D:
     *   - blank flgSuccess -> no transaction record at the bank's end at all
     *   - "F"               -> a transaction WAS found, but the identifying values
     *                          (ClientCode / Date / TxnAmount) the caller sent don't
     *                          match what's actually on file
     * Also where TAMPER_AMOUNT / TAMPER_CHECKSUM actually corrupt the outgoing response.
     */
    public VerificationResponse verify(String clientCode, String merchantCode, String merchantRefNo,
                                        String date, String txnAmount, String transactionId) {

        BankTransaction t = find(merchantRefNo);

        VerificationResponse response = new VerificationResponse();
        response.setMerchantCode(merchantCode);
        response.setClientCode(clientCode);
        response.setMerchantRefNo(merchantRefNo);
        response.setDate(date);
        response.setTxnAmount(txnAmount);
        response.setTransactionId(transactionId);
        response.setRef1("");

        if (t == null) {
            response.setFlgSuccess("");
            response.setMessage("Transaction not found at bank end");
            response.setBankRefNo(null);
            response.setCheckSum(null);
            return response;
        }

        response.setTxnCurrency(t.getTxnCurrency());
        response.setTxnScAmount(t.getTxnScAmount());
        response.setSuccessStaticFlag(t.getSuccessStaticFlag());
        response.setFailureStaticFlag(t.getFailureStaticFlag());

        boolean identityMatches = clientCode.equals(t.getClientCode()) && date.equals(t.getTransactionDate());
        boolean amountMatches = txnAmount.equals(t.getTxnAmount());

        if (!identityMatches || !amountMatches) {
            response.setTxnAmount(t.getTxnAmount());
            response.setFlgSuccess("F");
            response.setBankRefNo("0");
            response.setMessage(!amountMatches
                    ? "Verification amount does not match the original transaction"
                    : "Verification parameters do not match the original transaction");
            response.setCheckSum(ChecksumUtil.computeReturnChecksum(
                    t.getClientCode(), t.getMerchantCode(), t.getTxnCurrency(), t.getTxnAmount(), t.getTxnScAmount(),
                    t.getMerchantRefNo(), t.getSuccessStaticFlag(), t.getFailureStaticFlag(), t.getTransactionDate(),
                    "0", response.getMessage(), EpiConstants.CHECKSUM_KEY));
            return response;
        }

        String flg = switch (t.getStatus()) {
            case SUCCESS -> "S";
            case FAILURE, SESSION_EXPIRED -> "F";
            case PENDING -> "P";
            case INITIATED -> "";
        };

        // What actually goes out on the wire - starts as the truth, then TAMPER_* scenarios corrupt it
        String outgoingAmount = t.getTxnAmount();
        String outgoingBankRefNo = t.getBankRefNo();
        String outgoingMessage = t.getMessage();

        if (t.isTamperAmountInResponse()) {
            // Deliberately report an amount different from what was actually processed -
            // this is what tests the Gateway's amount cross-check.
            outgoingAmount = new BigDecimal(t.getTxnAmount()).add(new BigDecimal("500.00")).toPlainString();
        }

        String checksum = ChecksumUtil.computeReturnChecksum(
                t.getClientCode(), t.getMerchantCode(), t.getTxnCurrency(), outgoingAmount, t.getTxnScAmount(),
                t.getMerchantRefNo(), t.getSuccessStaticFlag(), t.getFailureStaticFlag(), t.getTransactionDate(),
                outgoingBankRefNo, outgoingMessage, EpiConstants.CHECKSUM_KEY);

        if (t.isTamperChecksumInResponse()) {
            // Deliberately corrupt an otherwise-correctly-computed checksum - this is
            // what tests the Gateway's checksum validation layer.
            checksum = "0";
        }

        response.setTxnAmount(outgoingAmount);
        response.setFlgSuccess(flg);
        response.setBankRefNo(outgoingBankRefNo);
        response.setMessage(outgoingMessage);
        response.setCheckSum(checksum);
        return response;
    }
}
