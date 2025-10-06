package com.adx.ad_x.service;

import com.adx.ad_x.model.Payout;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.PayoutRepository;
import com.adx.ad_x.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PayoutService {

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Calculate pending earnings for a seller - FIXED: Handle Object[] to Payment conversion
    public BigDecimal calculatePendingEarnings(User seller) {
        try {
            // Get all completed payments for this seller's products
            List<Object[]> sellerPaymentData = paymentRepository.findCompletedPaymentsBySeller(seller);

            BigDecimal totalEarnings = BigDecimal.ZERO;
            for (Object[] paymentData : sellerPaymentData) {
                if (paymentData[0] != null) {
                    BigDecimal paymentAmount = (BigDecimal) paymentData[0];
                    // Calculate seller's share (90% of payment amount, 10% platform commission)
                    BigDecimal sellerShare = paymentAmount.multiply(new BigDecimal("0.90"));
                    totalEarnings = totalEarnings.add(sellerShare);
                }
            }

            // Subtract already paid out amounts
            List<Payout> completedPayouts = payoutRepository.findBySellerAndStatus(seller, "PROCESSED");
            for (Payout payout : completedPayouts) {
                if (payout.getAmount() != null) {
                    totalEarnings = totalEarnings.subtract(payout.getAmount());
                }
            }

            return totalEarnings.compareTo(BigDecimal.ZERO) > 0 ? totalEarnings : BigDecimal.ZERO;

        } catch (Exception e) {
            // Fallback calculation
            return BigDecimal.ZERO;
        }
    }

    // Get payouts by seller
    public List<Payout> getPayoutsBySeller(User seller) {
        return payoutRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    // Create payout
    public Payout createPayout(User seller, BigDecimal amount, String method) {
        Payout payout = new Payout();
        payout.setSeller(seller);
        payout.setAmount(amount);
        payout.setPayoutMethod(method);
        payout.setStatus("PENDING");

        return payoutRepository.save(payout);
    }

    // Get total pending payouts
    public BigDecimal getTotalPendingPayouts() {
        try {
            BigDecimal pending = payoutRepository.calculateTotalPendingPayouts();
            return pending != null ? pending : BigDecimal.ZERO;
        } catch (Exception e) {
            List<Payout> pendingPayouts = payoutRepository.findByStatus("PENDING");
            return pendingPayouts.stream()
                    .map(Payout::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    // Get all payouts
    public List<Payout> getAllPayouts() {
        return payoutRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get payouts by status
    public List<Payout> getPayoutsByStatus(String status) {
        return payoutRepository.findByStatus(status);
    }

    // Process payout
    public boolean processPayout(Long payoutId) {
        return payoutRepository.findById(payoutId)
                .map(payout -> {
                    payout.setStatus("PROCESSED");
                    payout.setProcessedAt(LocalDateTime.now());
                    payoutRepository.save(payout);
                    return true;
                })
                .orElse(false);
    }

    // Request payout for seller
    public Payout requestPayout(User seller, BigDecimal amount, String method) {
        // Check if seller has sufficient pending earnings
        BigDecimal pendingEarnings = calculatePendingEarnings(seller);
        if (amount.compareTo(pendingEarnings) > 0) {
            throw new IllegalArgumentException("Requested amount exceeds available earnings");
        }

        return createPayout(seller, amount, method);
    }

    // Get payout by ID
    public Optional<Payout> getPayoutById(Long payoutId) {
        return payoutRepository.findById(payoutId);
    }
}