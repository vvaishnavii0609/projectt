# HDFC NetBanking Simulator — Complete End-to-End Build

Everything from the whiteboard, wired for real: Merchant creates the order -> Checkout
-> Gateway (validates, builds the real HDFC request + checksum) -> Bank (real control
panel, 15 test scenarios) -> Bank fires an S2S callback to Gateway *and* redirects the
browser straight back to Merchant -> Gateway never trusts the callback, always calls
Bank's Verify endpoint independently -> Merchant's own backend asks Gateway for the
final answer, never asks the Bank directly.

One Spring Boot app, one port (8080), three packages (`merchant` / `gateway` / `bank`).

---

## 1. The exact flow, as built

```
1.  GET  /merchant.html                  customer sees product + Pay Now
2.  GET  /checkout.html
       -> GET /merchant/order            Merchant creates + PERSISTS a MerchantOrder,
                                          returns {orderId, productName, amount, currency}
3.  Customer picks NetBanking + HDFC, clicks Pay
       -> POST /gateway/payment          browser -> Gateway directly (S2S from checkout's
                                          own backend is a documented simplification -
                                          see "Known simplifications" below)
4.  GatewayService.initiatePayment():
       - validates amount/method/bank
       - saves a PaymentTransaction (status REDIRECTED_TO_BANK)
       - builds the full HdfcPaymentRequest: Ref1 = orderId (Ch.2 Note 2 pass-through UDF),
         DynamicUrl = this app's own /gateway/callback
       - computes the forward checksum (Ch.14, CRC32)
       - returns the actual bank URL as plain text
5.  Browser navigates to GET /netbanking/merchant?ClientCode=...&Ref1=<orderId>&CheckSum=...
6.  EpiController -> BankService.validate(): mandatory fields -> identity -> amount ->
    currency -> checksum -> Ch.4 duplicate-success check. Reject or show login page.
7.  POST /netbanking/login (any credentials) -> shows the 15-scenario control panel
8.  POST /netbanking/authorize -> BankService.processScenario() decides the outcome,
    then BankCallbackService.sendCallback() fires the S2S callback (with whatever
    DELAY/DROP/DUPLICATE/TAMPER_* the chosen scenario implies)
9.  Bank redirects the browser DIRECTLY to Merchant:
       GET /receipt.html?orderId=<Ref1>&status=<hint>     (UX hint only, never trusted)
10. POST /gateway/callback (S2S, bank -> gateway)
       -> GatewayService.handleBankCallback(): idempotency check (already-terminal ->
          no-op), otherwise calls reconcileWithBank()
11. reconcileWithBank(): GET /netbanking/verify (S2S, gateway -> bank, Ch.8) ->
       (1) recompute + validate the response checksum
       (2) confirm identity (ClientCode/MerchantRefNo) matches
       (3) confirm the echoed TxnAmount matches what Gateway originally sent
       (4) only then translate flgSuccess (S/F/P) into SUCCESS/FAILURE/PENDING and save
12. receipt.html polls GET /merchant/transaction/{orderId}
       -> MerchantController -> GatewayClient -> GET /gateway/status/{orderId}
       -> if not yet terminal, ANOTHER on-demand Verify call happens here too -
          this is what recovers a DROPped callback
13. Merchant shows the real, Verify-confirmed result. Merchant never called Bank directly.
```

## 2. The 15 test scenarios (`bank/enums/SimulationScenario.java`)

| Category | Scenarios |
|---|---|
| Normal | `SUCCESS`, `PENDING` |
| Business failures | `FAILURE_INSUFFICIENT_FUNDS`, `FAILURE_AUTH`, `FAILURE_ACCOUNT_BLOCKED`, `FAILURE_INVALID_ACCOUNT`, `FAILURE_TXN_LIMIT_EXCEEDED` |
| Technical failures | `FAILURE_BANK_ERROR`, `FAILURE_BANK_MAINTENANCE` |
| Session | `SESSION_TIMEOUT` (nothing sent anywhere - no redirect, no callback) |
| Communication fault injection | `DELAY` (callback ~6s late), `DROP` (succeeds internally, callback never sent), `DUPLICATE` (callback fires twice) |
| Data-integrity attack simulation | `TAMPER_AMOUNT` (callback/verify report a different amount, self-consistent checksum), `TAMPER_CHECKSUM` (deliberately corrupted checksum) |

`TAMPER_AMOUNT` and `TAMPER_CHECKSUM` are the two that specifically exercise
`reconcileWithBank()`'s checksum/amount validation layers - pick one, watch the
Gateway's console log the rejection, and the transaction end up `FAILURE` (amount
tamper) or stuck retryable (checksum tamper) instead of a false `SUCCESS`.

