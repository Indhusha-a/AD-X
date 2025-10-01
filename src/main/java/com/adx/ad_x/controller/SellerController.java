package com.adx.ad_x.controller;

import com.adx.ad_x.model.*;
import com.adx.ad_x.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/seller")
public class SellerController {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private SellerProfileService sellerProfileService;

    @Autowired
    private SellerAnalyticsService sellerAnalyticsService;

    @Autowired
    private UserService userService;

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReviewService reviewService; // New for reviews

    // Check if user is seller
    private boolean isSeller(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "SELLER".equals(user.getRole());
    }

    // Seller Dashboard
    @GetMapping("/dashboard")
    public String sellerDashboard(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Get seller profile (create if doesn't exist)
        SellerProfile profile = sellerProfileService.getOrCreateSellerProfile(seller);

        // Get seller statistics with null safety
        Long productCount = productService.getProductCountBySeller(seller);
        Long orderCount = orderService.getOrderCountBySeller(seller);
        Long inquiryCount = inquiryService.getUnreadInquiryCountForSeller(seller);
        BigDecimal totalRevenue = orderService.getTotalRevenueBySeller(seller);

        // Get pending earnings
        BigDecimal pendingEarnings = payoutService.calculatePendingEarnings(seller);

        // Get recent activity
        List<Order> recentOrders = orderService.getRecentOrdersBySeller(seller, 5);
        List<Inquiry> recentInquiries = inquiryService.getSellerInquiries(seller).stream().limit(5).toList();

        // Get analytics summary with null safety
        SellerAnalytics analyticsSummary = sellerAnalyticsService.getSellerSummary(seller);

        // New: Review analytics and trust score
        ReviewService.ReviewAnalytics reviewAnalytics = reviewService.getReviewAnalytics(seller);
        String trustScore = reviewService.getTrustScore(seller);

        model.addAttribute("user", seller);
        model.addAttribute("profile", profile);
        model.addAttribute("productCount", productCount);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("inquiryCount", inquiryCount);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pendingEarnings", pendingEarnings);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("recentInquiries", recentInquiries);
        model.addAttribute("analyticsSummary", analyticsSummary);
        model.addAttribute("reviewAnalytics", reviewAnalytics);
        model.addAttribute("trustScore", trustScore);
        model.addAttribute("pageTitle", "AD-X - Seller Dashboard");
        return "seller-dashboard";
    }

    // View analytics
    @GetMapping("/analytics")
    public String viewAnalytics(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        List<SellerAnalytics> analytics = sellerAnalyticsService.getSellerAnalytics(seller, 30);
        SellerAnalytics summary = sellerAnalyticsService.getSellerSummary(seller);

        model.addAttribute("user", seller); // Add user to model
        model.addAttribute("analytics", analytics);
        model.addAttribute("summary", summary);
        model.addAttribute("pageTitle", "AD-X - Sales Analytics");
        return "seller-analytics";
    }

    // Profile Management
    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        SellerProfile profile = sellerProfileService.getOrCreateSellerProfile(seller);

        // New: Add trust score
        String trustScore = reviewService.getTrustScore(seller);

        model.addAttribute("user", seller); // Add user to model - THIS WAS MISSING
        model.addAttribute("profile", profile);
        model.addAttribute("trustScore", trustScore);
        model.addAttribute("pageTitle", "AD-X - Seller Profile");
        return "seller-profile";
    }

    // Update Profile
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute SellerProfile profileDetails,
                                HttpSession session,
                                Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        SellerProfile updatedProfile = sellerProfileService.updateSellerProfile(seller, profileDetails);

