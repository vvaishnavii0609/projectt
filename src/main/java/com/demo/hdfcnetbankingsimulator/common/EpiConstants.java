package com.demo.hdfcnetbankingsimulator.common;

public final class EpiConstants {

    private EpiConstants() {}

    public static final String CLIENT_CODE = "Client1";
    public static final String MERCHANT_CODE = "MERCHANT";

    public static final String CHECKSUM_KEY = "654321";

    // Where the merchant app's pages live. The bank redirects the browser here directly
    // once a scenario is processed (using Ref1=orderId to know which order), per the
    // whiteboard flow: Bank -> Merchant is a direct hop, not routed through PG.
    public static final String MERCHANT_BASE_URL = "http://localhost:9292";

    // Same app/port right now, but named separately since these are conceptually
    // different systems - the bank's callback target, specifically.
    public static final String GATEWAY_BASE_URL = "http://localhost:9292";

    // Where the bank's own pages/endpoints live - used when the bank redirects its
    // own browser session to its own control-panel page.
    public static final String BANK_BASE_URL = "http://localhost:9292";
}
