package com.demo.hdfcnetbankingsimulator.bank.service;

import com.demo.hdfcnetbankingsimulator.bank.enums.SimulationScenario;
import com.demo.hdfcnetbankingsimulator.bank.enums.TransactionStatus;
import com.demo.hdfcnetbankingsimulator.bank.model.BankTransaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BankCallbackServiceTest {

    private RestTemplate restTemplate;
    private BankCallbackService bankCallbackService;
    private BankTransaction transaction;

    @BeforeEach
    void setUp() {

        restTemplate = mock(RestTemplate.class);

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("OK"));

        bankCallbackService = new BankCallbackService(restTemplate, 50);

        transaction = new BankTransaction();

        transaction.setMerchantRefNo("REF1");
        transaction.setMerchantCode("MERCHANT");
        transaction.setTxnCurrency("INR");
        transaction.setTxnAmount("1.79");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setBankRefNo("HDFC12345");
        transaction.setMessage("");
        transaction.setDynamicUrl(
                "http://localhost:8080/gateway/callback"
        );
    }

    @Test
    void success_sendsCallbackOnce() {

        bankCallbackService.sendCallback(
                transaction,
                SimulationScenario.SUCCESS
        );

        verify(restTemplate, times(1))
                .postForEntity(
                        anyString(),
                        any(),
                        eq(String.class)
                );
    }

    @Test
    void drop_doesNotSendCallback() {

        bankCallbackService.sendCallback(
                transaction,
                SimulationScenario.DROP
        );

        verify(restTemplate, never())
                .postForEntity(
                        anyString(),
                        any(),
                        eq(String.class)
                );
    }

    @Test
    void sessionTimeout_doesNotSendCallback() {

        bankCallbackService.sendCallback(
                transaction,
                SimulationScenario.SESSION_TIMEOUT
        );

        verify(restTemplate, never())
                .postForEntity(
                        anyString(),
                        any(),
                        eq(String.class)
                );
    }

    @Test
    void failure_sendsCallbackOnce() {

        transaction.setStatus(TransactionStatus.FAILURE);
        transaction.setBankRefNo("0");
        transaction.setMessage("Insufficient Funds");

        bankCallbackService.sendCallback(
                transaction,
                SimulationScenario.FAILURE_INSUFFICIENT_FUNDS
        );

        verify(restTemplate, times(1))
                .postForEntity(
                        anyString(),
                        any(),
                        eq(String.class)
                );
    }
}