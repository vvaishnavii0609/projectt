package com.demo.hdfcnetbankingsimulator.bank.enums;


public enum SimulationScenario {

    SUCCESS,
    PENDING,

    FAILURE_INSUFFICIENT_FUNDS,
    FAILURE_AUTH,                 // wrong credentials / customer authentication failed
    FAILURE_ACCOUNT_BLOCKED,
    FAILURE_INVALID_ACCOUNT,
    FAILURE_TXN_LIMIT_EXCEEDED,

    FAILURE_BANK_ERROR,           // generic internal error
    FAILURE_BANK_MAINTENANCE,

    SESSION_TIMEOUT,              // customer never confirms - nothing processed, nothing sent

    DELAY,                        // succeeds, callback fires ~6s late
    DROP,                         // processed as SUCCESS internally, but NO callback ever sent
    DUPLICATE,                    // succeeds, callback fires twice

    // --- data-integrity attack simulation: tests PG's response validation layers ---
    TAMPER_AMOUNT,                // succeeds, but callback/verify report a DIFFERENT amount
    TAMPER_CHECKSUM               // succeeds, but callback/verify carry a corrupted checksum
}
