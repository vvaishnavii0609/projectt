package com.demo.hdfcnetbankingsimulator.bank.dto;

/** Body sent to the Gateway's S2S callback endpoint - must match gateway/dto/CallbackPayload's shape. */
public class CallbackPayload {

    private String clientCode;
    private String merchantCode;
    private String merchantRefNo;
    private String txnCurrency;
    private String txnAmount;
    private String txnScAmount;
    private String successStaticFlag;
    private String failureStaticFlag;
    private String date;
    private String status;
    private String bankRefNo;
    private String message;
    private String checkSum;

    public CallbackPayload() {}

    public String getClientCode() { return clientCode; }
    public void setClientCode(String v) { clientCode = v; }
    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String v) { merchantCode = v; }
    public String getMerchantRefNo() { return merchantRefNo; }
    public void setMerchantRefNo(String v) { merchantRefNo = v; }
    public String getTxnCurrency() { return txnCurrency; }
    public void setTxnCurrency(String v) { txnCurrency = v; }
    public String getTxnAmount() { return txnAmount; }
    public void setTxnAmount(String v) { txnAmount = v; }
    public String getTxnScAmount() { return txnScAmount; }
    public void setTxnScAmount(String v) { txnScAmount = v; }
    public String getSuccessStaticFlag() { return successStaticFlag; }
    public void setSuccessStaticFlag(String v) { successStaticFlag = v; }
    public String getFailureStaticFlag() { return failureStaticFlag; }
    public void setFailureStaticFlag(String v) { failureStaticFlag = v; }
    public String getDate() { return date; }
    public void setDate(String v) { date = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public String getBankRefNo() { return bankRefNo; }
    public void setBankRefNo(String v) { bankRefNo = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { message = v; }
    public String getCheckSum() { return checkSum; }
    public void setCheckSum(String v) { checkSum = v; }

    @Override
    public String toString() {
        return "CallbackPayload{"
                + "ClientCode=" + clientCode
                + ", MerchantCode=" + merchantCode
                + ", MerchantRefNo=" + merchantRefNo
                + ", TxnCurrency=" + txnCurrency
                + ", TxnAmount=" + txnAmount
                + ", TxnScAmount=" + txnScAmount
                + ", SuccessStaticFlag=" + successStaticFlag
                + ", FailureStaticFlag=" + failureStaticFlag
                + ", Date=" + date
                + ", Status=" + status
                + ", BankRefNo=" + bankRefNo
                + ", Message=" + message
                + ", CheckSum=" + checkSum
                + "}";
    }
}
