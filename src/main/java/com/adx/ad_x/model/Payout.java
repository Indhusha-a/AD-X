package com.adx.ad_x.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payouts")
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payout_method", nullable = false, length = 50)
    private String payoutMethod;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, PROCESSED, FAILED

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ADD THESE MISSING FIELDS:
    @Column(name = "payout_reference", length = 50)
    private String payoutReference;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "transaction_fee", precision = 10, scale = 2)
    private BigDecimal transactionFee;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // Constructors
    public Payout() {
        this.createdAt = LocalDateTime.now();
        // Generate payout reference
        this.payoutReference = "PO_" + System.currentTimeMillis();
        // Set default values
        this.transactionFee = BigDecimal.ZERO;
        this.netAmount = BigDecimal.ZERO;
    }

    public Payout(User seller, BigDecimal amount, String payoutMethod) {
        this();
        this.seller = seller;
        this.amount = amount;
        this.payoutMethod = payoutMethod;
        // Calculate net amount (amount - 10% platform fee)
        this.transactionFee = amount.multiply(new BigDecimal("0.10"));
        this.netAmount = amount.subtract(this.transactionFee);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        // Recalculate net amount and fee when amount changes
        if (amount != null) {
            this.transactionFee = amount.multiply(new BigDecimal("0.10"));
            this.netAmount = amount.subtract(this.transactionFee);
        }
    }

    public String getPayoutMethod() { return payoutMethod; }
    public void setPayoutMethod(String payoutMethod) { this.payoutMethod = payoutMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // New getters and setters
    public String getPayoutReference() { return payoutReference; }
    public void setPayoutReference(String payoutReference) { this.payoutReference = payoutReference; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public BigDecimal getTransactionFee() { return transactionFee; }
    public void setTransactionFee(BigDecimal transactionFee) { this.transactionFee = transactionFee; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}