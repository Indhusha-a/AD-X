package com.adx.ad_x.controller;

import com.adx.ad_x.model.*;
import com.adx.ad_x.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService; // For setting product from ID

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("user");
    }

    private boolean isBuyer(HttpSession session) {
        User user = getCurrentUser(session);
        return user != null && "BUYER".equals(user.getRole());
    }

    private boolean isSeller(HttpSession session) {
        User user = getCurrentUser(session);
        return user != null && "SELLER".equals(user.getRole());
    }

    // Submit product review
    @PostMapping("/product/submit")
    public String submitProductReview(@ModelAttribute ProductReview review,
                                      @RequestParam(required = false) Long orderItemId,
                                      HttpServletRequest request,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        if (!isBuyer(session)) return "redirect:/login";
        User buyer = getCurrentUser(session);
        review.setBuyer(buyer);

        // Fix: Parse productId from form (no DataFlavor)
        String productIdStr = request.getParameter("productId");
        if (productIdStr != null && !productIdStr.isEmpty()) {
            Long productId = Long.valueOf(productIdStr);
            Optional<Product> productOpt = productService.getProductById(productId);
            if (productOpt.isPresent()) {
                review.setProduct(productOpt.get());
            } else {
                redirectAttributes.addFlashAttribute("error", "Product not found");
                return "redirect:/buyer/browse";
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Product ID missing");
            return "redirect:/buyer/browse";
        }

        // Set orderItem (optional now)
        if (orderItemId != null) {
            Optional<Order> orderOpt = orderService.getOrderById(orderItemId);
            OrderItem orderItem = null;
            if (orderOpt.isPresent()) {
                orderItem = orderOpt.get().getItems().stream().findFirst().orElse(null);
            }
            if (orderItem != null) {
                review.setOrderItem(orderItem);
            }
        }

        try {
            reviewService.createProductReview(review);
            redirectAttributes.addFlashAttribute("success", "Review submitted for moderation!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/buyer/product/" + review.getProduct().getId();
    }

    // Submit seller review
    @PostMapping("/seller/submit")
    public String submitSellerReview(@ModelAttribute SellerReview review,
                                     @RequestParam Long orderId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!isBuyer(session)) return "redirect:/login";
        User buyer = getCurrentUser(session);
        review.setBuyer(buyer);
        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isPresent()) {
            review.setOrder(orderOpt.get());
            User seller = orderOpt.get().getItems().get(0).getProduct().getSeller(); // Assume first item seller
            review.setSeller(seller);
        }
        try {
            reviewService.createSellerReview(review);
            redirectAttributes.addFlashAttribute("success", "Seller review submitted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/buyer/purchases";
    }

    // View product reviews
    @GetMapping("/product/{productId}")
    public String viewProductReviews(@PathVariable Long productId,
                                     @RequestParam(defaultValue = "newest") String sortBy,
                                     @RequestParam(defaultValue = "all") String filter,
                                     Model model,
                                     HttpSession session) {
        User buyer = getCurrentUser(session);
        Optional<Product> productOpt = productService.getProductById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            List<ProductReview> reviews = reviewService.getProductReviewsSorted(product, sortBy, filter);
            Double avgRating = reviewService.getAverageProductRating(product);
            model.addAttribute("reviews", reviews);
            model.addAttribute("avgRating", avgRating != null ? avgRating : 0.0);
            model.addAttribute("product", product);
            model.addAttribute("canReview", reviews.stream().noneMatch(r -> r.getBuyer().getId().equals(buyer.getId())));
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("filter", filter);
        }
        return "buyer-product-details"; // Integrate into existing template
    }

    // Vote helpful
    @PostMapping("/vote/{reviewId}")
    public String vote(@PathVariable Long reviewId,
                       @RequestParam boolean helpful,
                       @RequestParam Long productId, // Added to pass for redirect
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        if (!isBuyer(session)) return "redirect:/login";
        reviewService.voteHelpful(reviewId, helpful, getCurrentUser(session));
        redirectAttributes.addFlashAttribute("success", "Vote recorded!");
        return "redirect:/reviews/product/" + productId;
    }

    // Seller add response
    @PostMapping("/response/{reviewId}")
    public String addResponse(@PathVariable Long reviewId,
                              @RequestParam String responseText,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!isSeller(session)) return "redirect:/login";
        User seller = getCurrentUser(session);
        // Assume reviewId is for ProductReview; extend for SellerReview
        Optional<ProductReview> prOpt = reviewService.getProductReviewById(reviewId);
        if (prOpt.isPresent()) {
            ReviewResponse response = new ReviewResponse(seller, responseText, prOpt.get());
            reviewService.createResponse(response);
            redirectAttributes.addFlashAttribute("success", "Response added!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Review not found");
        }
        return "redirect:/seller/reviews";
    }

    // Admin moderation
    @PostMapping("/admin/moderate/{reviewId}")
    public String moderate(@PathVariable Long reviewId,
                           @RequestParam String action,
                           @RequestParam String reason,
                           @RequestParam(defaultValue = "true") boolean isProductReview,
                           HttpSession session) {
        // Admin check delegated to AdminController
        reviewService.moderateReview(reviewId, action, reason, isProductReview);
        return "redirect:/admin/reviews";
    }
}