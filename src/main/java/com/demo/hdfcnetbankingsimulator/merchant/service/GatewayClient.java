package com.demo.hdfcnetbankingsimulator.merchant.service;

import com.demo.hdfcnetbankingsimulator.gateway.dto.TransactionStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * The Merchant's ONLY connection to the payment world. Per the whiteboard: "merchant
 * relies on PG, never on the bank" - this class is what makes that literally true in
 * code. MerchantController never talks to BankClient or anything in the bank package;
 * it only ever asks the Gateway.
 */
@Service
public class GatewayClient {

    private final RestTemplate restTemplate;
    private final String gatewayBaseUrl;

    public GatewayClient(RestTemplate restTemplate,
                          @Value("${epi.gateway-base-url:http://localhost:9292}") String gatewayBaseUrl) {
        this.restTemplate = restTemplate;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    /** "Merchant hits get-transaction on PG" - the diagram's final step. */
    public TransactionStatusResponse getTransaction(String orderId) {
        return restTemplate.getForObject(gatewayBaseUrl + "/gateway/status/" + orderId, TransactionStatusResponse.class);
    }
}
