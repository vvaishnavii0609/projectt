package com.demo.hdfcnetbankingsimulator.bank.enums;

public enum TransactionStatus {

    INITIATED,        // accepted by the bank, waiting for a scenario to be chosen
    SUCCESS,
    FAILURE,
    PENDING,
    SESSION_EXPIRED
}
