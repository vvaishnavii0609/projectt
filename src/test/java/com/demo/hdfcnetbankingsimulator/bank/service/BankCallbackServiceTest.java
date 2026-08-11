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
    // 50ms instead of the real 6000ms - this is exactly why the delay is a constructor
    // parameter rather than a hardcoded literal in BankCallbackService.
    private static final long TEST_DELAY_MILLIS = 50;

    private BankTransaction transaction;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        transaction = new BankTransaction();
        transaction.setClientCode("Client1");
        transaction.setMerchantCode("MERCHANT");
        transaction.setTxnCurrency("INR");
        transaction.setTxnAmount("1.79");
        transaction.setTxnScAmount("0.00");
        transaction.setMerchantRefNo("REF1");
        transaction.setSuccessStaticFlag("N");
        transaction.setFailureStaticFlag("N");
        transaction.setTransactionDate("25/05/2018 12:00:00");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setBankRefNo("HDFC12345");
        transaction.setMessage("");
        transaction.setDynamicUrl("http://localhost:8080/gateway/callback");
    }

    private BankCallbackService serviceUnderTest() {
        return new BankCallbackService(restTemplate, TEST_DELAY_MILLIS);
    }

    @Test
    void success_firesExactlyOneCallback() {
        serviceUnderTest().sendCallback(transaction, SimulationScenario.SUCCESS);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void drop_firesNoCallbackAtAll() throws InterruptedException {
        serviceUnderTest().sendCallback(transaction, SimulationScenario.DROP);
        // give any accidental async call a moment to have fired, then confirm it never did
        Thread.sleep(150);
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void sessionTimeout_firesNoCallbackAtAll() {
        serviceUnderTest().sendCallback(transaction, SimulationScenario.SESSION_TIMEOUT);
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void duplicate_firesExactlyTwoCallbacks() {
        serviceUnderTest().sendCallback(transaction, SimulationScenario.DUPLICATE);
        // DUPLICATE's second fire is synchronous after a 1s internal sleep - real time,
        // not worth mocking out for a test this small, so we just wait for it.
        verify(restTemplate, timeout(3000).times(2)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void delay_firesExactlyOnce_butOnlyAfterTheDelayElapses() {
        serviceUnderTest().sendCallback(transaction, SimulationScenario.DELAY);

        // immediately after calling, nothing should have fired yet
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));

        // but it must fire eventually (within a safe margin of our short test delay)
        verify(restTemplate, timeout(2000).times(1)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void everyFailureScenario_stillFiresExactlyOneCallback() {
        // A failed payment is still real information the Gateway needs told about -
        // only DROP/SESSION_TIMEOUT withhold the callback.
        transaction.setStatus(TransactionStatus.FAILURE);
        transaction.setBankRefNo("0");
        transaction.setMessage("Insufficient Funds");

        serviceUnderTest().sendCallback(transaction, SimulationScenario.FAILURE_INSUFFICIENT_FUNDS);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void callbackDeliveryFailure_doesNotThrow() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("connection refused"));

        // Must not propagate - a real bank doesn't crash its own transaction processing
        // because the gateway's endpoint happened to be unreachable.
        assertDoesNotThrow(() -> serviceUnderTest().sendCallback(transaction, SimulationScenario.SUCCESS));
    }

    @Test
    void usesDynamicUrlWhenPresent() {
        transaction.setDynamicUrl("http://localhost:8080/custom/callback/path");
        serviceUnderTest().sendCallback(transaction, SimulationScenario.SUCCESS);
        verify(restTemplate).postForEntity(eq("http://localhost:8080/custom/callback/path"), any(), eq(String.class));
    }

    @Test
    void fallsBackToDefaultGatewayCallbackUrl_whenDynamicUrlIsBlank() {
        transaction.setDynamicUrl("");
        serviceUnderTest().sendCallback(transaction, SimulationScenario.SUCCESS);
        verify(restTemplate).postForEntity(eq("http://localhost:8080/gateway/callback"), any(), eq(String.class));
    }
}
