package com.adx.ad_x.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type; // PAYMENT, REFUND, PAYOUT, COMMISSION, FEE, ADJUSTMENT

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payout_id")
    private Payout payout;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(nullable = false, length = 20)
    private String status = "COMPLETED"; // PENDING, COMPLETED, FAILED, CANCELLED

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public FinancialTransaction() {
        this.createdAt = LocalDateTime.now();
        this.transactionDate = LocalDateTime.now();
    }

    public FinancialTransaction(String type, BigDecimal amount, User user, String description) {
        this();
        this.type = type;
        this.amount = amount;
        this.user = user;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public Payout getPayout() { return payout; }
    public void setPayout(Payout payout) { this.payout = payout; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "FinancialTransaction{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", user=" + (user != null ? user.getEmail() : "null") +
                '}';
    }
}