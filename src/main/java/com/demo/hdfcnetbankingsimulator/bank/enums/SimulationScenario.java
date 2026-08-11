package com.demo.hdfcnetbankingsimulator.bank.enums;

/**
 * Every outcome the control panel can force. Grouped by category on purpose - when
 * you explain this to your sir, "we categorized failures by where they occur" reads
 * a lot better than a flat list of 15 names.
 */
public enum SimulationScenario {

    // --- normal outcomes ---
    SUCCESS,
    PENDING,

    // --- business failures: the payment itself is legitimately rejected ---
    FAILURE_INSUFFICIENT_FUNDS,
    FAILURE_AUTH,                 // wrong credentials / customer authentication failed
    FAILURE_ACCOUNT_BLOCKED,
    FAILURE_INVALID_ACCOUNT,
    FAILURE_TXN_LIMIT_EXCEEDED,

    // --- technical/bank-side failures: nothing wrong with the payment itself ---
    FAILURE_BANK_ERROR,           // generic internal error
    FAILURE_BANK_MAINTENANCE,

    // --- session ---
    SESSION_TIMEOUT,              // customer never confirms - nothing processed, nothing sent

    // --- communication fault injection: tests whether PG survives an unreliable bank ---
    DELAY,                        // succeeds, callback fires ~6s late
    DROP,                         // processed as SUCCESS internally, but NO callback ever sent
    DUPLICATE,                    // succeeds, callback fires twice

    // --- data-integrity attack simulation: tests PG's response validation layers ---
    TAMPER_AMOUNT,                // succeeds, but callback/verify report a DIFFERENT amount
    TAMPER_CHECKSUM               // succeeds, but callback/verify carry a corrupted checksum
}
