package com.adx.ad_x.controller;

import com.adx.ad_x.model.*;
import com.adx.ad_x.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    // Process payment for an order
    @PostMapping("/process/{orderId}")
    public String processPayment(@PathVariable Long orderId,
                                 @RequestParam String paymentMethod,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Verify order belongs to user
            if (!order.getBuyer().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You are not authorized to pay for this order");
                return "redirect:/buyer/purchases";
            }

            // Process payment
            Payment payment = paymentService.processPayment(order, user, paymentMethod, "SIMULATED");

            if ("COMPLETED".equals(payment.getStatus())) {
                redirectAttributes.addFlashAttribute("success", "Payment processed successfully!");
                return "redirect:/payment/success/" + payment.getId();
            } else {
                redirectAttributes.addFlashAttribute("error", "Payment failed. Please try again.");
                return "redirect:/buyer/payment/" + orderId;
            }
        }

        redirectAttributes.addFlashAttribute("error", "Order not found");
        return "redirect:/buyer/purchases";
    }

    // Payment success page
    @GetMapping("/success/{paymentId}")
    public String paymentSuccess(@PathVariable Long paymentId,
                                 HttpSession session,
                                 Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Payment> paymentOpt = paymentService.getPaymentById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();

            // Verify payment belongs to user
            if (!payment.getBuyer().getId().equals(user.getId())) {
                return "redirect:/buyer/purchases";
            }

            model.addAttribute("payment", payment);
            model.addAttribute("order", payment.getOrder());
            model.addAttribute("pageTitle", "AD-X - Payment Success");
            return "payment-success";
        }

        return "redirect:/buyer/purchases";
    }

    // Buyer payment history
    @GetMapping("/history")
    public String paymentHistory(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Payment> payments = paymentService.getPaymentsByBuyer(user);
        model.addAttribute("payments", payments);
        model.addAttribute("pageTitle", "AD-X - Payment History");
        return "buyer-payment-history";
    }

    // Seller earnings dashboard
    @GetMapping("/earnings")
    public String sellerEarnings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SELLER".equals(user.getRole())) {
            return "redirect:/login";
        }

        // Get pending earnings
        BigDecimal pendingEarnings = payoutService.calculatePendingEarnings(user);

        // Get payout history
        List<Payout> payouts = payoutService.getPayoutsBySeller(user);

        // Get recent payments that contribute to earnings
        List<Payment> recentPayments = paymentService.getPaymentsByDateRange(
                LocalDateTime.now().minusDays(30), LocalDateTime.now());

        model.addAttribute("pendingEarnings", pendingEarnings);
        model.addAttribute("payouts", payouts);
        model.addAttribute("recentPayments", recentPayments);
        model.addAttribute("pageTitle", "AD-X - Earnings Dashboard");
        return "seller-earnings";
    }

    // Request payout
    @PostMapping("/payout/request")
    public String requestPayout(@RequestParam BigDecimal amount,
                                @RequestParam String payoutMethod,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SELLER".equals(user.getRole())) {
            return "redirect:/login";
        }

        try {
            Payout payout = payoutService.createPayout(user, amount, payoutMethod);
            redirectAttributes.addFlashAttribute("success", "Payout request submitted successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/payment/earnings";
    }
}