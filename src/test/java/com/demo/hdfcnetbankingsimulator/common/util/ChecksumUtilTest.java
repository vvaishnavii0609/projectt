package com.demo.hdfcnetbankingsimulator.common.util;

import com.demo.hdfcnetbankingsimulator.gateway.dto.HdfcPaymentRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChecksumUtilTest {

    private HdfcPaymentRequest sampleRequest() {
        HdfcPaymentRequest r = new HdfcPaymentRequest();
        r.setClientCode("Client1");
        r.setMerchantCode("MERCHANT");
        r.setTxnCurrency("INR");
        r.setTxnAmount("1.79");
        r.setTxnScAmount("0.00");
        r.setMerchantRefNo("REF123456789012");
        r.setSuccessStaticFlag("N");
        r.setFailureStaticFlag("N");
        r.setDate("25/05/2018 12:00:00");
        r.setRef1("ORD123456789012");
        r.setRef2(""); r.setRef3(""); r.setRef4(""); r.setRef5(""); r.setRef6("");
        r.setRef7(""); r.setRef8(""); r.setRef9(""); r.setRef10(""); r.setRef11("");
        r.setDate1(""); r.setDate2("");
        r.setDisplayDetails("");
        r.setDetails1(""); r.setDetails2(""); r.setDetails3("");
        r.setDynamicUrl("http://localhost:8080/gateway/callback");
        return r;
    }

    @Test
    void sameInputsAlwaysProduceTheSameChecksum() {
        HdfcPaymentRequest r = sampleRequest();
        String c1 = ChecksumUtil.computeForwardChecksum(r, "654321");
        String c2 = ChecksumUtil.computeForwardChecksum(r, "654321");
        assertEquals(c1, c2, "CRC32 must be deterministic for identical input");
    }

    @Test
    void differentChecksumKeyProducesDifferentChecksum() {
        HdfcPaymentRequest r = sampleRequest();
        String c1 = ChecksumUtil.computeForwardChecksum(r, "654321");
        String c2 = ChecksumUtil.computeForwardChecksum(r, "999999");
        assertNotEquals(c1, c2, "Changing the shared secret must change the checksum");
    }

    @Test
    void changingAnySingleFieldChangesTheChecksum() {
        HdfcPaymentRequest r = sampleRequest();
        String original = ChecksumUtil.computeForwardChecksum(r, "654321");

        r.setTxnAmount("999.99"); // tamper the amount, nothing else
        String tampered = ChecksumUtil.computeForwardChecksum(r, "654321");

        assertNotEquals(original, tampered,
                "Amount tampering must be detectable purely from the checksum changing");
    }

    @Test
    void fieldOrderMatters_swappingTwoFieldValuesChangesChecksum() {
        // Concatenation-based checksums are order-sensitive by construction: if two
        // fields happen to have their values swapped, the checksum must differ, proving
        // the formula isn't accidentally order-independent (e.g. via some kind of sum).
        HdfcPaymentRequest a = sampleRequest();
        a.setSuccessStaticFlag("Y");
        a.setFailureStaticFlag("N");

        HdfcPaymentRequest b = sampleRequest();
        b.setSuccessStaticFlag("N");
        b.setFailureStaticFlag("Y");

        String checksumA = ChecksumUtil.computeForwardChecksum(a, "654321");
        String checksumB = ChecksumUtil.computeForwardChecksum(b, "654321");

        assertNotEquals(checksumA, checksumB);
    }

    @Test
    void mapOverloadAndDtoOverloadAgreeForTheSameLogicalRequest() {
        // The Gateway builds the checksum from an HdfcPaymentRequest object; the Bank
        // validates it from a raw query-param Map. These MUST produce identical results
        // for the same data, or every real request would be rejected.
        HdfcPaymentRequest r = sampleRequest();
        String fromDto = ChecksumUtil.computeForwardChecksum(r, "654321");

        Map<String, String> params = new HashMap<>();
        params.put("ClientCode", r.getClientCode());
        params.put("MerchantCode", r.getMerchantCode());
        params.put("TxnCurrency", r.getTxnCurrency());
        params.put("TxnAmount", r.getTxnAmount());
        params.put("TxnScAmount", r.getTxnScAmount());
        params.put("MerchantRefNo", r.getMerchantRefNo());
        params.put("SuccessStaticFlag", r.getSuccessStaticFlag());
        params.put("FailureStaticFlag", r.getFailureStaticFlag());
        params.put("Date", r.getDate());
        params.put("Ref1", r.getRef1());
        params.put("DynamicUrl", r.getDynamicUrl());

        String fromMap = ChecksumUtil.computeForwardChecksum(params, "654321");

        assertEquals(fromDto, fromMap, "Gateway's request-building checksum and Bank's "
                + "validation checksum must agree for identical data, or the real integration breaks");
    }

    @Test
    void returnChecksumChangesWhenBankRefNoChanges() {
        String c1 = ChecksumUtil.computeReturnChecksum(
                "Client1", "MERCHANT", "INR", "1.79", "0.00", "REF123",
                "N", "N", "25/05/2018 12:00:00", "HDFC1000", "", "654321");
        String c2 = ChecksumUtil.computeReturnChecksum(
                "Client1", "MERCHANT", "INR", "1.79", "0.00", "REF123",
                "N", "N", "25/05/2018 12:00:00", "0", "Insufficient Funds", "654321");

        assertNotEquals(c1, c2);
    }

    @Test
    void tamperHelperProducesAValueThatWillNeverEqualARecomputedChecksum() {
        String real = ChecksumUtil.computeReturnChecksum(
                "Client1", "MERCHANT", "INR", "1.79", "0.00", "REF123",
                "N", "N", "25/05/2018 12:00:00", "HDFC1000", "", "654321");
        String tampered = ChecksumUtil.tamper(real);
        assertNotEquals(real, tampered);
    }
}
