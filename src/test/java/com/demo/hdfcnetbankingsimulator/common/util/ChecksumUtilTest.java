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
    void crc32_shouldReturnExpectedChecksum() {

        String data = "hello";
        String actual = ChecksumUtil.crc32(data);
        assertEquals("907060870", actual);
    }

    @Test
    void crc32_shouldReturnSameChecksumForSameInput() {

        String first = ChecksumUtil.crc32("hello");
        String second = ChecksumUtil.crc32("hello");

        assertEquals(first, second);
    }
    @Test
    void crc32_shouldReturnDifferentChecksumForDifferentInput() {

        String first = ChecksumUtil.crc32("hello");
        String second = ChecksumUtil.crc32("Hello");

        assertNotEquals(first, second);
    }

    @Test
    void forwardChecksum_isDeterministic() {

        HdfcPaymentRequest request = sampleRequest();
        String checksum1 =
                ChecksumUtil.computeForwardChecksum(request, "654321");

        String checksum2 =
                ChecksumUtil.computeForwardChecksum(request, "654321");
        assertEquals(checksum1, checksum2);
    }
    @Test
    void changingAmount_changesChecksum() {

        HdfcPaymentRequest request = sampleRequest();

        String original =
                ChecksumUtil.computeForwardChecksum(
                        request,
                        "654321"
                );

        request.setTxnAmount("999.99");

        String changed =
                ChecksumUtil.computeForwardChecksum(
                        request,
                        "654321"
                );
        assertNotEquals(original, changed);
    }

    @Test
    void changingChecksumKey_changesChecksum() {

        HdfcPaymentRequest request = sampleRequest();

        String checksum1 =
                ChecksumUtil.computeForwardChecksum(
                        request,
                        "654321"
                );

        String checksum2 =
                ChecksumUtil.computeForwardChecksum(
                        request,
                        "999999"
                );

        assertNotEquals(checksum1, checksum2);
    }
    @Test
    void returnChecksumChangesWhenBankRefNoChange() {
        String c1 = ChecksumUtil.computeReturnChecksum(
                "Client1", "MERCHANT", "INR", "1.79", "0.00", "REF123",
                "N", "N", "25/05/2018 12:00:00", "HDFC1000", "", "654321");
        String c2 = ChecksumUtil.computeReturnChecksum(
                "Client1", "MERCHANT", "INR", "1.79", "0.00", "REF123",
                "N", "N", "25/05/2018 12:00:00", "0", "Insufficient Funds", "654321");

        assertNotEquals(c1, c2);
    }
    @Test
    void fieldOrderMatters_swappingTwoFieldValuesChangesChecksum() {
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
}
