package com.demo.hdfcnetbankingsimulator.merchant.dto;

public class MerchantOrderResponse {
    private String productName;
    private Double amount;
    private String currency;
    private String orderId;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public MerchantOrderResponse(String productName, Double amount, String currency, String orderId) {
        this.productName = productName;
        this.amount = amount;
        this.currency = currency;
        this.orderId = orderId;
    }

    public MerchantOrderResponse() {
    }
}
