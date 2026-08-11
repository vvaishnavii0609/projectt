package com.demo.hdfcnetbankingsimulator.merchant.controller;

import com.demo.hdfcnetbankingsimulator.gateway.dto.TransactionStatusResponse;
import com.demo.hdfcnetbankingsimulator.merchant.dto.MerchantOrderResponse;
import com.demo.hdfcnetbankingsimulator.merchant.dto.MerchantTransactionStatusResponse;
import com.demo.hdfcnetbankingsimulator.merchant.model.MerchantOrder;
import com.demo.hdfcnetbankingsimulator.merchant.repository.MerchantOrderRepository;
import com.demo.hdfcnetbankingsimulator.merchant.service.GatewayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/merchant")
@CrossOrigin
public class MerchantController {

    @Autowired
    private MerchantOrderRepository orderRepository;

    @Autowired
    private GatewayClient gatewayClient;

    @GetMapping("/order")
    public MerchantOrderResponse getOrder() {

        String orderId = "ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String productName = "Smart Digital Watch";
        Double amount = 1.79;
        String currency = "INR";

        MerchantOrder order = new MerchantOrder(orderId, productName, amount, currency, "CREATED", LocalDateTime.now());
        orderRepository.save(order);
        System.out.println("[MERCHANT] SAVED merchant_order orderId=" + orderId);

        return new MerchantOrderResponse(productName, amount, currency, orderId);
    }

    /**
     * "Merchant hits get-transaction on PG" - the merchant's backend is what talks to
     * PG server-to-server; the browser only ever talks to Merchant, never PG or Bank.
     */
    @GetMapping("/transaction/{orderId}")
    public MerchantTransactionStatusResponse getTransactionStatus(@PathVariable("orderId") String orderId) {
        System.out.println("[MERCHANT -> PG] GET /gateway/status/" + orderId);
        TransactionStatusResponse t = gatewayClient.getTransaction(orderId);
        if (t == null) {
            System.out.println("[MERCHANT] could not reach PG for orderId=" + orderId);
            return new MerchantTransactionStatusResponse(orderId, null, "UNKNOWN", null,
                    "Could not reach the payment gateway", null, null, null);
        }
        System.out.println("[MERCHANT <- PG] status=" + t.getStatus() + " for orderId=" + orderId);
        return new MerchantTransactionStatusResponse(
                t.getOrderId(), t.getTransactionId(), t.getStatus(), t.getBankRefNo(),
                t.getMessage(), t.getAmount(), t.getCurrency(), t.getReceipt());
    }
}
