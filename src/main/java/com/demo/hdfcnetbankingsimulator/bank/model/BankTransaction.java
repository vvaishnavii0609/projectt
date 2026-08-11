package com.demo.hdfcnetbankingsimulator.bank.model;

import com.demo.hdfcnetbankingsimulator.bank.enums.SimulationScenario;
import com.demo.hdfcnetbankingsimulator.bank.enums.TransactionStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // HDFC parameters (Ch.2 table)
    private String clientCode;
    private String merchantCode;
    private String txnCurrency;
    private String txnAmount;
    private String txnScAmount;
    private String merchantRefNo;
    private String successStaticFlag;
    private String failureStaticFlag;
    private String transactionDate;

    // Ref1 carries the merchant's orderId (pass-through UDF, Ch.2 Note 2) - this is
    // what lets the bank redirect the browser straight back to the right merchant
    // page without the merchant ever needing to know MerchantRefNo.
    private String ref1;
    private String ref2;
    private String ref3;
    private String ref4;
    private String ref5;
    private String ref6;
    private String ref7;
    private String ref8;
    private String ref9;
    private String ref10;
    private String ref11;

    private String date1;
    private String date2;
    private String displayDetails;
    private String details1;
    private String details2;
    private String details3;

    private String checksum;
    private String dynamicUrl;

    // outcome, set once a scenario is processed (Ch.2-D of the doc)
    private String bankRefNo = "0";
    private String message = "";

    // TAMPER_AMOUNT / TAMPER_CHECKSUM scenarios: the bank's own record stays correct,
    // but the OUTGOING callback/verify response gets deliberately corrupted - simulating
    // a response tampered in transit, not a bank that got its own bookkeeping wrong.
    private boolean tamperAmountInResponse = false;
    private boolean tamperChecksumInResponse = false;

    private String transactionId;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private SimulationScenario scenario;

    private int callbackCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BankTransaction() {
    }

    public Long getId() { return id; }

    public String getClientCode() { return clientCode; }
    public void setClientCode(String clientCode) { this.clientCode = clientCode; }
    public String getMerchantCode() { return merchantCode; }
    public void setMerchantCode(String merchantCode) { this.merchantCode = merchantCode; }
    public String getTxnCurrency() { return txnCurrency; }
    public void setTxnCurrency(String txnCurrency) { this.txnCurrency = txnCurrency; }
    public String getTxnAmount() { return txnAmount; }
    public void setTxnAmount(String txnAmount) { this.txnAmount = txnAmount; }
    public String getTxnScAmount() { return txnScAmount; }
    public void setTxnScAmount(String txnScAmount) { this.txnScAmount = txnScAmount; }
    public String getMerchantRefNo() { return merchantRefNo; }
    public void setMerchantRefNo(String merchantRefNo) { this.merchantRefNo = merchantRefNo; }
    public String getSuccessStaticFlag() { return successStaticFlag; }
    public void setSuccessStaticFlag(String successStaticFlag) { this.successStaticFlag = successStaticFlag; }
    public String getFailureStaticFlag() { return failureStaticFlag; }
    public void setFailureStaticFlag(String failureStaticFlag) { this.failureStaticFlag = failureStaticFlag; }
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public String getRef1() { return ref1; }
    public void setRef1(String ref1) { this.ref1 = ref1; }
    public String getRef2() { return ref2; }
    public void setRef2(String ref2) { this.ref2 = ref2; }
    public String getRef3() { return ref3; }
    public void setRef3(String ref3) { this.ref3 = ref3; }
    public String getRef4() { return ref4; }
    public void setRef4(String ref4) { this.ref4 = ref4; }
    public String getRef5() { return ref5; }
    public void setRef5(String ref5) { this.ref5 = ref5; }
    public String getRef6() { return ref6; }
    public void setRef6(String ref6) { this.ref6 = ref6; }
    public String getRef7() { return ref7; }
    public void setRef7(String ref7) { this.ref7 = ref7; }
    public String getRef8() { return ref8; }
    public void setRef8(String ref8) { this.ref8 = ref8; }
    public String getRef9() { return ref9; }
    public void setRef9(String ref9) { this.ref9 = ref9; }
    public String getRef10() { return ref10; }
    public void setRef10(String ref10) { this.ref10 = ref10; }
    public String getRef11() { return ref11; }
    public void setRef11(String ref11) { this.ref11 = ref11; }

    public String getDate1() { return date1; }
    public void setDate1(String date1) { this.date1 = date1; }
    public String getDate2() { return date2; }
    public void setDate2(String date2) { this.date2 = date2; }
    public String getDisplayDetails() { return displayDetails; }
    public void setDisplayDetails(String displayDetails) { this.displayDetails = displayDetails; }
    public String getDetails1() { return details1; }
    public void setDetails1(String details1) { this.details1 = details1; }
    public String getDetails2() { return details2; }
    public void setDetails2(String details2) { this.details2 = details2; }
    public String getDetails3() { return details3; }
    public void setDetails3(String details3) { this.details3 = details3; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getDynamicUrl() { return dynamicUrl; }
    public void setDynamicUrl(String dynamicUrl) { this.dynamicUrl = dynamicUrl; }

    public String getBankRefNo() { return bankRefNo; }
    public void setBankRefNo(String bankRefNo) { this.bankRefNo = bankRefNo; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isTamperAmountInResponse() { return tamperAmountInResponse; }
    public void setTamperAmountInResponse(boolean v) { this.tamperAmountInResponse = v; }
    public boolean isTamperChecksumInResponse() { return tamperChecksumInResponse; }
    public void setTamperChecksumInResponse(boolean v) { this.tamperChecksumInResponse = v; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public SimulationScenario getScenario() { return scenario; }
    public void setScenario(SimulationScenario scenario) { this.scenario = scenario; }

    public int getCallbackCount() { return callbackCount; }
    public void setCallbackCount(int callbackCount) { this.callbackCount = callbackCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
