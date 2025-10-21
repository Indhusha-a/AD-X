package com.adx.ad_x.service;

import com.adx.ad_x.model.Payment;
import com.adx.ad_x.model.User;
import com.adx.ad_x.model.Order;
import com.adx.ad_x.repository.PaymentRepository;
import com.adx.ad_x.designpatterns.strategy.PaymentStrategyContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentStrategyContext paymentStrategyContext; // DESIGN PATTERN: Strategy Pattern

    // Process payment - USES STRATEGY PATTERN
    public Payment processPayment(Order order, String paymentMethod, String transactionId) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setBuyer(order.getBuyer());
        payment.setAmount(order.getTotalAmount());
        payment.setTransactionId(transactionId);

        // DESIGN PATTERN: Strategy Pattern processes payment based on method
        boolean success = paymentStrategyContext.executePayment(payment, paymentMethod);
        
        if (!success) {
            payment.setStatus("FAILED");
        }
        // Note: Strategy already sets status to COMPLETED and paymentMethod

        return paymentRepository.save(payment);
    }

    // Get payment by ID
    public Optional<Payment> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    // Get payments by buyer
    public List<Payment> getPaymentsByBuyer(User buyer) {
        return paymentRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    // Get all payments
    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get total revenue
    public BigDecimal getTotalRevenue() {
        try {
            BigDecimal revenue = paymentRepository.calculateTotalRevenue();
            return revenue != null ? revenue : BigDecimal.ZERO;
        } catch (Exception e) {
            // Fallback calculation
            List<Payment> completedPayments = paymentRepository.findByStatus("COMPLETED");
            return completedPayments.stream()
                    .map(Payment::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    // Get total transaction count
    public Long getTotalTransactionCount() {
        try {
            return paymentRepository.count();
        } catch (Exception e) {
            return 0L;
        }
    }

    // Calculate total revenue for date range
    public BigDecimal calculateTotalRevenue(LocalDateTime start, LocalDateTime end) {
        try {
            BigDecimal revenue = paymentRepository.calculateTotalRevenueByDateRange(start, end);
            return revenue != null ? revenue : BigDecimal.ZERO;
        } catch (Exception e) {
            // Fallback calculation
            List<Payment> payments = paymentRepository.findByCreatedAtBetweenAndStatus(start, end, "COMPLETED");
            return payments.stream()
                    .map(Payment::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    // Process refund
    public boolean processRefund(Long paymentId, BigDecimal amount, String reason) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus("REFUNDED");
            payment.setRefundAmount(amount);
            payment.setRefundReason(reason);
            payment.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            return true;
        }
        return false;
    }

    // Get payments by date range
    public List<Payment> getPaymentsByDateRange(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    // Get payments by status
    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }

    // Create payment record
    public Payment createPayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    // Update payment status
    public Payment updatePaymentStatus(Long paymentId, String status) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(status);
            return paymentRepository.save(payment);
        }
        return null;
    }

    // Get payments for admin
    public List<Payment> getPaymentsByStatusForAdmin(String status) {
        return paymentRepository.findByStatus(status);
    }
}