        if (updatedProfile != null) {
            model.addAttribute("success", "Profile updated successfully!");
            // After update, redirect back to profile page with updated data
            return "redirect:/seller/profile";
        } else {
            model.addAttribute("error", "Failed to update profile.");
            return "redirect:/seller/profile";
        }
    }

    // Earnings Dashboard
    @GetMapping("/earnings")
    public String viewEarnings(HttpSession session) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }
        return "redirect:/payment/earnings";
    }

    // New: Seller reviews and responses
    @GetMapping("/reviews")
    public String sellerReviews(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        List<SellerReview> reviews = reviewService.getSellerReviewsSorted(seller, "newest", "approved");
        ReviewService.ReviewAnalytics analytics = reviewService.getReviewAnalytics(seller);
        String trustScore = reviewService.getTrustScore(seller);

        model.addAttribute("user", seller);
        model.addAttribute("reviews", reviews);
        model.addAttribute("analytics", analytics);
        model.addAttribute("trustScore", trustScore);
        model.addAttribute("pageTitle", "AD-X - Seller Reviews");
        return "seller-reviews";
    }

    // Product management endpoints (to fix 404 on /seller/products/new and /seller/products)
    @GetMapping("/products")
    public String listProducts(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        List<Product> products = productService.getProductsBySeller(seller);
        model.addAttribute("products", products);
        model.addAttribute("pageTitle", "AD-X - My Products");
        return "seller-products"; // Matches your provided template
    }

    @GetMapping("/products/new")
    public String newProduct(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        model.addAttribute("product", new Product());
        model.addAttribute("pageTitle", "AD-X - Add New Product");
        model.addAttribute("action", "Create"); // For form to show "Create Product"
        return "seller-product-form"; // Matches your provided template
    }

    @PostMapping("/products/new")
    public String createProduct(@ModelAttribute Product product, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        Product savedProduct = productService.createProduct(product, seller);
        if (savedProduct != null) {
            redirectAttributes.addFlashAttribute("success", "Product created successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to create product.");
        }
        return "redirect:/seller/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id, HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        Optional<Product> productOpt = productService.getProductByIdAndSeller(id, seller);
        if (productOpt.isPresent()) {
            model.addAttribute("product", productOpt.get());
            model.addAttribute("pageTitle", "AD-X - Edit Product");
            model.addAttribute("action", "Update"); // For form to show "Update Product"
            return "seller-product-form"; // Reuse the same form template
        }
        return "redirect:/seller/products";
    }

    @PostMapping("/products/edit/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute Product productDetails, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        Product savedProduct = productService.updateProduct(id, productDetails, seller);
        if (savedProduct != null) {
            redirectAttributes.addFlashAttribute("success", "Product updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to update product.");
        }
        return "redirect:/seller/products";
    }

    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        boolean deleted = productService.deleteProduct(id, seller);
        if (deleted) {
            redirectAttributes.addFlashAttribute("success", "Product deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete product.");
        }
        return "redirect:/seller/products";
    }

    // New: Inquiries endpoints (to fix 404 on /seller/inquiries)
    @GetMapping("/inquiries")
    public String listInquiries(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        List<Inquiry> inquiries = inquiryService.getSellerInquiries(seller);
        Long unreadCount = inquiryService.getUnreadInquiryCountForSeller(seller);
        Long totalInquiries = (long) inquiries.size();

        model.addAttribute("inquiries", inquiries);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("totalInquiries", totalInquiries);
        model.addAttribute("pageTitle", "AD-X - Customer Inquiries");
        return "seller-inquiries"; // Matches your provided template
    }

    @GetMapping("/inquiries/{id}")
    public String inquiryDetails(@PathVariable Long id, HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        Optional<Inquiry> inquiryOpt = inquiryService.getInquiryById(id);
        if (inquiryOpt.isPresent() && inquiryOpt.get().getSeller().getId().equals(seller.getId())) {
            Inquiry inquiry = inquiryOpt.get();
            inquiry.setIsRead(true); // Mark as read
            inquiryService.createInquiry(inquiry);

            model.addAttribute("inquiry", inquiry);
            model.addAttribute("pageTitle", "AD-X - Inquiry Details");
            return "seller-inquiry-details"; // Matches your provided template
        }
        return "redirect:/seller/inquiries";
    }

    @PostMapping("/inquiries/{id}/respond")
    public String respondToInquiry(@PathVariable Long id, @RequestParam String response, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        Optional<Inquiry> inquiryOpt = inquiryService.getInquiryById(id);
        if (inquiryOpt.isPresent() && inquiryOpt.get().getSeller().getId().equals(seller.getId())) {
            // Add response logic (e.g., create reply inquiry or update field)
            inquiryService.clone(inquiryOpt.get(), response, seller);
            redirectAttributes.addFlashAttribute("success", "Response sent to buyer!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Inquiry not found.");
        }
        return "redirect:/seller/inquiries/" + id;
    }
}