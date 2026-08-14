package com.demo.hdfcnetbankingsimulator.gateway.controller;

import com.demo.hdfcnetbankingsimulator.gateway.dto.CallbackPayload;
import com.demo.hdfcnetbankingsimulator.gateway.service.GatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gateway")
@CrossOrigin
public class GatewayCallbackController {

    private final GatewayService gatewayService;

    public GatewayCallbackController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestBody CallbackPayload payload) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("       PAYMENT GATEWAY <- BANK CALLBACK RECEIVED");
        System.out.println("============================================================");
        System.out.println("[GATEWAY] Bank callback received successfully.");
        System.out.println("[GATEWAY] Merchant Ref No : " + payload.getMerchantRefNo());
        System.out.println("[GATEWAY] Claimed Status  : " + payload.getStatus());
        System.out.println("[GATEWAY] Bank Ref No     : " + payload.getBankRefNo());
        System.out.println("[GATEWAY] Amount          : " + payload.getTxnAmount());
        System.out.println("[GATEWAY] Message         : " + payload.getMessage());
        System.out.println("------------------------------------------------------------");

        String result = gatewayService.handleBankCallback(payload);
        System.out.println("[GATEWAY <- callback] result: " + result);
        return ResponseEntity.ok(result);
    }
}
