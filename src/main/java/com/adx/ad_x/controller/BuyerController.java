package com.adx.ad_x.controller;

import com.adx.ad_x.model.*;
import com.adx.ad_x.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/buyer")
public class BuyerController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentService paymentService;

    // Check if user is buyer
    private boolean isBuyer(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "BUYER".equals(user.getRole());
    }

    // Browse all products
    @GetMapping("/browse")
    public String browseProducts(HttpSession session,
                                 @RequestParam(value = "category", required = false) String category,
                                 @RequestParam(value = "search", required = false) String search,
                                 Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }
        User buyer = (User) session.getAttribute("user");
        List<Product> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProducts(search.trim());
            model.addAttribute("searchQuery", search);
        } else if (category != null && !category.trim().isEmpty()) {
            products = productService.getProductsByCategory(category.trim());
            model.addAttribute("selectedCategory", category);
        } else {
            products = productService.getAllActiveProducts();
        }
        for (Product product : products) {
            product.setFavorited(favoriteService.isProductFavorited(buyer, product));
        }
        model.addAttribute("products", products);
        model.addAttribute("pageTitle", "AD-X - Browse Products");
        return "buyer-browse";
    }

    // View product details
    @GetMapping("/product/{id}")
    public String viewProduct(@PathVariable Long id, HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }
        User buyer = (User) session.getAttribute("user");
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent() && product.get().getActive()) {
            Product productObj = product.get();
            productObj.setFavorited(favoriteService.isProductFavorited(buyer, productObj));
            model.addAttribute("product", productObj);
            model.addAttribute("pageTitle", "AD-X - " + productObj.getTitle());
            return "buyer-product-details";
        }
        model.addAttribute("error", "Product not found or unavailable.");
        return "redirect:/buyer/browse";
    }

    // Toggle favorite
    @PostMapping("/favorites/toggle/{productId}")
    public String toggleFavorite(@PathVariable Long productId, HttpSession session) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }
        User buyer = (User) session.getAttribute("user");
        Optional<Product> product = productService.getProductById(productId);
        if (product.isPresent()) {
            favoriteService.toggleFavorite(buyer, product.get());
        }
        return "redirect:/buyer/product/" + productId;
    }

    // Remove from favorites
    @PostMapping("/favorites/remove/{productId}")
    public String removeFavorite(@PathVariable Long productId, HttpSession session) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }
        User buyer = (User) session.getAttribute("user");
        Optional<Product> product = productService.getProductById(productId);
        if (product.isPresent()) {
            favoriteService.removeFromFavorites(buyer, product.get());
        }
        return "redirect:/buyer/favorites";
    }

    // View favorites
    @GetMapping("/favorites")
    public String viewFavorites(HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }
        User buyer = (User) session.getAttribute("user");
        List<Favorite> favorites = favoriteService.getUserFavorites(buyer);
        Long favoriteCount = favoriteService.getFavoriteCount(buyer);
        model.addAttribute("favorites", favorites);
        model.addAttribute("favoriteCount", favoriteCount);
        model.addAttribute("pageTitle", "AD-X - My Favorites");
        return "buyer-favorites";
    }

    // Buy product
    @PostMapping("/buy/{productId}")
    public String buyProduct(@PathVariable Long productId, HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Product> product = productService.getProductById(productId);

        if (product.isPresent() && product.get().getActive()) {
            Order order = orderService.createSingleOrder(buyer, product.get());
            model.addAttribute("success", "Order placed successfully! Please complete payment to confirm your order.");
            return "redirect:/buyer/purchases";
        }

        model.addAttribute("error", "Product not available.");
        return "redirect:/buyer/product/" + productId;
    }

    // View purchase history
    @GetMapping("/purchases")
    public String viewPurchases(HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        List<Order> orders = orderService.getUserOrders(buyer);
        Long orderCount = orderService.getUserOrderCount(buyer);

        // Get payment count for dashboard
        Long paymentCount = (long) orders.stream()
                .filter(order -> order.getPayment() != null)
                .count();

        model.addAttribute("orders", orders);
        model.addAttribute("orderCount", orderCount);
        model.addAttribute("paymentCount", paymentCount);
        model.addAttribute("pageTitle", "AD-X - Purchase History");
        return "buyer-purchases";
    }

    // Cancel order
    @PostMapping("/cancel-order/{orderId}")
    public String cancelOrder(@PathVariable Long orderId, HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        boolean cancelled = orderService.cancelOrder(orderId, buyer);

        if (cancelled) {
            model.addAttribute("success", "Order cancelled successfully.");
        } else {
            model.addAttribute("error", "Unable to cancel order. Order may be already processed.");
        }

        return "redirect:/buyer/purchases";
    }

    // Inquiries page
    @GetMapping("/inquiries")
    public String viewInquiries(HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        List<Inquiry> inquiries = inquiryService.getBuyerInquiries(buyer);
        Long inquiryCount = inquiryService.getUnreadInquiryCountForBuyer(buyer);

        model.addAttribute("inquiries", inquiries);
        model.addAttribute("inquiryCount", inquiryCount);
        model.addAttribute("pageTitle", "AD-X - My Inquiries");
        return "buyer-inquiries";
    }

    // Contact seller form
    @GetMapping("/contact/{productId}")
    public String contactSeller(@PathVariable Long productId, HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }
        Optional<Product> product = productService.getProductById(productId);
        if (product.isPresent()) {
            model.addAttribute("product", product.get());
            model.addAttribute("pageTitle", "AD-X - Contact Seller");
            return "buyer-contact-seller";
        }
        return "redirect:/buyer/browse";
    }

    @PostMapping("/contact/{productId}")
    public String sendMessage(@PathVariable Long productId,
                              @RequestParam String message,
                              HttpSession session,
                              Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Product> product = productService.getProductById(productId);

        if (product.isPresent() && message != null && !message.trim().isEmpty()) {
            // Get the seller from the product
            User seller = product.get().getSeller();

            // Create inquiry
            inquiryService.createInquiry(buyer, seller, product.get(), message);

            model.addAttribute("success", "Your message has been sent to the seller!");
            model.addAttribute("product", product.get());
            return "buyer-contact-seller";
        }

        model.addAttribute("error", "Please enter a message.");
        model.addAttribute("product", product.orElse(null));
        return "buyer-contact-seller";
    }

    // NEW: View payment method selection
    @GetMapping("/payment/{orderId}")
    public String selectPaymentMethod(@PathVariable Long orderId, HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Order> order = orderService.getOrderById(orderId);

        if (order.isPresent() && order.get().getBuyer().getId().equals(buyer.getId())) {
            model.addAttribute("order", order.get());
            model.addAttribute("pageTitle", "AD-X - Select Payment Method");
            return "payment-method";
        }

        return "redirect:/buyer/purchases";
    }

    // NEW: View payment history
    @GetMapping("/payment/history")
    public String paymentHistory(HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        List<Payment> payments = paymentService.getPaymentsByBuyer(buyer);

        // Calculate total spent safely
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (Payment payment : payments) {
            if ("COMPLETED".equals(payment.getStatus()) && payment.getAmount() != null) {
                totalSpent = totalSpent.add(payment.getAmount());
            }
        }

        model.addAttribute("payments", payments);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("pageTitle", "AD-X - Payment History");
        return "buyer-payment-history";
    }
}