package com.demo.hdfcnetbankingsimulator.bank.controller;

import com.demo.hdfcnetbankingsimulator.bank.dto.VerificationResponse;
import com.demo.hdfcnetbankingsimulator.bank.service.BankService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VerifyController {

    private final BankService bankService;

    public VerifyController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/netbanking/verify")
    public VerificationResponse verify(@RequestParam("ClientCode") String clientCode,
                                        @RequestParam("MerchantCode") String merchantCode,
                                        @RequestParam("MerchantRefNo") String merchantRefNo,
                                        @RequestParam("Date") String date,
                                        @RequestParam("TxnAmount") String txnAmount,
                                        @RequestParam(value = "TransactionId", required = false) String transactionId,
                                        @RequestParam(value = "FlgVerify", required = false) String flgVerify) {

        System.out.println("[BANK <- verify request] MerchantRefNo=" + merchantRefNo + " TxnAmount=" + txnAmount);
        VerificationResponse response = bankService.verify(clientCode, merchantCode, merchantRefNo, date, txnAmount, transactionId);
        System.out.println("[BANK -> verify response] flgSuccess=" + response.getFlgSuccess()
                + " bankRefNo=" + response.getBankRefNo());
        return response;
    }
}
