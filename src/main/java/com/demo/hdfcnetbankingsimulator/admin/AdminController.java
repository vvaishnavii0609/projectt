package com.demo.hdfcnetbankingsimulator.admin;

import com.demo.hdfcnetbankingsimulator.bank.repository.BankTransactionRepository;
import com.demo.hdfcnetbankingsimulator.gateway.repository.PaymentTransactionRepository;
import com.demo.hdfcnetbankingsimulator.merchant.repository.MerchantOrderRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Testing convenience only - clears all three tables so every manual test run starts
 * from a clean slate, instead of accidentally re-authorizing an old (already-terminal)
 * transaction and hitting the idempotency guard. Not something a real payment system
 * would ever expose.
 */
@RestController
public class AdminController {

    private final MerchantOrderRepository merchantOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public AdminController(MerchantOrderRepository merchantOrderRepository,
                            PaymentTransactionRepository paymentTransactionRepository,
                            BankTransactionRepository bankTransactionRepository) {
        this.merchantOrderRepository = merchantOrderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    @DeleteMapping("/admin/reset")
    public String resetAll() {
        long orders = merchantOrderRepository.count();
        long payments = paymentTransactionRepository.count();
        long bankTxns = bankTransactionRepository.count();

        merchantOrderRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
        bankTransactionRepository.deleteAll();

        String result = "Cleared " + orders + " merchant_order, " + payments
                + " payment_transaction, " + bankTxns + " bank_transaction rows";
        System.out.println("[ADMIN] " + result);
        return result;
    }
}
