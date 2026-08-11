package com.demo.hdfcnetbankingsimulator.gateway.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;

    private String orderId;

    private String clientCode;

    private String merchantCode;

    private String merchantRefNo;

    // the exact Date string sent to the bank - needed again later, unchanged, for the Verify lookup
    private String date;

    private String amount;

    private String currency;

    // needed to independently recompute the return checksum (Ch.14) - always constant in this
    // demo ("0.00" / "N" / "N") but stored rather than assumed, since real integrations vary them
    private String txnScAmount;
    private String successStaticFlag;
    private String failureStaticFlag;

    private String paymentMethod;

    private String bankId;

    private String status;

    private String bankRefNo;

    private String message;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    public PaymentTransaction() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }


    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }


    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }


    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }


    public String getMerchantRefNo() {
        return merchantRefNo;
    }

    public void setMerchantRefNo(String merchantRefNo) {
        this.merchantRefNo = merchantRefNo;
    }


    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }


    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }


    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }


    public String getTxnScAmount() {
        return txnScAmount;
    }

    public void setTxnScAmount(String txnScAmount) {
        this.txnScAmount = txnScAmount;
    }


    public String getSuccessStaticFlag() {
        return successStaticFlag;
    }

    public void setSuccessStaticFlag(String successStaticFlag) {
        this.successStaticFlag = successStaticFlag;
    }


    public String getFailureStaticFlag() {
        return failureStaticFlag;
    }

    public void setFailureStaticFlag(String failureStaticFlag) {
        this.failureStaticFlag = failureStaticFlag;
    }


    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }


    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getBankRefNo() {
        return bankRefNo;
    }

    public void setBankRefNo(String bankRefNo) {
        this.bankRefNo = bankRefNo;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
