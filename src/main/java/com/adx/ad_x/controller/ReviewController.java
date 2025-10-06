package com.adx.ad_x.controller;

import com.adx.ad_x.model.*;
import com.adx.ad_x.service.ProductReviewService;
import com.adx.ad_x.service.ProductService;
import com.adx.ad_x.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ProductReviewService reviewService;

    @Autowired
    private ProductService productService;

    @Autowired
    private NotificationService notificationService;

    // Submit review form
    @GetMapping("/submit/{productId}")
    public String showSubmitReviewForm(@PathVariable Long productId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"BUYER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Optional<Product> product = productService.getProductById(productId);
        if (product.isPresent()) {
            // Check if buyer can review this product
            boolean canReview = reviewService.canBuyerReviewProduct(user, product.get());
            if (!canReview) {
                model.addAttribute("error", "You can only review products you have purchased and haven't reviewed yet.");
                return "redirect:/buyer/product/" + productId;
            }

            model.addAttribute("product", product.get());
            model.addAttribute("review", new ProductReview());
            model.addAttribute("pageTitle", "AD-X - Submit Review");
            return "submit-review";
        }

        model.addAttribute("error", "Product not found.");
        return "redirect:/buyer/browse";
    }

    // Submit review
    @PostMapping("/submit/{productId}")
    public String submitReview(@PathVariable Long productId,
                               @RequestParam Integer rating,
                               @RequestParam String comment,
                               HttpSession session,
                               Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"BUYER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Optional<Product> product = productService.getProductById(productId);
        if (product.isPresent()) {
            try {
                ProductReview review = reviewService.createReview(product.get(), user, rating, comment);
                model.addAttribute("success", "Review submitted successfully!");
                return "redirect:/buyer/product/" + productId;
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
                model.addAttribute("product", product.get());
                model.addAttribute("rating", rating);
                model.addAttribute("comment", comment);
                return "submit-review";
            }
        }

        model.addAttribute("error", "Product not found.");
        return "redirect:/buyer/browse";
    }

    // Edit review form
    @GetMapping("/edit/{reviewId}")
    public String showEditReviewForm(@PathVariable Long reviewId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<ProductReview> review = reviewService.getReviewById(reviewId);
        if (review.isPresent() && review.get().getBuyer().getId().equals(user.getId())) {
            model.addAttribute("review", review.get());
            model.addAttribute("pageTitle", "AD-X - Edit Review");
            return "edit-review"; // Make sure this matches the template name
        }

        model.addAttribute("error", "Review not found or you don't have permission to edit it.");
        return "redirect:/buyer/purchases";
    }

    // Update review
    @PostMapping("/edit/{reviewId}")
    public String updateReview(@PathVariable Long reviewId,
                               @RequestParam Integer rating,
                               @RequestParam String comment,
                               HttpSession session,
                               Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            ProductReview updatedReview = reviewService.updateReview(reviewId, user, rating, comment);
            model.addAttribute("success", "Review updated successfully!");
            return "redirect:/buyer/product/" + updatedReview.getProduct().getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            Optional<ProductReview> review = reviewService.getReviewById(reviewId);
            if (review.isPresent()) {
                model.addAttribute("review", review.get());
            }
            return "edit-review";
        }
    }

    // Delete review
    @PostMapping("/delete/{reviewId}")
    public String deleteReview(@PathVariable Long reviewId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        boolean deleted = reviewService.deleteReview(reviewId, user);
        if (deleted) {
            model.addAttribute("success", "Review deleted successfully!");
        } else {
            model.addAttribute("error", "Failed to delete review.");
        }

        return "redirect:/buyer/purchases";
    }

    // View my reviews (for buyers)
    @GetMapping("/my-reviews")
    public String viewMyReviews(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<ProductReview> reviews = reviewService.getBuyerReviews(user);
        model.addAttribute("reviews", reviews);
        model.addAttribute("pageTitle", "AD-X - My Reviews");
        return "my-reviews";
    }

    // View seller reviews (for sellers)
    @GetMapping("/seller-reviews")
    public String viewSellerReviews(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SELLER".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<ProductReview> reviews = reviewService.getSellerReviews(user);
        Double averageRating = reviewService.getSellerReviewCount(user) > 0 ?
                reviews.stream().mapToInt(ProductReview::getRating).average().orElse(0.0) : 0.0;

        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("pageTitle", "AD-X - Customer Reviews");
        return "seller-reviews";
    }
}