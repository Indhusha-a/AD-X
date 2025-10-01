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

    @Autowired
    private NotificationService notificationService;

    // Process a payment for an order
    @Transactional
    public Payment processPayment(Order order, User buyer, String paymentMethod, String paymentGateway) {
        Payment payment = new Payment(order, buyer, order.getTotalAmount(), paymentMethod);
        payment.setPaymentGateway(paymentGateway);
        payment.setTransactionId(generateTransactionId());

        // Save payment first to get an ID
        payment = paymentRepository.save(payment);

        // Simulate payment processing
        boolean paymentSuccess = simulatePaymentProcessing(payment);

        if (paymentSuccess) {
            payment.setStatus("COMPLETED");
            payment.setPaymentDate(LocalDateTime.now());

            // Update order status and link payment
            order.setStatus("CONFIRMED");
            order.setPayment(payment);
            orderService.updateOrder(order);

            // Record financial transaction
            recordFinancialTransaction("PAYMENT", payment.getAmount(), buyer,
                    "Payment for Order #" + order.getId(), payment);

            // Update seller revenue
            updateSellerRevenue(order, payment.getSellerEarnings());

            // Save the updated payment
            payment = paymentRepository.save(payment);

            // CREATE: Create notification for successful payment
            notificationService.createNotification(
                    buyer,
                    "Payment Received",
                    "Payment of $" + payment.getAmount() + " has been received successfully.",
                    "PAYMENT"
            );

            // CREATE: Notify seller about payment received
            for (OrderItem item : order.getItems()) {
                User seller = item.getProduct().getSeller();
                notificationService.createNotification(
                        seller,
                        "Payment Received",
                        "Payment of $" + payment.getSellerEarnings() + " has been received for order #" + order.getId(),
                        "PAYMENT"
                );
            }
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

            Payment savedPayment = paymentRepository.save(payment);

            // CREATE: Notify buyer about refund
            notificationService.createNotification(
                    payment.getBuyer(),
                    "Refund Processed",
                    "Refund of $" + refundAmount + " has been processed for your payment.",
                    "PAYMENT"
            );

            // CREATE: Notify seller about refund
            notificationService.createNotification(
                    payment.getOrder().getItems().get(0).getProduct().getSeller(),
                    "Refund Issued",
                    "Refund of $" + refundAmount + " has been issued for order #" + payment.getOrder().getId(),
                    "PAYMENT"
            );

            return savedPayment;
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
        List<Payment> payments = paymentRepository.findByDateRange(startDate, endDate);
        return payments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .map(Payment::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Calculate platform commission
    public BigDecimal calculatePlatformCommission(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments = paymentRepository.findByDateRange(startDate, endDate);
        return payments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .map(Payment::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Get payment statistics
    public PaymentStatistics getPaymentStatistics() {
        PaymentStatistics stats = new PaymentStatistics();

        // Get all payments
        List<Payment> allPayments = paymentRepository.findAll();

        // Calculate statistics
        stats.setTotalPayments((long) allPayments.size());
        stats.setCompletedPayments(allPayments.stream().filter(p -> "COMPLETED".equals(p.getStatus())).count());
        stats.setPendingPayments(allPayments.stream().filter(p -> "PENDING".equals(p.getStatus())).count());
        stats.setFailedPayments(allPayments.stream().filter(p -> "FAILED".equals(p.getStatus())).count());
        stats.setRefundedPayments(allPayments.stream().filter(p -> "REFUNDED".equals(p.getStatus())).count());

        // Calculate today's revenue
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        stats.setTodayRevenue(calculatePlatformRevenue(todayStart, todayEnd));

        return stats;
    }

    // Helper method to simulate payment processing
    private boolean simulatePaymentProcessing(Payment payment) {
        // For demo purposes, simulate a successful payment 95% of the time
        return Math.random() > 0.05; // 5% failure rate for demo
    }

    // Helper method to generate unique transaction ID
    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Helper method to record financial transactions
    private void recordFinancialTransaction(String type, BigDecimal amount, User user, String description, Payment payment) {
        FinancialTransaction transaction = new FinancialTransaction(type, amount, user, description);
        transaction.setPayment(payment);
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

    // Statistics class - ADDED BACK
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