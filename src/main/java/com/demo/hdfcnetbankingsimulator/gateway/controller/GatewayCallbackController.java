package com.demo.hdfcnetbankingsimulator.gateway.controller;

import com.demo.hdfcnetbankingsimulator.gateway.dto.CallbackPayload;
import com.demo.hdfcnetbankingsimulator.gateway.service.GatewayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** "Bank Backend -> Gateway Backend" callback receiver. */
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
        System.out.println("[GATEWAY <- callback] FULL PAYLOAD RECEIVED: " + payload);
        System.out.println("[GATEWAY <- callback] merchantRefNo=" + payload.getMerchantRefNo()
                + " claimedStatus=" + payload.getStatus() + "  (NOT trusted yet - verifying independently)");
        String result = gatewayService.handleBankCallback(payload);
        System.out.println("[GATEWAY <- callback] result: " + result);
        return ResponseEntity.ok(result);
    }
}