## 3. How to run

```bash
mvn spring-boot:run
```
Needs your existing MySQL config (`application.properties`, unchanged). Open
**http://localhost:8080/merchant.html**.

## 4. How to run the tests

```bash
mvn test
```

Four new test classes, all plain Mockito unit tests (no Spring context, no MySQL
needed for any of them):

- `ChecksumUtilTest` - determinism, tamper-detection, and (importantly) that the
  Gateway's DTO-based checksum formula and the Bank's Map-based one agree on the same data
- `BankServiceTest` - full `validate()`/`accept()`/`processScenario()` (parameterized
  across all 7 failure reasons)/`verify()` coverage, including both tamper scenarios
- `BankCallbackServiceTest` - Mockito-verified call counts: SUCCESS fires once, DROP
  fires zero, DUPLICATE fires twice, DELAY fires once but only after the delay
- `GatewayServiceTest` - the actual trust boundary: checksum rejection, identity
  rejection, amount-mismatch rejection, idempotent duplicate-callback handling,
  on-demand-Verify recovery for a dropped callback

WARNING: One pre-existing test needs MySQL running: `HdfcNetBankingSimulatorApplicationTests`
(the original Spring Initializr context-load test) boots the full app context including
the real datasource. If you don't have MySQL up, either start it, or run just the new
unit tests:
```bash
mvn test -Dtest='!HdfcNetBankingSimulatorApplicationTests'
```
`src/test/resources/application-test.properties` sketches an H2-based alternative if
you'd rather decouple that test from MySQL entirely - it's inert until you add the H2
dependency and `@ActiveProfiles("test")`, left as a documented option rather than
something forced on you.

## 5. What changed in this pass (on top of everything built so far)

- **`Ref1` now actually carries `orderId`** end-to-end - it was being hardcoded to `""`
  in `GatewayService` and never added to the outgoing URL at all. Fixed in both places.
- **Bank redirects the browser directly to Merchant**, not through a PG bridge -
  `GatewayReturnController` (the old bridge) is removed, confirmed nothing still calls it.
- **Merchant's backend now genuinely talks to Gateway server-to-server** -
  `merchant/service/GatewayClient.java` (new) + `GET /merchant/transaction/{orderId}`
  (new). Previously the browser polled Gateway's `/gateway/status` directly, bypassing
  Merchant's backend entirely - `receipt.html` now calls Merchant instead.
- **`GET /gateway/status/{orderId}` returns a slim `TransactionStatusResponse` DTO**
  instead of the raw JPA entity - avoids leaking internal fields and avoids any
  `LocalDateTime`/Jackson risk on the new Merchant->Gateway hop.
- **Both `CallbackPayload` and `VerificationResponse` DTOs are now field-identical**
  between the `bank` and `gateway` packages (verified field-by-field) - this is exactly
  the kind of drift that would silently break real HTTP (de)serialization.
- **Removed dead code**: `MerchantService`/`MerchantPaymentRequest`/`MerchantPaymentResponse`
  (superseded by the rebuilt `MerchantController`), `BankPaymentDetails`/`HdfcBankResponse`/
  the empty `HdfcPaymentResponse` stub (leftovers from the old scenario-as-JSON-API design).
- **Found and fixed a real inconsistency**: `BankCallbackService` referenced methods
  (`getEffectiveAmount()`, `isTamperChecksum()`) that didn't exist anywhere, and
  `LoginController`'s scenario radio buttons used names (`FAILURE_ACCOUNT_LOCKED`,
  `AMOUNT_TAMPER`) that didn't match the actual enum constants. Both fixed and swept for
  any other stale references.
- **`GatewayService` switched from field injection to a dual constructor** (no-arg for
  Spring's field injection to keep working unmodified, a real constructor for tests) -
  this is what let `GatewayServiceTest` mock `PaymentTransactionRepository`/`BankClient`
  cleanly without spinning up Spring.

## 6. Known simplifications (worth being upfront about)

- **Checkout's `POST /gateway/payment` call is browser -> Gateway directly**, not
  Merchant-backend -> Gateway (S2S) as the original whiteboard arrow implies. This
  matches what was already there; genuinely making Merchant's backend the one to call
  Gateway (instead of the browser) is a contained follow-up if you want the diagram's
  arrow direction literally exact.
- **Bank's return-checksum formula omits `Ref1..Ref11`, `Date1`, `Date2`** (documented
  in `ChecksumUtil`'s own comment) - fine since this demo always leaves them empty, but
  would need extending if you start populating those fields for real.
- **The document's `/netbanking/epi` verify path vs. our `/netbanking/verify`** - still
  using our own path name rather than the doc's literal one, as flagged earlier.
