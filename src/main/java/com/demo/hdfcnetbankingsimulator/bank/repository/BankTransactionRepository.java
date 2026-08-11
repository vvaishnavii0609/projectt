package com.demo.hdfcnetbankingsimulator.bank.repository;

import com.demo.hdfcnetbankingsimulator.bank.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByTransactionId(String transactionId);

    Optional<BankTransaction> findByMerchantRefNo(String merchantRefNo);
}
