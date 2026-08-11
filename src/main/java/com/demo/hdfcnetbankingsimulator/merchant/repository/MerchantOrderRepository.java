package com.demo.hdfcnetbankingsimulator.merchant.repository;

import com.demo.hdfcnetbankingsimulator.merchant.model.MerchantOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantOrderRepository extends JpaRepository<MerchantOrder,Long> {

    Optional<MerchantOrder> findByOrderId(String orderId);
}
