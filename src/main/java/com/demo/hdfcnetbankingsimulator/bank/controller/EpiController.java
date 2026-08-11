package com.demo.hdfcnetbankingsimulator.bank.controller;

import com.demo.hdfcnetbankingsimulator.bank.model.BankTransaction;
import com.demo.hdfcnetbankingsimulator.bank.service.BankService;
import com.demo.hdfcnetbankingsimulator.common.EpiConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * The real EPI entry point (Ch.2). Pure logic - no HTML here. On success, redirects
 * the browser to the static control-panel page (static/bank-scenario.html), passing
 * just enough via query params for that page to display itself and know what to submit.
 */
@RestController
public class EpiController {

    private final BankService bankService;

    public EpiController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/netbanking/merchant")
    public ResponseEntity<String> initiateGet(@RequestParam Map<String, String> params) {
        return handle(params);
    }

    @PostMapping("/netbanking/merchant")
    public ResponseEntity<String> initiatePost(@RequestParam Map<String, String> params) {
        return handle(params);
    }

    private ResponseEntity<String> handle(Map<String, String> params) {
        System.out.println("[BANK] received EPI request: MerchantRefNo=" + params.get("MerchantRefNo")
                + " TxnAmount=" + params.get("TxnAmount"));

        String rejection = bankService.validate(params);
        if (rejection != null) {
            System.out.println("[BANK] REJECTED: " + rejection);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(rejection);
        }

        BankTransaction t = bankService.accept(params);
        System.out.println("[BANK] accepted, showing control panel for MerchantRefNo=" + t.getMerchantRefNo());

        String redirectUrl = UriComponentsBuilder.fromUriString(EpiConstants.BANK_BASE_URL + "/bank-scenario.html")
                .queryParam("merchantRefNo", t.getMerchantRefNo())
                .queryParam("merchantCode", t.getMerchantCode())
                .queryParam("txnCurrency", t.getTxnCurrency())
                .queryParam("txnAmount", t.getTxnAmount())
                .build().encode().toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
