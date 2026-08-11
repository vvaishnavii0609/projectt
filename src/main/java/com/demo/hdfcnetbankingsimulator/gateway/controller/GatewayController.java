package com.demo.hdfcnetbankingsimulator.gateway.controller;

import com.demo.hdfcnetbankingsimulator.gateway.dto.CheckoutPaymentRequest;
import com.demo.hdfcnetbankingsimulator.gateway.dto.PaymentInitiationResponse;
import com.demo.hdfcnetbankingsimulator.gateway.service.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/gateway")
@CrossOrigin
public class GatewayController {

    @Autowired
    private GatewayService gatewayService;

    @PostMapping("/payment")
    public ResponseEntity<?> initiatePayment(@RequestBody CheckoutPaymentRequest request) {
        try {
            PaymentInitiationResponse response = gatewayService.initiatePayment(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
