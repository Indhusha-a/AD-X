package com.adx.ad_x.service;

import com.adx.ad_x.model.*;
import com.adx.ad_x.repository.ProductReviewRepository;
import com.adx.ad_x.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductReviewService {

    @Autowired
    private ProductReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationService notificationService;

    // Create a new review - REMOVED purchase validation
    public ProductReview createReview(Product product, User buyer, Integer rating, String comment) {
        // REMOVED: Purchase validation - allow anyone to review

        // Check if review already exists - THIS IS CRITICAL
        Optional<ProductReview> existingReview = reviewRepository.findByBuyerAndProduct(buyer, product);
        if (existingReview.isPresent()) {
            throw new IllegalArgumentException("You have already reviewed this product. You can only submit one review per product.");
        }

        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
        }

        // Validate comment length
        if (comment != null && comment.length() > 500) {
            throw new IllegalArgumentException("Comment cannot exceed 500 characters");
        }

        ProductReview review = new ProductReview(product, buyer, product.getSeller(), rating, comment);
        ProductReview savedReview = reviewRepository.save(review);

        // Create notification for seller
        String notificationTitle = "New Product Review";
        String notificationMessage = buyer.getFirstName() + " left a " + rating + "-star review for your product: " + product.getTitle();
        notificationService.createNotification(
                product.getSeller(),
                notificationTitle,
                notificationMessage,
                "REVIEW",
                savedReview.getId(),
                "REVIEW"
        );

        return savedReview;
    }

    // Update a review
    public ProductReview updateReview(Long reviewId, User buyer, Integer rating, String comment) {
        Optional<ProductReview> reviewOpt = reviewRepository.findById(reviewId);

        if (reviewOpt.isPresent()) {
            ProductReview review = reviewOpt.get();

            // Verify ownership
            if (!review.getBuyer().getId().equals(buyer.getId())) {
                throw new IllegalArgumentException("You can only edit your own reviews");
            }

            // Check if within 24-hour edit window
            if (!review.canBeEdited()) {
                throw new IllegalArgumentException("You can only edit reviews within 24 hours of posting");
            }

            // Validate rating
            if (rating != null && (rating < 1 || rating > 5)) {
                throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
            }

            // Validate comment length
            if (comment != null && comment.length() > 500) {
                throw new IllegalArgumentException("Comment cannot exceed 500 characters");
            }

            if (rating != null) {
                review.setRating(rating);
            }
            if (comment != null) {
                review.setComment(comment);
            }

            return reviewRepository.save(review);
        }

        throw new IllegalArgumentException("Review not found");
    }

    // Delete a review (soft delete)
    public boolean deleteReview(Long reviewId, User user) {
        Optional<ProductReview> reviewOpt = reviewRepository.findById(reviewId);

        if (reviewOpt.isPresent()) {
            ProductReview review = reviewOpt.get();

            // Verify ownership or admin role
            if (!review.getBuyer().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
                throw new IllegalArgumentException("You can only delete your own reviews");
            }

            review.setActive(false);
            reviewRepository.save(review);
            return true;
        }

        return false;
    }

    // Get reviews for a product
    public List<ProductReview> getProductReviews(Product product) {
        return reviewRepository.findByProductAndActiveTrueOrderByCreatedAtDesc(product);
    }

    // Get reviews by seller
    public List<ProductReview> getSellerReviews(User seller) {
        return reviewRepository.findBySellerAndActiveTrueOrderByCreatedAtDesc(seller);
    }

    // Get reviews by buyer
    public List<ProductReview> getBuyerReviews(User buyer) {
        return reviewRepository.findByBuyerAndActiveTrueOrderByCreatedAtDesc(buyer);
    }

    // Get average rating for a product
    public Double getProductAverageRating(Product product) {
        return reviewRepository.calculateAverageRatingByProduct(product);
    }

    // Get review count for a product
    public Long getProductReviewCount(Product product) {
        return reviewRepository.countByProductAndActiveTrue(product);
    }

    // Get review count for a seller
    public Long getSellerReviewCount(User seller) {
        return reviewRepository.countBySellerAndActiveTrue(seller);
    }

    // Check if buyer can review a product - UPDATED: Always return true
    public boolean canBuyerReviewProduct(User buyer, Product product) {
        // REMOVED: Purchase validation - allow anyone to review
        boolean hasReviewed = reviewRepository.findByBuyerAndProduct(buyer, product).isPresent();
        return !hasReviewed; // Only prevent if already reviewed
    }

    // Get recent reviews
    public List<ProductReview> getRecentReviews() {
        return reviewRepository.findTop5ByActiveTrueOrderByCreatedAtDesc();
    }

    // Get specific review
    public Optional<ProductReview> getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId);
    }
}