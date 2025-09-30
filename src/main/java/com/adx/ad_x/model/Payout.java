package com.adx.ad_x.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payouts")
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED

    @Column(name = "payout_method", length = 50)
    private String payoutMethod; // BANK_TRANSFER, PAYPAL, CHECK, etc.

    @Column(name = "payout_reference", unique = true, length = 100)
    private String payoutReference;

    @Column(name = "transaction_fee", precision = 10, scale = 2)
    private BigDecimal transactionFee = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "payout_period_start")
    private LocalDate payoutPeriodStart;

    @Column(name = "payout_period_end")
    private LocalDate payoutPeriodEnd;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(name = "estimated_arrival_date")
    private LocalDate estimatedArrivalDate;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "bank_account_last4", length = 4)
    private String bankAccountLast4;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Payout() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Payout(User seller, BigDecimal amount, String payoutMethod) {
        this();
        this.seller = seller;
        this.amount = amount;
        this.payoutMethod = payoutMethod;
        calculateNetAmount();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Calculate net amount after fees
    public void calculateNetAmount() {
        if (this.amount != null && this.transactionFee != null) {
            this.netAmount = this.amount.subtract(this.transactionFee);
        } else if (this.amount != null) {
            this.netAmount = this.amount;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        calculateNetAmount();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPayoutMethod() { return payoutMethod; }
    public void setPayoutMethod(String payoutMethod) { this.payoutMethod = payoutMethod; }

    public String getPayoutReference() { return payoutReference; }
    public void setPayoutReference(String payoutReference) { this.payoutReference = payoutReference; }

    public BigDecimal getTransactionFee() { return transactionFee; }
    public void setTransactionFee(BigDecimal transactionFee) {
        this.transactionFee = transactionFee;
        calculateNetAmount();
    }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public LocalDate getPayoutPeriodStart() { return payoutPeriodStart; }
    public void setPayoutPeriodStart(LocalDate payoutPeriodStart) { this.payoutPeriodStart = payoutPeriodStart; }

    public LocalDate getPayoutPeriodEnd() { return payoutPeriodEnd; }
    public void setPayoutPeriodEnd(LocalDate payoutPeriodEnd) { this.payoutPeriodEnd = payoutPeriodEnd; }

    public LocalDateTime getProcessedDate() { return processedDate; }
    public void setProcessedDate(LocalDateTime processedDate) { this.processedDate = processedDate; }

    public LocalDate getEstimatedArrivalDate() { return estimatedArrivalDate; }
    public void setEstimatedArrivalDate(LocalDate estimatedArrivalDate) { this.estimatedArrivalDate = estimatedArrivalDate; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getBankAccountLast4() { return bankAccountLast4; }
    public void setBankAccountLast4(String bankAccountLast4) { this.bankAccountLast4 = bankAccountLast4; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Payout{" +
                "id=" + id +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", payoutMethod='" + payoutMethod + '\'' +
                '}';
    }
}