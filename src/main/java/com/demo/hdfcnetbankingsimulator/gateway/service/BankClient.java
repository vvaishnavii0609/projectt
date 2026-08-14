package com.demo.hdfcnetbankingsimulator.gateway.service;

import com.demo.hdfcnetbankingsimulator.common.EpiConstants;
import com.demo.hdfcnetbankingsimulator.gateway.dto.VerificationResponse;
import com.demo.hdfcnetbankingsimulator.gateway.model.PaymentTransaction;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The Gateway's connection to the bank's /netbanking/verify endpoint.
 * Even though it's the same app/port, this is a REAL HTTP call (not a direct method
 * call into BankService) - that's deliberate: it keeps the "server to server" shape
 * of the real integration honest, so the checksum/params genuinely travel over HTTP
 * exactly like they would if the bank were a separate system.
 */
@Service
public class BankClient {

    private static final String BANK_BASE_URL = "http://localhost:9292";

    private final RestTemplate restTemplate;

    public BankClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public VerificationResponse verify(PaymentTransaction t) {
        String url = UriComponentsBuilder.fromUriString(BANK_BASE_URL + "/netbanking/verify")
                .queryParam("MerchantCode", EpiConstants.MERCHANT_CODE)
                .queryParam("ClientCode", t.getClientCode())
                .queryParam("MerchantRefNo", t.getMerchantRefNo())
                .queryParam("Date", t.getDate())
                .queryParam("TxnAmount", t.getAmount())
                .queryParam("TransactionId", "XTXTV01")
                .queryParam("FlgVerify", "Y")
                .build()
                .toUriString();

        System.out.println("[GATEWAY -> BANK, S2S Verify] " + url);
        VerificationResponse response = restTemplate.getForObject(url, VerificationResponse.class);
        System.out.println("[BANK -> GATEWAY, S2S Verify response] " + (response == null ? "null"
                : "flgSuccess=" + response.getFlgSuccess() + " bankRefNo=" + response.getBankRefNo()));
        return response;
    }
}
