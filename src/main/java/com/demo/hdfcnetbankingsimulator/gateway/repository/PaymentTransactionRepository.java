package com.demo.hdfcnetbankingsimulator.gateway.repository;

import com.demo.hdfcnetbankingsimulator.gateway.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByOrderId(String orderId);

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    Optional<PaymentTransaction> findByMerchantRefNo(String merchantRefNo);
}