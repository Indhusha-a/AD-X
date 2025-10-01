package com.adx.ad_x.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_reviews")
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem; // Ties to purchased item

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(length = 1000)
    private String comment;

    @ElementCollection
    @CollectionTable(name = "product_review_media", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "media_url")
    private List<String> mediaUrls = new ArrayList<>();

    @Column(name = "helpfulness_votes", nullable = false)
    private Integer helpfulnessVotes = 0;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "productReview", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ReviewResponse response;

    public ProductReview() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ProductReview(User buyer, Product product, OrderItem orderItem, Integer rating, String comment) {
        this();
        this.buyer = buyer;
        this.product = product;
        this.orderItem = orderItem;
        this.rating = rating;
        this.comment = comment;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Transient
    private Double productAverageRating;

    public Double getProductAverageRating() { return productAverageRating; }
    public void setProductAverageRating(Double productAverageRating) { this.productAverageRating = productAverageRating; }

    // Getters/Setters (all fields)
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public User getBuyer() { return buyer; } public void setBuyer(User buyer) { this.buyer = buyer; }
    public Product getProduct() { return product; } public void setProduct(Product product) { this.product = product; }
    public OrderItem getOrderItem() { return orderItem; } public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }
    public Integer getRating() { return rating; } public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; } public void setComment(String comment) { this.comment = comment; }
    public List<String> getMediaUrls() { return mediaUrls; } public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
    public Integer getHelpfulnessVotes() { return helpfulnessVotes; } public void setHelpfulnessVotes(Integer helpfulnessVotes) { this.helpfulnessVotes = helpfulnessVotes; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public Boolean getIsActive() { return isActive; } public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public ReviewResponse getResponse() { return response; } public void setResponse(ReviewResponse response) { this.response = response; }

    @Override
    public String toString() {
        return "ProductReview{" + "id=" + id + ", rating=" + rating + ", status='" + status + '\'' + '}';
    }
}