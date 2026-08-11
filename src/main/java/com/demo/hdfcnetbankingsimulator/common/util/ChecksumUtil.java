package com.demo.hdfcnetbankingsimulator.common.util;

import com.demo.hdfcnetbankingsimulator.gateway.dto.HdfcPaymentRequest;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.CRC32;


public final class ChecksumUtil {

    private ChecksumUtil() {}

    public static String crc32(String data) {
        CRC32 crc = new CRC32();
        crc.update(data.getBytes(StandardCharsets.UTF_8));
        return String.valueOf(crc.getValue());
    }

    private static String nz(String s) { return s == null ? "" : s; }

    public static String forwardChecksumString(HdfcPaymentRequest r, String checksumKey) {
        return nz(r.getClientCode()) + nz(r.getMerchantCode()) + nz(r.getTxnCurrency()) + nz(r.getTxnAmount())
                + nz(r.getTxnScAmount()) + nz(r.getMerchantRefNo()) + nz(r.getSuccessStaticFlag()) + nz(r.getFailureStaticFlag())
                + nz(r.getDate()) + nz(r.getRef1()) + nz(r.getRef2()) + nz(r.getRef3()) + nz(r.getRef4()) + nz(r.getRef5())
                + nz(r.getRef6()) + nz(r.getRef7()) + nz(r.getRef8()) + nz(r.getRef9()) + nz(r.getRef10()) + nz(r.getRef11())
                + nz(r.getDate1()) + nz(r.getDate2()) + nz(r.getDisplayDetails())
                + nz(r.getDetails1()) + nz(r.getDetails2()) + nz(r.getDetails3()) + nz(r.getDynamicUrl())
                + checksumKey;
    }

    public static String computeForwardChecksum(HdfcPaymentRequest r, String checksumKey) {
        return crc32(forwardChecksumString(r, checksumKey));
    }

    private static String g(Map<String, String> p, String key) {
        String v = p.get(key);
        return v == null ? "" : v;
    }

    public static String forwardChecksumString(Map<String, String> p, String checksumKey) {
        return g(p, "ClientCode") + g(p, "MerchantCode") + g(p, "TxnCurrency") + g(p, "TxnAmount")
                + g(p, "TxnScAmount") + g(p, "MerchantRefNo") + g(p, "SuccessStaticFlag") + g(p, "FailureStaticFlag")
                + g(p, "Date") + g(p, "Ref1") + g(p, "Ref2") + g(p, "Ref3") + g(p, "Ref4") + g(p, "Ref5")
                + g(p, "Ref6") + g(p, "Ref7") + g(p, "Ref8") + g(p, "Ref9") + g(p, "Ref10") + g(p, "Ref11")
                + g(p, "Date1") + g(p, "Date2") + g(p, "DisplayDetails")
                + g(p, "Details1") + g(p, "Details2") + g(p, "Details3") + g(p, "DynamicUrl")
                + checksumKey;
    }

    public static String computeForwardChecksum(Map<String, String> p, String checksumKey) {
        return crc32(forwardChecksumString(p, checksumKey));
    }

    /** Simplified to skip Ref1-11/Date1/Date2 (this demo always leaves them empty anyway). */
    public static String computeReturnChecksum(String clientCode, String merchantCode, String txnCurrency,
                                                String txnAmount, String txnScAmount, String merchantRefNo,
                                                String successStaticFlag, String failureStaticFlag, String date,
                                                String bankRefNo, String message, String checksumKey) {
        String data = nz(clientCode) + nz(merchantCode) + nz(txnCurrency) + nz(txnAmount) + nz(txnScAmount)
                + nz(merchantRefNo) + nz(successStaticFlag) + nz(failureStaticFlag) + nz(date)
                + nz(bankRefNo) + nz(message) + checksumKey;
        return crc32(data);
    }

    /**
     * Used only by the bank simulator's CHECKSUM_TAMPER test scenario: deliberately
     * returns a value that will NOT match a correctly recomputed checksum, so we can
     * prove the Gateway's checksum-validation layer actually catches it.
     */
    public static String tamper(String checksum) {
        return "0" + checksum;
    }
}
