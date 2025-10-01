package com.adx.ad_x.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_responses")
public class ReviewResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_review_id")
    private ProductReview productReview;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_review_id")
    private SellerReview sellerReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(length = 1000, nullable = false)
    private String responseText;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ReviewResponse() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ReviewResponse(User seller, String responseText, ProductReview productReview) {
        this();
        this.seller = seller;
        this.responseText = responseText;
        this.productReview = productReview;
    }

    public ReviewResponse(User seller, String responseText, SellerReview sellerReview) {
        this();
        this.seller = seller;
        this.responseText = responseText;
        this.sellerReview = sellerReview;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters/Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public ProductReview getProductReview() { return productReview; } public void setProductReview(ProductReview productReview) { this.productReview = productReview; }
    public SellerReview getSellerReview() { return sellerReview; } public void setSellerReview(SellerReview sellerReview) { this.sellerReview = sellerReview; }
    public User getSeller() { return seller; } public void setSeller(User seller) { this.seller = seller; }
    public String getResponseText() { return responseText; } public void setResponseText(String responseText) { this.responseText = responseText; }
    public Boolean getIsActive() { return isActive; } public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "ReviewResponse{" + "id=" + id + ", responseText='" + responseText + '\'' + '}';
    }
}