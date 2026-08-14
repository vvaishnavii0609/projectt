package com.demo.hdfcnetbankingsimulator.gateway.dto;


public class PaymentInitiationResponse {

    private String transactionId;
    private String orderId;
    private String amount;
    private String currency;
    private String status;
    private String redirectUrl;

    public PaymentInitiationResponse() {}

    public PaymentInitiationResponse(String transactionId, String orderId, String amount,
                                      String currency, String status, String redirectUrl) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.redirectUrl = redirectUrl;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
}
