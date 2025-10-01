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

    @Autowired
    private ReviewService reviewService;

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
    public String viewProduct(@PathVariable Long id,
                              HttpSession session,
                              Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setFavorited(favoriteService.isProductFavorited(buyer, product));

            // Reviews
            List<ProductReview> reviews = reviewService.getProductReviewsSorted(product, "newest", "approved");
            Double avgRating = reviewService.getAverageProductRating(product);
            boolean canReview = reviews.stream().noneMatch(r -> r.getBuyer().getId().equals(buyer.getId()));

            // Set orderItemId if canReview
            Long orderItemId = null;
            if (canReview) {
                List<OrderItem> purchasedItems = orderService.getPurchasedItemsForProduct(buyer, product);
                if (!purchasedItems.isEmpty()) {
                    orderItemId = purchasedItems.get(0).getId();
                }
            }

            model.addAttribute("product", product);
            model.addAttribute("reviews", reviews);
            model.addAttribute("avgRating", avgRating != null ? avgRating : 0.0);
            model.addAttribute("canReview", canReview);
            model.addAttribute("orderItemId", orderItemId);
            model.addAttribute("pageTitle", "AD-X - Product Details");
            return "buyer-product-details";
        }
        return "redirect:/buyer/browse";
    }

    // Toggle favorite
    @PostMapping("/favorites/toggle/{id}")
    public String toggleFavorite(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            favoriteService.toggleFavorite(buyer, product);
            redirectAttributes.addFlashAttribute("success", "Favorite updated!");
        }
        return "redirect:/buyer/product/" + id;
    }

    // Buy product
    @PostMapping("/buy/{id}")
    public String buyProduct(@PathVariable Long id, HttpSession session) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isPresent()) {
            Order order = orderService.createSingleOrder(buyer, productOpt.get());
            return "redirect:/buyer/payment/" + order.getId();
        }
        return "redirect:/buyer/browse";
    }

    // Fix: Contact seller - @GetMapping for load form, @PostMapping for submit
    @GetMapping("/contact/{id}")
    public String contactSellerForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isPresent()) {
            model.addAttribute("product", productOpt.get());
            model.addAttribute("inquiry", new Inquiry()); // For form binding
            model.addAttribute("pageTitle", "AD-X - Contact Seller");
            return "buyer-contact-seller";
        }
        return "redirect:/buyer/browse";
    }

    @PostMapping("/contact/{id}")
    public String contactSeller(@PathVariable Long id, @ModelAttribute Inquiry inquiry, HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Product> productOpt = productService.getProductById(id);

        if (productOpt.isPresent() && inquiry.getMessage() != null && !inquiry.getMessage().trim().isEmpty()) {
            // Get the seller from the product
            User seller = productOpt.get().getSeller();

            // Create inquiry
            inquiry.setBuyer(buyer);
            inquiry.setSeller(seller);
            inquiry.setProduct(productOpt.get());
            inquiryService.createInquiry(inquiry);

            model.addAttribute("success", "Your message has been sent to the seller!");
            model.addAttribute("product", productOpt.get());
            return "buyer-contact-seller";
        }

        model.addAttribute("error", "Please enter a message.");
        model.addAttribute("product", productOpt.orElse(null));
        model.addAttribute("inquiry", inquiry);
        return "buyer-contact-seller";
    }

    // View payment method selection
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

    // View payment history
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

    // New: View purchases / order history
    @GetMapping("/purchases")
    public String purchases(HttpSession session, Model model) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        List<Order> orders = orderService.getUserOrders(buyer);

        // Stats
        Long totalOrders = orderService.getUserOrderCount(buyer);
        Long paidOrders = orders.stream().filter(Order::isPaid).count();

        model.addAttribute("orders", orders);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("paidOrders", paidOrders);
        model.addAttribute("pageTitle", "AD-X - Purchase History");
        return "buyer-purchases";
    }

    // New: Write product review
    @GetMapping("/review/product/{orderItemId}")
    public String writeProductReview(@PathVariable Long orderItemId, Model model, HttpSession session) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Order> orderOpt = orderService.getOrderById(orderItemId); // Assume orderItemId is orderId for simplicity
        if (orderOpt.isPresent() && orderOpt.get().isPaid()) {
            OrderItem item = orderOpt.get().getItems().stream().findFirst().orElse(null);
            if (item != null) {
                Product product = item.getProduct();
                model.addAttribute("product", product);
                model.addAttribute("orderItemId", orderItemId);
                model.addAttribute("pageTitle", "AD-X - Write Product Review");
                return "write-product-review"; // New template or reuse form
            }
        }
        return "redirect:/buyer/purchases";
    }

    // New: Rate seller
    @GetMapping("/review/seller/{orderId}")
    public String rateSeller(@PathVariable Long orderId, Model model, HttpSession session) {
        if (!isBuyer(session)) {
            return "redirect:/login";
        }

        User buyer = (User) session.getAttribute("user");
        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isPresent() && orderOpt.get().isPaid()) {
            Order order = orderOpt.get();
            User seller = order.getItems().stream().findFirst().map(item -> item.getProduct().getSeller()).orElse(null);
            if (seller != null) {
                model.addAttribute("seller", seller);
                model.addAttribute("orderId", orderId);
                model.addAttribute("pageTitle", "AD-X - Rate Seller");
                return "write-seller-review"; // New template
            }
        }
        return "redirect:/buyer/purchases";
    }
}