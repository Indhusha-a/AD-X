package com.adx.ad_x.service;

import com.adx.ad_x.model.*;
import com.adx.ad_x.repository.PaymentRepository;
import com.adx.ad_x.repository.FinancialTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FinancialTransactionRepository financialTransactionRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SellerProfileService sellerProfileService;

    // Process a payment for an order
    @Transactional
    public Payment processPayment(Order order, User buyer, String paymentMethod, String paymentGateway) {
        Payment payment = new Payment(order, buyer, order.getTotalAmount(), paymentMethod);
        payment.setPaymentGateway(paymentGateway);
        payment.setTransactionId(generateTransactionId());

        // Save payment first to get an ID
        payment = paymentRepository.save(payment);

        // Simulate payment processing (in real app, integrate with payment gateway)
        boolean paymentSuccess = simulatePaymentProcessing(payment);

        if (paymentSuccess) {
            payment.setStatus("COMPLETED");
            payment.setPaymentDate(LocalDateTime.now());

            // Update order status and link payment
            order.setStatus("CONFIRMED");
            order.setPayment(payment);
            orderService.updateOrder(order);

            // Record financial transaction - FIXED: payment is now saved
            recordFinancialTransaction("PAYMENT", payment.getAmount(), buyer,
                    "Payment for Order #" + order.getId(), payment);

            // Update seller revenue
            updateSellerRevenue(order, payment.getSellerEarnings());

            // Save the updated payment
            payment = paymentRepository.save(payment);
        } else {
            payment.setStatus("FAILED");
            payment.setGatewayResponse("Payment processing failed");
            payment = paymentRepository.save(payment);
        }

        return payment;
    }

    // Process refund for a payment
    @Transactional
    public Payment processRefund(Long paymentId, BigDecimal refundAmount, String reason) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();

            if (!"COMPLETED".equals(payment.getStatus())) {
                throw new IllegalStateException("Cannot refund a payment that is not completed");
            }

            if (refundAmount.compareTo(payment.getAmount()) > 0) {
                throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
            }

            BigDecimal remainingRefundable = payment.getAmount().subtract(payment.getRefundedAmount());
            if (refundAmount.compareTo(remainingRefundable) > 0) {
                throw new IllegalArgumentException("Refund amount exceeds remaining refundable amount");
            }

            // Update payment status and refund amount
            payment.setRefundedAmount(payment.getRefundedAmount().add(refundAmount));

            if (refundAmount.compareTo(payment.getAmount()) == 0) {
                payment.setStatus("REFUNDED");
            } else {
                payment.setStatus("PARTIALLY_REFUNDED");
            }

            payment.setRefundDate(LocalDateTime.now());

            // Record financial transaction for refund
            recordFinancialTransaction("REFUND", refundAmount.negate(), payment.getBuyer(),
                    "Refund for Payment #" + payment.getId() + ": " + reason, payment);

            // Update seller revenue (deduct refund)
            updateSellerRevenueOnRefund(payment.getOrder(), refundAmount);

            return paymentRepository.save(payment);
        }
        return null;
    }

    // Get payment by ID
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    // Get payments by buyer
    public List<Payment> getPaymentsByBuyer(User buyer) {
        return paymentRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    // Get payments by status
    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    // Get payments by date range
    public List<Payment> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.findByDateRange(startDate, endDate);
    }

    // Calculate platform revenue
    public BigDecimal calculatePlatformRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.calculateTotalRevenue(startDate, endDate);
    }

    // Calculate platform commission
    public BigDecimal calculatePlatformCommission(LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.calculateTotalCommission(startDate, endDate);
    }

    // Get payment statistics
    public PaymentStatistics getPaymentStatistics() {
        PaymentStatistics stats = new PaymentStatistics();
        stats.setTotalPayments(paymentRepository.count());
        stats.setCompletedPayments(paymentRepository.countByStatus("COMPLETED"));
        stats.setPendingPayments(paymentRepository.countByStatus("PENDING"));
        stats.setFailedPayments(paymentRepository.countByStatus("FAILED"));
        stats.setRefundedPayments(paymentRepository.countByStatus("REFUNDED"));

        // Calculate today's revenue
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        stats.setTodayRevenue(paymentRepository.calculateTotalRevenue(todayStart, todayEnd));

        return stats;
    }

    // Helper method to simulate payment processing
    private boolean simulatePaymentProcessing(Payment payment) {
        // In a real application, this would integrate with a payment gateway like Stripe, PayPal, etc.
        // For demo purposes, we'll simulate a successful payment 95% of the time
        return Math.random() > 0.05; // 5% failure rate for demo
    }

    // Helper method to generate unique transaction ID
    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Helper method to record financial transactions
    private void recordFinancialTransaction(String type, BigDecimal amount, User user, String description, Payment payment) {
        FinancialTransaction transaction = new FinancialTransaction(type, amount, user, description);
        transaction.setPayment(payment); // Payment is now saved and has an ID
        financialTransactionRepository.save(transaction);
    }

    // Helper method to update seller revenue
    private void updateSellerRevenue(Order order, BigDecimal earnings) {
        // For each product in the order, update the respective seller's revenue
        for (OrderItem item : order.getItems()) {
            User seller = item.getProduct().getSeller();
            sellerProfileService.updateRevenueAndOrders(seller, earnings.doubleValue(), true);
        }
    }

    // Helper method to update seller revenue on refund
    private void updateSellerRevenueOnRefund(Order order, BigDecimal refundAmount) {
        // For each product in the order, deduct the refund from seller's revenue
        for (OrderItem item : order.getItems()) {
            User seller = item.getProduct().getSeller();
            sellerProfileService.updateRevenueAndOrders(seller, -refundAmount.doubleValue(), false);
        }
    }

    // Statistics class
    public static class PaymentStatistics {
        private Long totalPayments;
        private Long completedPayments;
        private Long pendingPayments;
        private Long failedPayments;
        private Long refundedPayments;
        private BigDecimal todayRevenue;

        // Getters and setters
        public Long getTotalPayments() { return totalPayments; }
        public void setTotalPayments(Long totalPayments) { this.totalPayments = totalPayments; }

        public Long getCompletedPayments() { return completedPayments; }
        public void setCompletedPayments(Long completedPayments) { this.completedPayments = completedPayments; }

        public Long getPendingPayments() { return pendingPayments; }
        public void setPendingPayments(Long pendingPayments) { this.pendingPayments = pendingPayments; }

        public Long getFailedPayments() { return failedPayments; }
        public void setFailedPayments(Long failedPayments) { this.failedPayments = failedPayments; }

        public Long getRefundedPayments() { return refundedPayments; }
        public void setRefundedPayments(Long refundedPayments) { this.refundedPayments = refundedPayments; }

        public BigDecimal getTodayRevenue() { return todayRevenue; }
        public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }
    }
}