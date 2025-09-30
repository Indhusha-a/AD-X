package com.adx.ad_x.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_analytics")
public class SellerAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "analytics_date")
    private LocalDate analyticsDate;

    @Column(name = "total_views")
    private Integer totalViews = 0;

    @Column(name = "product_views")
    private Integer productViews = 0;

    @Column(name = "inquiries_received")
    private Integer inquiriesReceived = 0;

    @Column(name = "orders_received")
    private Integer ordersReceived = 0;

    @Column(name = "revenue_generated", precision = 12, scale = 2)
    private BigDecimal revenueGenerated = BigDecimal.ZERO;

    @Column(name = "conversion_rate", precision = 5, scale = 2)
    private BigDecimal conversionRate = BigDecimal.ZERO;

    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public SellerAnalytics() {
        this.createdAt = LocalDateTime.now();
        this.analyticsDate = LocalDate.now();
    }

    public SellerAnalytics(User seller) {
        this();
        this.seller = seller;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }

    public LocalDate getAnalyticsDate() { return analyticsDate; }
    public void setAnalyticsDate(LocalDate analyticsDate) { this.analyticsDate = analyticsDate; }

    public Integer getTotalViews() { return totalViews; }
    public void setTotalViews(Integer totalViews) { this.totalViews = totalViews; }

    public Integer getProductViews() { return productViews; }
    public void setProductViews(Integer productViews) { this.productViews = productViews; }

    public Integer getInquiriesReceived() { return inquiriesReceived; }
    public void setInquiriesReceived(Integer inquiriesReceived) { this.inquiriesReceived = inquiriesReceived; }

    public Integer getOrdersReceived() { return ordersReceived; }
    public void setOrdersReceived(Integer ordersReceived) { this.ordersReceived = ordersReceived; }

    public BigDecimal getRevenueGenerated() { return revenueGenerated; }
    public void setRevenueGenerated(BigDecimal revenueGenerated) { this.revenueGenerated = revenueGenerated; }

    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal conversionRate) { this.conversionRate = conversionRate; }

    public Integer getResponseTimeMinutes() { return responseTimeMinutes; }
    public void setResponseTimeMinutes(Integer responseTimeMinutes) { this.responseTimeMinutes = responseTimeMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}