package com.adx.ad_x.controller;

import com.adx.ad_x.model.*;
import com.adx.ad_x.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private NotificationService notificationService;

    @Autowired
    private ProductReviewService reviewService;

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

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

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
        List<ProductReview> recentReviews = reviewService.getSellerReviews(seller).stream().limit(5).toList();

        // Get analytics summary with null safety
        SellerAnalytics analyticsSummary = sellerAnalyticsService.getSellerSummary(seller);

        // Create safe analytics data for template
        Map<String, Object> safeAnalytics = new HashMap<>();
        safeAnalytics.put("totalViews", analyticsSummary != null ? analyticsSummary.getTotalViews() : 0);
        safeAnalytics.put("inquiriesReceived", analyticsSummary != null ? analyticsSummary.getInquiriesReceived() : 0);
        safeAnalytics.put("ordersReceived", analyticsSummary != null ? analyticsSummary.getOrdersReceived() : 0);
        safeAnalytics.put("revenueGenerated", analyticsSummary != null ? analyticsSummary.getRevenueGenerated() : BigDecimal.ZERO);
        safeAnalytics.put("conversionRate", analyticsSummary != null ? analyticsSummary.getConversionRate() : BigDecimal.ZERO);

        model.addAttribute("user", seller);
        model.addAttribute("profile", profile);
        model.addAttribute("productCount", productCount);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("inquiryCount", inquiryCount);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pendingEarnings", pendingEarnings);
        model.addAttribute("recentOrders", recentOrders != null ? recentOrders : new ArrayList<>());
        model.addAttribute("recentInquiries", recentInquiries != null ? recentInquiries : new ArrayList<>());
        model.addAttribute("recentReviews", recentReviews != null ? recentReviews : new ArrayList<>());
        model.addAttribute("analyticsSummary", safeAnalytics);
        model.addAttribute("pageTitle", "AD-X - Seller Dashboard");

        return "seller-dashboard";
    }

    // Product Management - List Products
    @GetMapping("/products")
    public String listProducts(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        List<Product> products = productService.getProductsBySeller(seller);

        model.addAttribute("user", seller);
        model.addAttribute("products", products);
        model.addAttribute("pageTitle", "AD-X - My Products");
        return "seller-products";
    }

    // Show Add Product Form
    @GetMapping("/products/new")
    public String showAddProductForm(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        model.addAttribute("user", seller);
        model.addAttribute("product", new Product());
        model.addAttribute("pageTitle", "AD-X - Add Product");
        return "seller-product-form";
    }

    // Show Edit Product Form
    @GetMapping("/products/edit/{id}")
    public String showEditProductForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        Optional<Product> product = productService.getProductByIdAndSeller(id, seller);

        if (product.isPresent()) {
            model.addAttribute("user", seller);
            model.addAttribute("product", product.get());
            model.addAttribute("pageTitle", "AD-X - Edit Product");
            return "seller-product-form";
        }

        model.addAttribute("error", "Product not found or you don't have permission to edit it.");
        return "redirect:/seller/products";
    }

    // Save Product (Create or Update)
    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product,
                              HttpSession session,
                              Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        try {
            if (product.getId() == null) {
                // Create new product
                productService.createProduct(product, seller);
                model.addAttribute("success", "Product created successfully!");
            } else {
                // Update existing product
                Product updatedProduct = productService.updateProduct(product.getId(), product, seller);
                if (updatedProduct != null) {
                    model.addAttribute("success", "Product updated successfully!");
                } else {
                    model.addAttribute("error", "Product not found or you don't have permission to edit it.");
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error saving product: " + e.getMessage());
        }

        return "redirect:/seller/products";
    }

    // Delete Product
    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        boolean deleted = productService.deleteProduct(id, seller);

        if (deleted) {
            model.addAttribute("success", "Product deleted successfully!");
        } else {
            model.addAttribute("error", "Product not found or you don't have permission to delete it.");
        }

        return "redirect:/seller/products";
    }

    // Order Management
    @GetMapping("/orders")
    public String listOrders(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        List<Order> orders = orderService.getOrdersBySeller(seller);

        model.addAttribute("user", seller);
        model.addAttribute("orders", orders);
        model.addAttribute("pageTitle", "AD-X - My Orders");
        return "seller-orders";
    }

    // Update Order Status
    @PostMapping("/orders/update-status/{id}")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    HttpSession session,
                                    Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");
        boolean updated = orderService.updateOrderStatus(id, status, seller);

        if (updated) {
            model.addAttribute("success", "Order status updated successfully!");
        } else {
            model.addAttribute("error", "Order not found or you don't have permission to update it.");
        }

        return "redirect:/seller/orders";
    }

    // Inquiry Management
    @GetMapping("/inquiries")
    public String listInquiries(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        List<Inquiry> inquiries = inquiryService.getSellerInquiries(seller);
        Long unreadCount = inquiryService.getUnreadInquiryCountForSeller(seller);

        model.addAttribute("user", seller);
        model.addAttribute("inquiries", inquiries);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("pageTitle", "AD-X - Customer Inquiries");
        return "seller-inquiries";
    }

    // View Inquiry Details
    @GetMapping("/inquiries/{id}")
    public String viewInquiry(@PathVariable Long id, HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        Optional<Inquiry> inquiry = inquiryService.getInquiryById(id);

        if (inquiry.isPresent() && inquiry.get().getSeller().getId().equals(seller.getId())) {
            // Mark as read when viewing
            inquiryService.markInquiryAsRead(id, seller);

            model.addAttribute("user", seller);
            model.addAttribute("inquiry", inquiry.get());
            model.addAttribute("pageTitle", "AD-X - Inquiry Details");
            return "seller-inquiry-details";
        }

        model.addAttribute("error", "Inquiry not found.");
        return "redirect:/seller/inquiries";
    }

    // Respond to Inquiry
    @PostMapping("/inquiries/respond/{id}")
    public String respondToInquiry(@PathVariable Long id,
                                   @RequestParam String response,
                                   HttpSession session,
                                   Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        if (response == null || response.trim().isEmpty()) {
            model.addAttribute("error", "Please enter a response message.");
            return "redirect:/seller/inquiries/" + id;
        }

        Inquiry responseInquiry = inquiryService.createInquiryResponse(id, response, seller);

        if (responseInquiry != null) {
            model.addAttribute("success", "Response sent successfully!");
        } else {
            model.addAttribute("error", "Failed to send response.");
        }

        return "redirect:/seller/inquiries/" + id;
    }

    // Analytics Dashboard
    @GetMapping("/analytics")
    public String viewAnalytics(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        List<SellerAnalytics> analytics = sellerAnalyticsService.getSellerAnalytics(seller, 30);
        SellerAnalytics summary = sellerAnalyticsService.getSellerSummary(seller);

        model.addAttribute("user", seller);
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

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        SellerProfile profile = sellerProfileService.getOrCreateSellerProfile(seller);

        model.addAttribute("user", seller);
        model.addAttribute("profile", profile);
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
        } else {
            model.addAttribute("error", "Failed to update profile.");
        }

        return "redirect:/seller/profile";
    }

    // Earnings Dashboard
    @GetMapping("/earnings")
    public String viewEarnings(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        return "redirect:/payment/earnings";
    }

    // Seller Reviews Management
    @GetMapping("/reviews")
    public String viewSellerReviews(HttpSession session, Model model) {
        if (!isSeller(session)) {
            return "redirect:/login";
        }

        User seller = (User) session.getAttribute("user");

        // Add notification count to model
        Long notificationCount = notificationService.getUnreadNotificationCount(seller);
        model.addAttribute("notificationCount", notificationCount);

        List<ProductReview> reviews = reviewService.getSellerReviews(seller);
        Double averageRating = reviewService.getSellerReviewCount(seller) > 0 ?
                reviews.stream().mapToInt(ProductReview::getRating).average().orElse(0.0) : 0.0;

        model.addAttribute("user", seller);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("pageTitle", "AD-X - Customer Reviews");
        return "seller-reviews";
    }
}