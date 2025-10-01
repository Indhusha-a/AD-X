package com.adx.ad_x.service;

import com.adx.ad_x.model.*;
import com.adx.ad_x.repository.PayoutRepository;
import com.adx.ad_x.repository.PaymentRepository;
import com.adx.ad_x.repository.FinancialTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PayoutService {

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FinancialTransactionRepository financialTransactionRepository;

    @Autowired
    private SellerProfileService sellerProfileService;

    // Create a payout for a seller
    public Payout createPayout(User seller, BigDecimal amount, String payoutMethod) {
        // Verify seller has sufficient pending earnings
        BigDecimal pendingEarnings = calculatePendingEarnings(seller);
        if (amount.compareTo(pendingEarnings) > 0) {
            throw new IllegalArgumentException("Requested payout amount exceeds pending earnings");
        }

        Payout payout = new Payout(seller, amount, payoutMethod);
        payout.setPayoutReference(generatePayoutReference());
        payout.setPayoutPeriodStart(LocalDate.now().minusDays(7)); // Last 7 days
        payout.setPayoutPeriodEnd(LocalDate.now());
        payout.setEstimatedArrivalDate(LocalDate.now().plusDays(3)); // Estimated 3 business days

        // Calculate transaction fee (2% or $1 minimum)
        BigDecimal fee = amount.multiply(new BigDecimal("0.02"));
        if (fee.compareTo(new BigDecimal("1.00")) < 0) {
            fee = new BigDecimal("1.00");
        }
        payout.setTransactionFee(fee);
        payout.calculateNetAmount();

        return payoutRepository.save(payout);
    }

    // Process a payout (mark as processing/completed)
    public Payout processPayout(Long payoutId) {
        Optional<Payout> payoutOpt = payoutRepository.findById(payoutId);
        if (payoutOpt.isPresent()) {
            Payout payout = payoutOpt.get();

            if ("PENDING".equals(payout.getStatus())) {
                payout.setStatus("PROCESSING");
                payout.setProcessedDate(LocalDateTime.now());

                // Simulate payout processing (in real app, integrate with bank/PayPal)
                boolean payoutSuccess = simulatePayoutProcessing(payout);

                if (payoutSuccess) {
                    payout.setStatus("COMPLETED");

                    // Record financial transaction
                    recordFinancialTransaction("PAYOUT", payout.getNetAmount().negate(),
                            payout.getSeller(), "Payout to seller", payout);

                    // Record commission fee transaction
                    recordFinancialTransaction("FEE", payout.getTransactionFee(),
                            payout.getSeller(), "Transaction fee for payout", payout);
                } else {
                    payout.setStatus("FAILED");
                    payout.setFailureReason("Payout processing failed");
                }

                return payoutRepository.save(payout);
            }
        }
        return null;
    }

    // Calculate pending earnings for a seller
    public BigDecimal calculatePendingEarnings(User seller) {
        // Use the repository method to calculate pending earnings
        return payoutRepository.calculatePendingEarnings(seller);
    }

    // Get payouts by seller
    public List<Payout> getPayoutsBySeller(User seller) {
        return payoutRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    // Get payouts by status
    public List<Payout> getPayoutsByStatus(String status) {
        return payoutRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    // Get all payouts
    public List<Payout> getAllPayouts() {
        return payoutRepository.findAll();
    }

    // Get payout by ID
    public Optional<Payout> getPayoutById(Long id) {
        return payoutRepository.findById(id);
    }

    // Calculate total payouts for platform
    public BigDecimal calculateTotalPayouts(LocalDate startDate, LocalDate endDate) {
        List<Payout> payouts = payoutRepository.findByDateRange(startDate, endDate);
        return payouts.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .map(Payout::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Generate weekly payouts for all eligible sellers
    public void generateWeeklyPayouts() {
        List<User> sellers = sellerProfileService.getAllSellers();

        for (User seller : sellers) {
            BigDecimal pendingEarnings = calculatePendingEarnings(seller);

            // Only generate payout if earnings exceed minimum ($10)
            if (pendingEarnings.compareTo(new BigDecimal("10.00")) >= 0) {
                createPayout(seller, pendingEarnings, "BANK_TRANSFER");
            }
        }
    }

    // Helper method to simulate payout processing
    private boolean simulatePayoutProcessing(Payout payout) {
        // In a real application, this would integrate with a payout service
        // For demo purposes, we'll simulate a successful payout 98% of the time
        return Math.random() > 0.02; // 2% failure rate for demo
    }

    // Helper method to generate unique payout reference
    private String generatePayoutReference() {
        return "PO_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Helper method to record financial transactions
    private void recordFinancialTransaction(String type, BigDecimal amount, User user, String description, Payout payout) {
        FinancialTransaction transaction = new FinancialTransaction(type, amount, user, description);
        transaction.setPayout(payout);
        financialTransactionRepository.save(transaction);
    }
}