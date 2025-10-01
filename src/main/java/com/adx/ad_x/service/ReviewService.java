package com.adx.ad_x.service;

import com.adx.ad_x.model.*;
import com.adx.ad_x.repository.ProductReviewRepository;
import com.adx.ad_x.repository.SellerReviewRepository;
import com.adx.ad_x.repository.ReviewResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ProductReviewRepository productReviewRepository;

    @Autowired
    private SellerReviewRepository sellerReviewRepository;

    @Autowired
    private ReviewResponseRepository reviewResponseRepository;

    @SuppressWarnings("unused") // Used in eligibility
    @Autowired
    private OrderService orderService;

    @Autowired
    private SellerProfileService sellerProfileService;

    // Eligibility checks...
    private boolean isEligibleForReview(User buyer, Product product, OrderItem orderItem) {
        if (orderItem == null) return false;
        Order order = orderItem.getOrder();
        if (order == null || !buyer.getId().equals(order.getBuyer().getId())) return false;
        Payment payment = order.getPayment();
        return payment != null && "COMPLETED".equals(payment.getStatus());
    }

    private boolean isEligibleForSellerReview(User buyer, User seller, Order order) {
        if (order == null || !buyer.getId().equals(order.getBuyer().getId())) return false;
        Payment payment = order.getPayment();
        if (payment == null || !"COMPLETED".equals(payment.getStatus())) return false;
        return order.getItems().stream().anyMatch(item -> item.getProduct().getSeller().getId().equals(seller.getId()));
    }

    @Transactional
    public ProductReview createProductReview(ProductReview review) {
        if (!isEligibleForReview(review.getBuyer(), review.getProduct(), review.getOrderItem())) {
            throw new IllegalArgumentException("Not eligible to review this product");
        }
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be 1-5");
        }
        List<ProductReview> existing = productReviewRepository.findByBuyerAndProductAndIsActiveTrue(review.getBuyer(), review.getProduct());
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("You have already reviewed this product");
        }
        review = productReviewRepository.save(review);
        updateProductRating(review.getProduct());
        // Fix: Aggregate to seller rating (since product belongs to seller)
        if (review.getProduct().getSeller() != null) {
            updateSellerRating(review.getProduct().getSeller());
        }
        return review;
    }

    @Transactional
    public SellerReview createSellerReview(SellerReview review) {
        if (!isEligibleForSellerReview(review.getBuyer(), review.getSeller(), review.getOrder())) {
            throw new IllegalArgumentException("Not eligible to review this seller");
        }
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be 1-5");
        }
        List<SellerReview> existing = sellerReviewRepository.findByBuyerAndSellerAndIsActiveTrue(review.getBuyer(), review.getSeller());
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("You have already reviewed this seller");
        }
        review = sellerReviewRepository.save(review);
        updateSellerRating(review.getSeller());
        return review;
    }

    @Transactional
    public ReviewResponse createResponse(ReviewResponse response) {
        if (response.getProductReview() != null) {
            response.getProductReview().setResponse(response);
            productReviewRepository.save(response.getProductReview());
        } else if (response.getSellerReview() != null) {
            response.getSellerReview().setResponse(response);
            sellerReviewRepository.save(response.getSellerReview());
        }
        return reviewResponseRepository.save(response);
    }

    public Optional<ProductReview> getProductReview(Long id, User buyer) {
        return productReviewRepository.findByIdAndBuyer(id, buyer);
    }

    public Optional<SellerReview> getSellerReview(Long id, User buyer) {
        return sellerReviewRepository.findByIdAndBuyer(id, buyer);
    }

    @Transactional
    public ProductReview updateProductReview(Long id, ProductReview updated, User buyer) {
        Optional<ProductReview> existingOpt = getProductReview(id, buyer);
        if (existingOpt.isPresent() && "APPROVED".equals(existingOpt.get().getStatus())) {
            ProductReview existing = existingOpt.get();
            if (updated.getRating() != null) existing.setRating(updated.getRating());
            if (updated.getComment() != null) existing.setComment(updated.getComment());
            ProductReview saved = productReviewRepository.save(existing);
            updateProductRating(saved.getProduct());
            if (saved.getProduct().getSeller() != null) {
                updateSellerRating(saved.getProduct().getSeller()); // Aggregate to seller
            }
            return saved;
        }
        return null;
    }

    @Transactional
    public SellerReview updateSellerReview(Long id, SellerReview updated, User buyer) {
        Optional<SellerReview> existingOpt = getSellerReview(id, buyer);
        if (existingOpt.isPresent() && "APPROVED".equals(existingOpt.get().getStatus())) {
            SellerReview existing = existingOpt.get();
            if (updated.getRating() != null) existing.setRating(updated.getRating());
            if (updated.getComment() != null) existing.setComment(updated.getComment());
            SellerReview saved = sellerReviewRepository.save(existing);
            updateSellerRating(saved.getSeller());
            return saved;
        }
        return null;
    }

    @Transactional
    public void deleteProductReview(Long id, User buyer) {
        Optional<ProductReview> reviewOpt = getProductReview(id, buyer);
        if (reviewOpt.isPresent()) {
            ProductReview review = reviewOpt.get();
            review.setIsActive(false);
            productReviewRepository.save(review);
            updateProductRating(review.getProduct());
            if (review.getProduct().getSeller() != null) {
                updateSellerRating(review.getProduct().getSeller()); // Re-aggregate
            }
        }
    }

    @Transactional
    public void deleteSellerReview(Long id, User buyer) {
        Optional<SellerReview> reviewOpt = getSellerReview(id, buyer);
        if (reviewOpt.isPresent()) {
            SellerReview review = reviewOpt.get();
            review.setIsActive(false);
            sellerReviewRepository.save(review);
            updateSellerRating(review.getSeller());
        }
    }

    public void voteHelpful(Long reviewId, boolean isHelpful, User voter) {
        Optional<ProductReview> prOpt = productReviewRepository.findById(reviewId);
        if (prOpt.isPresent()) {
            ProductReview pr = prOpt.get();
            pr.setHelpfulnessVotes(isHelpful ? pr.getHelpfulnessVotes() + 1 : Math.max(0, pr.getHelpfulnessVotes() - 1));
            productReviewRepository.save(pr);
        }
        Optional<SellerReview> srOpt = sellerReviewRepository.findById(reviewId);
        if (srOpt.isPresent()) {
            SellerReview sr = srOpt.get();
            sr.setHelpfulnessVotes(isHelpful ? sr.getHelpfulnessVotes() + 1 : Math.max(0, sr.getHelpfulnessVotes() - 1));
            sellerReviewRepository.save(sr);
        }
    }

    // Admin moderation
    @Transactional
    public void moderateReview(Long reviewId, String action, String reason, boolean isProductReview) {
        if (isProductReview) {
            Optional<ProductReview> prOpt = productReviewRepository.findById(reviewId);
            if (prOpt.isPresent()) {
                ProductReview review = prOpt.get();
                review.setStatus(action);
                if ("REJECT".equals(action)) {
                    review.setComment(review.getComment() + " (Rejected: " + reason + ")");
                }
                productReviewRepository.save(review);
                if ("APPROVED".equals(action)) {
                    updateProductRating(review.getProduct());
                    if (review.getProduct().getSeller() != null) {
                        updateSellerRating(review.getProduct().getSeller());
                    }
                }
            }
        } else {
            Optional<SellerReview> srOpt = sellerReviewRepository.findById(reviewId);
            if (srOpt.isPresent()) {
                SellerReview review = srOpt.get();
                review.setStatus(action);
                if ("REJECT".equals(action)) {
                    review.setComment(review.getComment() + " (Rejected: " + reason + ")");
                }
                sellerReviewRepository.save(review);
                if ("APPROVED".equals(action)) updateSellerRating(review.getSeller());
            }
        }
    }

    private void updateProductRating(Product product) {
        Double avg = productReviewRepository.findAverageRatingByProduct(product, "APPROVED");
        if (avg != null) {
            System.out.println("Updated product avg rating: " + avg);
        }
    }

    private void updateSellerRating(User seller) {
        Double avg = sellerReviewRepository.findAverageRatingBySeller(seller, "APPROVED");
        if (avg != null) {
            sellerProfileService.updateAverageRating(seller, avg);
        }
    }

    @SuppressWarnings("unused") // Internal use
    public List<ProductReview> getProductReviewsSorted(Product product, String sortBy, String filter) {
        List<ProductReview> reviews = productReviewRepository.findByProductAndIsActiveTrueOrderByCreatedAtDesc(product);
        if ("highest_rated".equals(sortBy)) {
            reviews = reviews.stream().sorted((r1, r2) -> r2.getRating().compareTo(r1.getRating())).collect(Collectors.toList());
        } else if ("most_helpful".equals(sortBy)) {
            reviews = reviews.stream().sorted((r1, r2) -> r2.getHelpfulnessVotes().compareTo(r1.getHelpfulnessVotes())).collect(Collectors.toList());
        }
        if ("approved".equals(filter)) {
            reviews = reviews.stream().filter(r -> "APPROVED".equals(r.getStatus())).collect(Collectors.toList());
        }
        return reviews;
    }

    public List<SellerReview> getSellerReviewsSorted(User seller, String sortBy, String filter) {
        List<SellerReview> reviews = sellerReviewRepository.findBySellerAndIsActiveTrueOrderByCreatedAtDesc(seller);
        if ("highest_rated".equals(sortBy)) {
            reviews = reviews.stream().sorted((r1, r2) -> r2.getRating().compareTo(r1.getRating())).collect(Collectors.toList());
        } else if ("most_helpful".equals(sortBy)) {
            reviews = reviews.stream().sorted((r1, r2) -> r2.getHelpfulnessVotes().compareTo(r1.getHelpfulnessVotes())).collect(Collectors.toList());
        }
        if ("approved".equals(filter)) {
            reviews = reviews.stream().filter(r -> "APPROVED".equals(r.getStatus())).collect(Collectors.toList());
        }
        return reviews;
    }

    public ReviewAnalytics getReviewAnalytics(User seller) {
        ReviewAnalytics analytics = new ReviewAnalytics();
        analytics.setAverageRating(sellerReviewRepository.findAverageRatingBySeller(seller, "APPROVED"));
        analytics.setTotalReviews(sellerReviewRepository.countBySellerAndIsActiveTrue(seller, "APPROVED"));
        List<SellerReview> reviews = sellerReviewRepository.findBySellerAndIsActiveTrueOrderByCreatedAtDesc(seller);
        analytics.setPositiveReviews(reviews.stream().filter(r -> r.getRating() > 3 && "APPROVED".equals(r.getStatus())).count());
        analytics.setNegativeReviews(reviews.stream().filter(r -> r.getRating() <= 3 && "APPROVED".equals(r.getStatus())).count());
        return analytics;
    }

    public static class ReviewAnalytics {
        private Double averageRating;
        private Long totalReviews;
        private Long positiveReviews;
        private Long negativeReviews;

        // Getters/Setters
        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
        public Long getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Long totalReviews) { this.totalReviews = totalReviews; }
        public Long getPositiveReviews() { return positiveReviews; }
        public void setPositiveReviews(Long positiveReviews) { this.positiveReviews = positiveReviews; }
        public Long getNegativeReviews() { return negativeReviews; }
        public void setNegativeReviews(Long negativeReviews) { this.negativeReviews = negativeReviews; }
    }

    public String getTrustScore(User seller) {
        Double avg = sellerReviewRepository.findAverageRatingBySeller(seller, "APPROVED");
        if (avg == null) return "New";
        return avg > 4 ? "High" : (avg > 3 ? "Medium" : "Low");
    }

    public Optional<ProductReview> getProductReviewById(Long id) {
        return productReviewRepository.findById(id);
    }

    public Double getAverageProductRating(Product product) {
        return productReviewRepository.findAverageRatingByProduct(product, "APPROVED");
    }
}