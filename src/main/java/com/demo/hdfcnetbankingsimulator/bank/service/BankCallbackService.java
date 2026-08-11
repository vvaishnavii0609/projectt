package com.demo.hdfcnetbankingsimulator.bank.service;

import com.demo.hdfcnetbankingsimulator.bank.dto.CallbackPayload;
import com.demo.hdfcnetbankingsimulator.bank.enums.SimulationScenario;
import com.demo.hdfcnetbankingsimulator.bank.model.BankTransaction;
import com.demo.hdfcnetbankingsimulator.common.EpiConstants;
import com.demo.hdfcnetbankingsimulator.common.util.ChecksumUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Fires the backend (server-to-server) callback at the Payment Gateway.
 * BankService decides WHAT happened; this class decides HOW RELIABLY the gateway
 * is told about it - exactly where DELAY / DROP / DUPLICATE fault injection lives,
 * and where TAMPER_AMOUNT / TAMPER_CHECKSUM actually corrupt the outgoing payload.
 *
 * The delay is a field (not a hardcoded literal) specifically so tests can override
 * it to a few milliseconds instead of waiting 6 real seconds.
 */
@Service
public class BankCallbackService {

    private final RestTemplate restTemplate;
    private final long delayMillis;

    public BankCallbackService(RestTemplate restTemplate,
                                @Value("${bank.callback-delay-millis:6000}") long delayMillis) {
        this.restTemplate = restTemplate;
        this.delayMillis = delayMillis;
    }

    public void sendCallback(BankTransaction t, SimulationScenario scenario) {

        String callbackUrl = (t.getDynamicUrl() != null && !t.getDynamicUrl().isBlank())
                ? t.getDynamicUrl()
                : EpiConstants.GATEWAY_BASE_URL + "/gateway/callback";

        switch (scenario) {
            case DROP, SESSION_TIMEOUT -> System.out.println("[BANK -> callback] " + scenario + " for "
                    + t.getMerchantRefNo() + " - deliberately NOT calling Gateway. Only Verify can recover this.");
            case DELAY -> {
                System.out.println("[BANK -> callback] DELAY for " + t.getMerchantRefNo()
                        + " - will fire in " + delayMillis + "ms");
                new Thread(() -> {
                    try { Thread.sleep(delayMillis); } catch (InterruptedException ignored) {}
                    fireOnce(t, callbackUrl);
                }).start();
            }
            case DUPLICATE -> {
                System.out.println("[BANK -> callback] DUPLICATE for " + t.getMerchantRefNo() + " - firing twice");
                fireOnce(t, callbackUrl);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                fireOnce(t, callbackUrl); // same outcome, sent twice - gateway must be idempotent
            }
            default -> fireOnce(t, callbackUrl); // SUCCESS, all FAILURE_*, PENDING, TAMPER_AMOUNT, TAMPER_CHECKSUM
        }
    }

    private void fireOnce(BankTransaction t, String callbackUrl) {
        System.out.println("[BANK -> callback] POST " + callbackUrl + "  merchantRefNo=" + t.getMerchantRefNo()
                + " status=" + t.getStatus() + " bankRefNo=" + t.getBankRefNo());
        try {
            // What actually goes out on the wire - starts as the truth, then the two
            // TAMPER_* scenarios corrupt it, exactly mirroring BankService.verify()'s logic
            // so a Gateway that only sees the callback (not Verify) would ALSO be fooled -
            // which is precisely why Verify is mandatory, not optional.
            String outgoingAmount = t.getTxnAmount();
            if (t.isTamperAmountInResponse()) {
                outgoingAmount = new BigDecimal(t.getTxnAmount()).add(new BigDecimal("500.00")).toPlainString();
            }

            CallbackPayload payload = new CallbackPayload();
            payload.setClientCode(t.getClientCode());
            payload.setMerchantCode(t.getMerchantCode());
            payload.setMerchantRefNo(t.getMerchantRefNo());
            payload.setTxnCurrency(t.getTxnCurrency());
            payload.setTxnAmount(outgoingAmount);
            payload.setTxnScAmount(t.getTxnScAmount());
            payload.setSuccessStaticFlag(t.getSuccessStaticFlag());
            payload.setFailureStaticFlag(t.getFailureStaticFlag());
            payload.setDate(t.getTransactionDate());
            payload.setStatus(t.getStatus().name());
            payload.setBankRefNo(t.getBankRefNo());
            payload.setMessage(t.getMessage());

            String checksum = ChecksumUtil.computeReturnChecksum(
                    t.getClientCode(), t.getMerchantCode(), t.getTxnCurrency(), outgoingAmount, t.getTxnScAmount(),
                    t.getMerchantRefNo(), t.getSuccessStaticFlag(), t.getFailureStaticFlag(), t.getTransactionDate(),
                    t.getBankRefNo(), t.getMessage(), EpiConstants.CHECKSUM_KEY);

            if (t.isTamperChecksumInResponse()) {
                checksum = ChecksumUtil.tamper(checksum);
            }
            payload.setCheckSum(checksum);

            System.out.println("[BANK -> callback] FULL PAYLOAD: " + payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(callbackUrl, new HttpEntity<>(payload, headers), String.class);
            System.out.println("[BANK -> callback] delivered OK for " + t.getMerchantRefNo());
        } catch (Exception e) {
            // A real bank doesn't crash its own processing because the gateway's endpoint
            // happened to be down - it logs and moves on. The Gateway's Verify call recovers this.
            System.out.println("[BANK -> callback] delivery FAILED for " + t.getMerchantRefNo() + ": " + e.getMessage());
        }
    }
}
