package com.demo.hdfcnetbankingsimulator.gateway.dto;

/**
 * Ch.8 "Verify Transaction" response, as seen from the Gateway side.
 * Fields mirror what the real HDFC doc's Ch.8-C says the verify response echoes back:
 * MerchantCode, Date, MerchantRefNo, ClientCode, TxnAmount, TransactionId, Ref1,
 * flgVerify, BankRefNo, flgSuccess, Message.
 */
public class VerificationResponse {

    // echoed identity/lookup fields - the Gateway cross-checks these against what it sent
    private String merchantCode;
    private String clientCode;
    private String merchantRefNo;
    private String date;
    private String txnAmount;
    private String txnCurrency;
    private String txnScAmount;
    private String successStaticFlag;
    private String failureStaticFlag;
    private String transactionId; // echoes "XTXTV01", confirms this was handled as a verify call
    private String ref1;

    // the actual outcome
    private String flgSuccess;  // "S" success / "F" failure or mismatch / blank = not found
    private String bankRefNo;
    private String message;
    private String checkSum;

    public VerificationResponse() {}

    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String v) { merchantCode = v; }
    public String getClientCode() { return clientCode; }
    public void setClientCode(String v) { clientCode = v; }
    public String getMerchantRefNo() { return merchantRefNo; }
    public void setMerchantRefNo(String merchantRefNo) { this.merchantRefNo = merchantRefNo; }
    public String getDate() { return date; }
    public void setDate(String v) { date = v; }
    public String getTxnAmount() { return txnAmount; }
    public void setTxnAmount(String v) { txnAmount = v; }
    public String getTxnCurrency() { return txnCurrency; }
    public void setTxnCurrency(String v) { txnCurrency = v; }
    public String getTxnScAmount() { return txnScAmount; }
    public void setTxnScAmount(String v) { txnScAmount = v; }
    public String getSuccessStaticFlag() { return successStaticFlag; }
    public void setSuccessStaticFlag(String v) { successStaticFlag = v; }
    public String getFailureStaticFlag() { return failureStaticFlag; }
    public void setFailureStaticFlag(String v) { failureStaticFlag = v; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String v) { transactionId = v; }
    public String getRef1() { return ref1; }
    public void setRef1(String v) { ref1 = v; }
    public String getFlgSuccess() { return flgSuccess; }
    public void setFlgSuccess(String flgSuccess) { this.flgSuccess = flgSuccess; }
    public String getBankRefNo() { return bankRefNo; }
    public void setBankRefNo(String bankRefNo) { this.bankRefNo = bankRefNo; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCheckSum() { return checkSum; }
    public void setCheckSum(String checkSum) { this.checkSum = checkSum; }
}
