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
        // save() just returns whatever it was given, like a real save would after flush
        when(repository.save(any(BankTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        bankService = new BankService(repository);
    }

    private Map<String, String> validRequestParams() {
        Map<String, String> p = new HashMap<>();
        p.put("ClientCode", EpiConstants.CLIENT_CODE);
        p.put("MerchantCode", EpiConstants.MERCHANT_CODE);
        p.put("TxnCurrency", "INR");
        p.put("TxnAmount", "1.79");
        p.put("TxnScAmount", "0.00");
        p.put("MerchantRefNo", "REF123456789012");
        p.put("SuccessStaticFlag", "N");
        p.put("FailureStaticFlag", "N");
        p.put("Date", "25/05/2018 12:00:00");
        p.put("Ref1", "ORD123456789012");
        p.put("DynamicUrl", "http://localhost:8080/gateway/callback");
        p.put("CheckSum", ChecksumUtil.computeForwardChecksum(p, EpiConstants.CHECKSUM_KEY));
        return p;
    }

    // ---------------------------------------------------------------
    // validate()
    // ---------------------------------------------------------------

    @Test
    void validate_acceptsAWellFormedRequest() {
        when(repository.findByMerchantRefNo(any())).thenReturn(Optional.empty());
        assertNull(bankService.validate(validRequestParams()));
    }

    @Test
    void validate_rejectsMissingMandatoryField() {
        Map<String, String> p = validRequestParams();
        p.remove("TxnAmount");
        String reason = bankService.validate(p);
        assertNotNull(reason);
        assertTrue(reason.contains("TxnAmount"));
    }

    @Test
    void validate_rejectsUnknownMerchantCode() {
        Map<String, String> p = validRequestParams();
        p.put("MerchantCode", "SOME_OTHER_MERCHANT");
        p.put("CheckSum", ChecksumUtil.computeForwardChecksum(p, EpiConstants.CHECKSUM_KEY));
        assertEquals("Merchant not found", bankService.validate(p));
    }

    @Test
    void validate_rejectsZeroAmount() {
        Map<String, String> p = validRequestParams();
        p.put("TxnAmount", "0");
        p.put("CheckSum", ChecksumUtil.computeForwardChecksum(p, EpiConstants.CHECKSUM_KEY));
        assertEquals("Invalid transaction amount", bankService.validate(p));
    }

    @Test
    void validate_rejectsNegativeAmount() {
        Map<String, String> p = validRequestParams();
        p.put("TxnAmount", "-50");
        p.put("CheckSum", ChecksumUtil.computeForwardChecksum(p, EpiConstants.CHECKSUM_KEY));
        assertEquals("Invalid transaction amount", bankService.validate(p));
    }

    @Test
    void validate_rejectsNonNumericAmount() {
        Map<String, String> p = validRequestParams();
        p.put("TxnAmount", "abc");
        p.put("CheckSum", ChecksumUtil.computeForwardChecksum(p, EpiConstants.CHECKSUM_KEY));
        assertEquals("Invalid transaction amount", bankService.validate(p));
    }

    @Test
    void validate_rejectsUnsupportedCurrency() {
        Map<String, String> p = validRequestParams();
        p.put("TxnCurrency", "USD");
        p.put("CheckSum", ChecksumUtil.computeForwardChecksum(p, EpiConstants.CHECKSUM_KEY));
        assertEquals("Unsupported currency", bankService.validate(p));
    }

    @Test
    void validate_rejectsWrongChecksum() {
        Map<String, String> p = validRequestParams();
        p.put("CheckSum", "999999999"); // deliberately wrong, doesn't match the real formula
        assertEquals("Checksum validation failed", bankService.validate(p));
    }

    @Test
    void validate_rejectsDuplicateSuccessfulTransaction() {
        Map<String, String> p = validRequestParams();

        BankTransaction existingSuccess = new BankTransaction();
        existingSuccess.setClientCode(p.get("ClientCode"));
        existingSuccess.setTransactionDate(p.get("Date"));
        existingSuccess.setStatus(TransactionStatus.SUCCESS);

        when(repository.findByMerchantRefNo(p.get("MerchantRefNo"))).thenReturn(Optional.of(existingSuccess));

        assertEquals("Duplicate transaction", bankService.validate(p));
    }

    @Test
    void validate_allowsRetryAfterAFailedAttempt_notADuplicate() {
        // Ch.4: the uniqueness rule only applies to SUCCESSFUL transactions - a prior
        // failure (e.g. insufficient funds) must NOT block a retry with the same ref.
        Map<String, String> p = validRequestParams();

        BankTransaction existingFailure = new BankTransaction();
        existingFailure.setClientCode(p.get("ClientCode"));
        existingFailure.setTransactionDate(p.get("Date"));
        existingFailure.setStatus(TransactionStatus.FAILURE);

        when(repository.findByMerchantRefNo(p.get("MerchantRefNo"))).thenReturn(Optional.of(existingFailure));

        assertNull(bankService.validate(p));
    }

    // ---------------------------------------------------------------
    // accept()
    // ---------------------------------------------------------------

    @Test
    void accept_storesRef1SoItCanBeEchoedBackLater() {
        BankTransaction t = bankService.accept(validRequestParams());
        assertEquals("ORD123456789012", t.getRef1());
    }

    @Test
    void accept_startsAsInitiatedWithZeroBankRefNo() {
        BankTransaction t = bankService.accept(validRequestParams());
        assertEquals(TransactionStatus.INITIATED, t.getStatus());
        assertEquals("0", t.getBankRefNo());
        assertEquals("", t.getMessage());
        assertNotNull(t.getTransactionId());
    }

    // ---------------------------------------------------------------
    // processScenario() - every scenario, grouped by category
    // ---------------------------------------------------------------

    private BankTransaction transactionReadyForScenario() {
        BankTransaction t = bankService.accept(validRequestParams());
        when(repository.findByMerchantRefNo(t.getMerchantRefNo())).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void processScenario_success_generatesNonZeroBankRefNo() {
        BankTransaction t = transactionReadyForScenario();
        BankTransaction result = bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.SUCCESS);

        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertNotEquals("0", result.getBankRefNo());
        assertEquals("", result.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = SimulationScenario.class, names = {
            "FAILURE_INSUFFICIENT_FUNDS", "FAILURE_AUTH", "FAILURE_ACCOUNT_BLOCKED",
            "FAILURE_INVALID_ACCOUNT", "FAILURE_TXN_LIMIT_EXCEEDED",
            "FAILURE_BANK_ERROR", "FAILURE_BANK_MAINTENANCE"
    })
    void processScenario_everyFailureReason_producesZeroBankRefNoAndANonBlankMessage(SimulationScenario scenario) {
        BankTransaction t = transactionReadyForScenario();
        BankTransaction result = bankService.processScenario(t.getMerchantRefNo(), scenario);

        // Ch.2-D's golden rule, restated as a test: failure ALWAYS means BankRefNo = "0"
        assertEquals(TransactionStatus.FAILURE, result.getStatus());
        assertEquals("0", result.getBankRefNo());
        assertFalse(result.getMessage().isBlank(), scenario + " must explain why it failed");
    }

    @Test
    void processScenario_pending_isNeitherSuccessNorFailure() {
        BankTransaction t = transactionReadyForScenario();
        BankTransaction result = bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.PENDING);

        assertEquals(TransactionStatus.PENDING, result.getStatus());
        assertEquals("0", result.getBankRefNo());
    }

    @Test
    void processScenario_sessionTimeout_looksLikeAFailureButWithItsOwnStatus() {
        BankTransaction t = transactionReadyForScenario();
        BankTransaction result = bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.SESSION_TIMEOUT);

        assertEquals(TransactionStatus.SESSION_EXPIRED, result.getStatus());
        assertEquals("0", result.getBankRefNo());
    }

    @Test
    void processScenario_delayDropDuplicate_allSucceedInternally() {
        // These three are about CALLBACK RELIABILITY, not the payment outcome itself -
        // the underlying transaction is always a real SUCCESS.
        for (SimulationScenario s : new SimulationScenario[]{
                SimulationScenario.DELAY, SimulationScenario.DROP, SimulationScenario.DUPLICATE}) {
            BankTransaction t = transactionReadyForScenario();
            BankTransaction result = bankService.processScenario(t.getMerchantRefNo(), s);
            assertEquals(TransactionStatus.SUCCESS, result.getStatus(), s + " must succeed internally");
            assertNotEquals("0", result.getBankRefNo());
        }
    }

    @Test
    void processScenario_tamperAmount_succeedsInternallyButSetsTheTamperFlag() {
        BankTransaction t = transactionReadyForScenario();
        BankTransaction result = bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.TAMPER_AMOUNT);

        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertTrue(result.isTamperAmountInResponse());
        assertFalse(result.isTamperChecksumInResponse());
        // The bank's OWN record of the amount must stay correct - only the outgoing
        // response gets corrupted, never the underlying transaction data.
        assertEquals("1.79", result.getTxnAmount());
    }

    @Test
    void processScenario_tamperChecksum_succeedsInternallyButSetsTheTamperFlag() {
        BankTransaction t = transactionReadyForScenario();
        BankTransaction result = bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.TAMPER_CHECKSUM);

        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertTrue(result.isTamperChecksumInResponse());
        assertFalse(result.isTamperAmountInResponse());
    }

    // ---------------------------------------------------------------
    // verify()
    // ---------------------------------------------------------------

    @Test
    void verify_returnsBlankFlgSuccess_whenNoTransactionExists() {
        when(repository.findByMerchantRefNo("NOPE")).thenReturn(Optional.empty());

        VerificationResponse r = bankService.verify(
                EpiConstants.CLIENT_CODE, EpiConstants.MERCHANT_CODE, "NOPE",
                "25/05/2018 12:00:00", "1.79", "XTXTV01");

        assertEquals("", r.getFlgSuccess());
    }

    @Test
    void verify_returnsS_forAMatchingSuccessfulTransaction() {
        BankTransaction t = transactionReadyForScenario();
        bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.SUCCESS);

        VerificationResponse r = bankService.verify(
                t.getClientCode(), t.getMerchantCode(), t.getMerchantRefNo(),
                t.getTransactionDate(), t.getTxnAmount(), "XTXTV01");

        assertEquals("S", r.getFlgSuccess());
        assertNotEquals("0", r.getBankRefNo());
        assertNotNull(r.getCheckSum());
    }

    @Test
    void verify_returnsF_whenAmountDoesNotMatch_thisIsTheAmountTamperingCase() {
        BankTransaction t = transactionReadyForScenario();
        bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.SUCCESS);

        VerificationResponse r = bankService.verify(
                t.getClientCode(), t.getMerchantCode(), t.getMerchantRefNo(),
                t.getTransactionDate(), "999.99", "XTXTV01"); // wrong amount on purpose

        assertEquals("F", r.getFlgSuccess());
        assertEquals("0", r.getBankRefNo());
        assertTrue(r.getMessage().toLowerCase().contains("amount"));
    }

    @Test
    void verify_returnsF_whenClientCodeDoesNotMatch() {
        BankTransaction t = transactionReadyForScenario();
        bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.SUCCESS);

        VerificationResponse r = bankService.verify(
                "SomeOtherClient", t.getMerchantCode(), t.getMerchantRefNo(),
                t.getTransactionDate(), t.getTxnAmount(), "XTXTV01");

        assertEquals("F", r.getFlgSuccess());
    }

    @Test
    void verify_returnsF_forAFailedTransaction() {
        BankTransaction t = transactionReadyForScenario();
        bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.FAILURE_INSUFFICIENT_FUNDS);

        VerificationResponse r = bankService.verify(
                t.getClientCode(), t.getMerchantCode(), t.getMerchantRefNo(),
                t.getTransactionDate(), t.getTxnAmount(), "XTXTV01");

        assertEquals("F", r.getFlgSuccess());
        assertEquals("0", r.getBankRefNo());
    }

    @Test
    void verify_returnsP_forAPendingTransaction() {
        BankTransaction t = transactionReadyForScenario();
        bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.PENDING);

        VerificationResponse r = bankService.verify(
                t.getClientCode(), t.getMerchantCode(), t.getMerchantRefNo(),
                t.getTransactionDate(), t.getTxnAmount(), "XTXTV01");

        assertEquals("P", r.getFlgSuccess());
    }

    @Test
    void verify_tamperAmountScenario_actuallyReportsADifferentAmountThanWhatWasStored() {
        BankTransaction t = transactionReadyForScenario();
        bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.TAMPER_AMOUNT);

        // Caller asks using the ORIGINAL correct amount, exactly as the Gateway would -
        // it has no way of knowing in advance that the bank is about to lie.
        VerificationResponse r = bankService.verify(
                t.getClientCode(), t.getMerchantCode(), t.getMerchantRefNo(),
                t.getTransactionDate(), "1.79", "XTXTV01");

        assertNotEquals("1.79", r.getTxnAmount(), "TAMPER_AMOUNT must report a different amount than what's on file");

        // And the checksum must be SELF-CONSISTENT with the tampered amount (this is
        // what makes the scenario a genuine test of the Gateway's amount cross-check,
        // not just a checksum failure in disguise).
        String recomputed = ChecksumUtil.computeReturnChecksum(
                r.getClientCode(), r.getMerchantCode(), r.getTxnCurrency(), r.getTxnAmount(), r.getTxnScAmount(),
                r.getMerchantRefNo(), r.getSuccessStaticFlag(), r.getFailureStaticFlag(), r.getDate(),
                r.getBankRefNo(), r.getMessage(), EpiConstants.CHECKSUM_KEY);
        assertEquals(recomputed, r.getCheckSum(),
                "the checksum must validate correctly against the (tampered) amount actually sent");
    }

    @Test
    void verify_tamperChecksumScenario_checksumWillNeverValidate() {
        BankTransaction t = transactionReadyForScenario();
        bankService.processScenario(t.getMerchantRefNo(), SimulationScenario.TAMPER_CHECKSUM);

        VerificationResponse r = bankService.verify(
                t.getClientCode(), t.getMerchantCode(), t.getMerchantRefNo(),
                t.getTransactionDate(), t.getTxnAmount(), "XTXTV01");

        String recomputed = ChecksumUtil.computeReturnChecksum(
                r.getClientCode(), r.getMerchantCode(), r.getTxnCurrency(), r.getTxnAmount(), r.getTxnScAmount(),
                r.getMerchantRefNo(), r.getSuccessStaticFlag(), r.getFailureStaticFlag(), r.getDate(),
                r.getBankRefNo(), r.getMessage(), EpiConstants.CHECKSUM_KEY);

        assertNotEquals(recomputed, r.getCheckSum(),
                "TAMPER_CHECKSUM must produce a checksum that fails independent recomputation");
    }
}
