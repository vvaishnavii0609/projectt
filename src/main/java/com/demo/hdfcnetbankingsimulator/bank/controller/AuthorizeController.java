package com.demo.hdfcnetbankingsimulator.bank.controller;

import com.demo.hdfcnetbankingsimulator.bank.enums.SimulationScenario;
import com.demo.hdfcnetbankingsimulator.bank.model.BankTransaction;
import com.demo.hdfcnetbankingsimulator.bank.service.BankCallbackService;
import com.demo.hdfcnetbankingsimulator.bank.service.BankService;
import com.demo.hdfcnetbankingsimulator.common.EpiConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class AuthorizeController {

    private final BankService bankService;
    private final BankCallbackService bankCallbackService;

    public AuthorizeController(BankService bankService, BankCallbackService bankCallbackService) {
        this.bankService = bankService;
        this.bankCallbackService = bankCallbackService;
    }

    @PostMapping(value = "/netbanking/authorize", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> authorize(@RequestParam("merchantRefNo") String merchantRefNo,
                                             @RequestParam("scenario") SimulationScenario scenario) {

        System.out.println("[BANK] tester chose scenario=" + scenario + " for MerchantRefNo=" + merchantRefNo);

        BankTransaction t = bankService.processScenario(merchantRefNo, scenario);

        if (scenario == SimulationScenario.SESSION_TIMEOUT) {
            System.out.println("[BANK] SESSION_TIMEOUT - nothing sent to merchant or gateway, by design");
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                    .body("Session Expired. Please login again to complete the transaction.");
        }

        bankCallbackService.sendCallback(t, scenario);

        String orderId = t.getRef1() == null ? "" : t.getRef1();
        String redirectUrl = EpiConstants.MERCHANT_BASE_URL + "/payment-processing.html?orderId=" + orderId;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUrl);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
