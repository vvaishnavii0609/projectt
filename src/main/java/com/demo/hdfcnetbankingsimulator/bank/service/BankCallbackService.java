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

        System.out.println();
        System.out.println("============================================================");
        System.out.println("                    BANK CALLBACK PROCESS");
        System.out.println("============================================================");
        System.out.println("[BANK] Transaction processing completed.");
        System.out.println("[BANK] Scenario        : " + scenario);
        System.out.println("[BANK] Merchant Ref No : " + t.getMerchantRefNo());
        System.out.println("[BANK] Bank Ref No     : " + t.getBankRefNo());
        System.out.println("[BANK] Final Status    : " + t.getStatus());
        System.out.println("------------------------------------------------------------");


        switch (scenario) {

            case DROP, SESSION_TIMEOUT -> {

                System.out.println("[BANK] CALLBACK NOT SENT");
                System.out.println("[BANK] Scenario requires the callback to be dropped.");
                System.out.println("[BANK] Payment Gateway will NOT receive the callback.");
                System.out.println("[BANK] Gateway Verify must recover the transaction.");
            }


            case DELAY -> {

                System.out.println("[BANK] CALLBACK INITIATED");
                System.out.println("[BANK] Callback will be sent to Payment Gateway after "
                        + delayMillis + " ms.");
                System.out.println("[BANK] Merchant Ref No : " + t.getMerchantRefNo());
                System.out.println("[BANK] Callback URL    : " + callbackUrl);

                new Thread(() -> {
                    try {
                        Thread.sleep(delayMillis);
                    } catch (InterruptedException ignored) {
                    }

                    fireOnce(t, callbackUrl);
                }).start();
            }


            case DUPLICATE -> {

                System.out.println("[BANK] CALLBACK INITIATED");
                System.out.println("[BANK] Duplicate callback scenario selected.");
                System.out.println("[BANK] Same transaction response will be sent TWICE.");
                System.out.println("[BANK] Payment Gateway must handle the duplicate safely.");

                fireOnce(t, callbackUrl);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }

                fireOnce(t, callbackUrl);
            }


            default -> {

                System.out.println("[BANK] CALLBACK INITIATED");
                System.out.println("[BANK] Bank will send the transaction result to Payment Gateway.");

                fireOnce(t, callbackUrl);
            }
        }

        System.out.println("============================================================");
        System.out.println();
    }


    private void fireOnce(BankTransaction t, String callbackUrl) {

        try {

            // ---------------------------------------------------------
            // STEP 1: Prepare the amount that will be sent in callback
            // ---------------------------------------------------------

            String outgoingAmount = t.getTxnAmount();

            if (t.isTamperAmountInResponse()) {

                outgoingAmount = new BigDecimal(t.getTxnAmount())
                        .add(new BigDecimal("500.00"))
                        .toPlainString();

                System.out.println();
                System.out.println("[BANK] TEST SCENARIO: AMOUNT TAMPERING");
                System.out.println("[BANK] Actual transaction amount : " + t.getTxnAmount());
                System.out.println("[BANK] Amount sent in callback     : " + outgoingAmount);
                System.out.println("[BANK] Gateway should detect the amount mismatch.");
            }


            // ---------------------------------------------------------
            // STEP 2: Create callback payload
            // ---------------------------------------------------------

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


            // ---------------------------------------------------------
            // STEP 3: Generate checksum
            // ---------------------------------------------------------

            String checksum = ChecksumUtil.computeReturnChecksum(
                    t.getClientCode(),
                    t.getMerchantCode(),
                    t.getTxnCurrency(),
                    outgoingAmount,
                    t.getTxnScAmount(),
                    t.getMerchantRefNo(),
                    t.getSuccessStaticFlag(),
                    t.getFailureStaticFlag(),
                    t.getTransactionDate(),
                    t.getBankRefNo(),
                    t.getMessage(),
                    EpiConstants.CHECKSUM_KEY
            );


            // ---------------------------------------------------------
            // STEP 4: Deliberately corrupt checksum for test scenario
            // ---------------------------------------------------------

            if (t.isTamperChecksumInResponse()) {

                System.out.println();
                System.out.println("[BANK] TEST SCENARIO: CHECKSUM TAMPERING");
                System.out.println("[BANK] Original checksum generated.");
                System.out.println("[BANK] Checksum will now be deliberately corrupted.");
                System.out.println("[BANK] Gateway should reject the untrusted response.");

                checksum = ChecksumUtil.tamper(checksum);
            }

            payload.setCheckSum(checksum);


            // ---------------------------------------------------------
            // STEP 5: Display callback information
            // ---------------------------------------------------------

            System.out.println();
            System.out.println("------------------------------------------------------------");
            System.out.println("              BANK -> PAYMENT GATEWAY");
            System.out.println("                 CALLBACK INITIATED");
            System.out.println("------------------------------------------------------------");

            System.out.println("[BANK] Bank is sending transaction result to Payment Gateway.");
            System.out.println("[BANK] Callback URL    : " + callbackUrl);
            System.out.println("[BANK] Client Code     : " + payload.getClientCode());
            System.out.println("[BANK] Merchant Code   : " + payload.getMerchantCode());
            System.out.println("[BANK] Merchant Ref No  : " + payload.getMerchantRefNo());
            System.out.println("[BANK] Transaction Amt  : " + payload.getTxnAmount());
            System.out.println("[BANK] Currency         : " + payload.getTxnCurrency());
            System.out.println("[BANK] Status           : " + payload.getStatus());
            System.out.println("[BANK] Bank Ref No      : " + payload.getBankRefNo());
            System.out.println("[BANK] Message          : " + payload.getMessage());
            System.out.println("[BANK] Checksum         : " + payload.getCheckSum());

            System.out.println("------------------------------------------------------------");


            // ---------------------------------------------------------
            // STEP 6: Create HTTP headers
            // ---------------------------------------------------------

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            System.out.println("[BANK] HTTP Method      : POST");
            System.out.println("[BANK] Content-Type     : application/json");
            System.out.println("[BANK] Preparing server-to-server callback...");
            System.out.println("------------------------------------------------------------");


            // ---------------------------------------------------------
            // STEP 7: BANK -> PAYMENT GATEWAY
            // ---------------------------------------------------------

            System.out.println();
            System.out.println("[BANK] Sending callback to Payment Gateway...");
            System.out.println("[BANK] Bank Backend");
            System.out.println("        |");
            System.out.println("        | HTTP POST /gateway/callback");
            System.out.println("        v");
            System.out.println("[PAYMENT GATEWAY]");


            restTemplate.postForEntity(
                    callbackUrl,
                    new HttpEntity<>(payload, headers),
                    String.class
            );


            // ---------------------------------------------------------
            // STEP 8: Callback successfully delivered
            // ---------------------------------------------------------

            System.out.println();
            System.out.println("------------------------------------------------------------");
            System.out.println("[BANK] CALLBACK SENT SUCCESSFULLY");
            System.out.println("------------------------------------------------------------");
            System.out.println("[BANK] Transaction result successfully delivered to Payment Gateway.");
            System.out.println("[BANK] Merchant Ref No : " + t.getMerchantRefNo());
            System.out.println("[BANK] Payment Gateway : " + callbackUrl);
            System.out.println("[BANK] Gateway can now process the callback.");
            System.out.println("------------------------------------------------------------");
            System.out.println();


        } catch (Exception e) {

            // ---------------------------------------------------------
            // CALLBACK DELIVERY FAILED
            // ---------------------------------------------------------

            System.out.println();
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("              BANK CALLBACK DELIVERY FAILED");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("[BANK] Could not deliver transaction result to Payment Gateway.");
            System.out.println("[BANK] Merchant Ref No : " + t.getMerchantRefNo());
            System.out.println("[BANK] Callback URL    : " + callbackUrl);
            System.out.println("[BANK] Reason          : " + e.getMessage());
            System.out.println("[BANK] Transaction remains processed at Bank.");
            System.out.println("[BANK] Gateway Verify can recover the final status.");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println();
        }
    }
}
