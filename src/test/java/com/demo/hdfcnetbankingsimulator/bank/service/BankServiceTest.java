package com.demo.hdfcnetbankingsimulator.bank.service;

import com.demo.hdfcnetbankingsimulator.bank.dto.VerificationResponse;
import com.demo.hdfcnetbankingsimulator.bank.enums.SimulationScenario;
import com.demo.hdfcnetbankingsimulator.bank.enums.TransactionStatus;
import com.demo.hdfcnetbankingsimulator.bank.model.BankTransaction;
import com.demo.hdfcnetbankingsimulator.bank.repository.BankTransactionRepository;
import com.demo.hdfcnetbankingsimulator.common.EpiConstants;
import com.demo.hdfcnetbankingsimulator.common.util.ChecksumUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BankServiceTest {

    private BankTransactionRepository repository;
    private BankService bankService;
    @BeforeEach
    void setUp() {
        repository = mock(BankTransactionRepository.class);

        when(repository.save(any(BankTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        bankService = new BankService(repository);
    }

    private Map<String, String> validRequestParams() {
        Map<String, String> p = new HashMap<>();

        p.put("ClientCode", EpiConstants.CLIENT_CODE);
        p.put("MerchantCode", EpiConstants.MERCHANT_CODE);
        p.put("TxnCurrency", "INR");
        p.put("TxnAmount", "1.79");
        p.put("TxnScAmount", "0.00");
        p.put("MerchantRefNo", "REF123");
        p.put("SuccessStaticFlag", "N");
        p.put("FailureStaticFlag", "N");
        p.put("Date", "25/05/2018 12:00:00");
        p.put("Ref1", "ORD123");

        p.put(
                "CheckSum",
                ChecksumUtil.computeForwardChecksum(
                        p,
                        EpiConstants.CHECKSUM_KEY
                )
        );

        return p;
    }

    // TEST 1
    @Test
    void validate_acceptsValidRequest() {

        when(repository.findByMerchantRefNo(any()))
                .thenReturn(Optional.empty());

        String result = bankService.validate(validRequestParams());

        assertNull(result);
    }

    // TEST 2
    @Test
    void validate_rejectsMissingAmount() {

        Map<String, String> params = validRequestParams();

        params.remove("TxnAmount");

        String result = bankService.validate(params);

        assertNotNull(result);
        assertTrue(result.contains("TxnAmount"));
    }

    // TEST 3
    @Test
    void accept_createsInitiatedTransaction() {

        BankTransaction result =
                bankService.accept(validRequestParams());

        assertEquals(
                TransactionStatus.INITIATED,
                result.getStatus()
        );

        assertEquals("0", result.getBankRefNo());

        assertNotNull(result.getTransactionId());
    }

    // TEST 4
    @Test
    void processScenario_successMakesTransactionSuccessful() {

        BankTransaction t =
                bankService.accept(validRequestParams());

        when(repository.findByMerchantRefNo(t.getMerchantRefNo()))
                .thenReturn(Optional.of(t));

        BankTransaction result =
                bankService.processScenario(
                        t.getMerchantRefNo(),
                        SimulationScenario.SUCCESS
                );

        assertEquals(
                TransactionStatus.SUCCESS,
                result.getStatus()
        );

        assertNotEquals("0", result.getBankRefNo());
    }

    // TEST 5
    @Test
    void verify_returnsSForSuccessfulTransaction() {

        BankTransaction t =
                bankService.accept(validRequestParams());

        when(repository.findByMerchantRefNo(t.getMerchantRefNo()))
                .thenReturn(Optional.of(t));

        bankService.processScenario(
                t.getMerchantRefNo(),
                SimulationScenario.SUCCESS
        );

        VerificationResponse result =
                bankService.verify(
                        t.getClientCode(),
                        t.getMerchantCode(),
                        t.getMerchantRefNo(),
                        t.getTransactionDate(),
                        t.getTxnAmount(),
                        "XTXTV01"
                );

        assertEquals("S", result.getFlgSuccess());
    }

}
