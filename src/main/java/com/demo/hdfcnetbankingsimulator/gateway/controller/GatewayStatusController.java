package com.demo.hdfcnetbankingsimulator.gateway.controller;

import com.demo.hdfcnetbankingsimulator.gateway.dto.TransactionStatusResponse;
import com.demo.hdfcnetbankingsimulator.gateway.model.PaymentTransaction;
import com.demo.hdfcnetbankingsimulator.gateway.service.GatewayService;
import org.springframework.web.bind.annotation.*;

/** Polled (indirectly, via Merchant's own /merchant/transaction endpoint) after every
 *  checkout. Triggers an on-demand Verify if nothing conclusive has arrived yet. */
@RestController
@RequestMapping("/gateway")
@CrossOrigin
public class GatewayStatusController {

    private final GatewayService gatewayService;

    public GatewayStatusController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @GetMapping("/status/{orderId}")
    public TransactionStatusResponse getStatus(@PathVariable("orderId") String orderId) {
        PaymentTransaction t = gatewayService.getStatus(orderId);
        String receipt = "SUCCESS".equals(t.getStatus()) ? "REC-" + t.getOrderId() : null;
        return new TransactionStatusResponse(
                t.getOrderId(), t.getTransactionId(), t.getStatus(), t.getBankRefNo(),
                t.getMessage(), t.getAmount(), t.getCurrency(), receipt);
    }
}
