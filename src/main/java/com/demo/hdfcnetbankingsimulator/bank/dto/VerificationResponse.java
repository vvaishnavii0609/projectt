package com.demo.hdfcnetbankingsimulator.bank.dto;

/** Ch.8 "Verify Transaction" response - must match gateway/dto/VerificationResponse's shape exactly. */
public class VerificationResponse {

    private String merchantCode;
    private String clientCode;
    private String merchantRefNo;
    private String date;
    private String txnAmount;
    private String txnCurrency;
    private String txnScAmount;
    private String successStaticFlag;
    private String failureStaticFlag;
    private String transactionId;
    private String ref1;

    private String flgSuccess;  // "S" / "F" / blank (not found)
    private String bankRefNo;
    private String message;
    private String checkSum;

    public VerificationResponse() {}

    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String v) { merchantCode = v; }
    public String getClientCode() { return clientCode; }
    public void setClientCode(String v) { clientCode = v; }
    public String getMerchantRefNo() { return merchantRefNo; }
    public void setMerchantRefNo(String v) { merchantRefNo = v; }
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
    public void setFlgSuccess(String v) { flgSuccess = v; }
    public String getBankRefNo() { return bankRefNo; }
    public void setBankRefNo(String v) { bankRefNo = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { message = v; }
    public String getCheckSum() { return checkSum; }
    public void setCheckSum(String v) { checkSum = v; }
}
