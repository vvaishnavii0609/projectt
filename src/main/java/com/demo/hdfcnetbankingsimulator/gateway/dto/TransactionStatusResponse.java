package com.demo.hdfcnetbankingsimulator.gateway.dto;


public class TransactionStatusResponse {

    private String orderId;
    private String transactionId;
    private String status;
    private String bankRefNo;
    private String message;
    private String amount;
    private String currency;
    private String receipt;

    public TransactionStatusResponse() {}

    public TransactionStatusResponse(String orderId, String transactionId, String status, String bankRefNo,
                                      String message, String amount, String currency, String receipt) {
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.status = status;
        this.bankRefNo = bankRefNo;
        this.message = message;
        this.amount = amount;
        this.currency = currency;
        this.receipt = receipt;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBankRefNo() { return bankRefNo; }
    public void setBankRefNo(String bankRefNo) { this.bankRefNo = bankRefNo; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getReceipt() { return receipt; }
    public void setReceipt(String receipt) { this.receipt = receipt; }
}
