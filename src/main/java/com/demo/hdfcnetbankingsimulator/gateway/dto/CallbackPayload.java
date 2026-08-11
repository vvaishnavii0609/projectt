package com.demo.hdfcnetbankingsimulator.gateway.dto;

/** Body of the backend (server-to-server) callback the bank fires at us. Must match
 *  bank/dto/CallbackPayload's shape exactly (Jackson matches by JSON property name).
 *  This is a HINT, not the source of truth - handleBankCallback() immediately
 *  re-confirms via the S2S Verify endpoint before trusting any of this. */
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
    public void setStatus(String status) { this.status = status; }
    public String getBankRefNo() { return bankRefNo; }
    public void setBankRefNo(String bankRefNo) { this.bankRefNo = bankRefNo; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCheckSum() { return checkSum; }
    public void setCheckSum(String checkSum) { this.checkSum = checkSum; }

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
