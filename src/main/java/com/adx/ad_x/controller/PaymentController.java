package com.adx.ad_x.controller;

import com.adx.ad_x.model.*;
import com.adx.ad_x.service.PaymentService;
import com.adx.ad_x.service.PayoutService;
import com.adx.ad_x.service.OrderService;
import com.adx.ad_x.service.NotificationService;
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
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private NotificationService notificationService;

    // Process payment for an order - FIXED: Made card details optional for non-card payments
    @PostMapping("/process/{orderId}")
    public String processPayment(@PathVariable Long orderId,
                                 @RequestParam String paymentMethod,
                                 @RequestParam(required = false) String cardNumber,
                                 @RequestParam(required = false) String expiryDate,
                                 @RequestParam(required = false) String cvv,
                                 @RequestParam(required = false) String cardHolderName,
                                 HttpSession session,
                                 Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"BUYER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderService.getOrderById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Verify order belongs to the current user
            if (!order.getBuyer().getId().equals(user.getId())) {
                model.addAttribute("error", "You can only pay for your own orders.");
                return "redirect:/buyer/purchases";
            }

            try {
                // Generate a mock transaction ID based on payment method
                String transactionId;
                if ("CREDIT_CARD".equals(paymentMethod)) {
                    transactionId = "CC_" + System.currentTimeMillis();
                    // Validate card details for credit card payments
                    if (cardNumber == null || cardNumber.trim().isEmpty() ||
                            expiryDate == null || expiryDate.trim().isEmpty() ||
                            cvv == null || cvv.trim().isEmpty() ||
                            cardHolderName == null || cardHolderName.trim().isEmpty()) {
                        model.addAttribute("error", "Please provide all card details for credit card payment.");
                        model.addAttribute("order", orderOpt.get());
                        return "payment-method";
                    }
                } else if ("BANK_TRANSFER".equals(paymentMethod)) {
                    transactionId = "BT_" + System.currentTimeMillis();
                    // Bank transfer doesn't need card details
                } else if ("BANK_SLIP".equals(paymentMethod)) {
                    transactionId = "BS_" + System.currentTimeMillis();
                    // Bank slip doesn't need card details
                } else {
                    transactionId = "TXN_" + System.currentTimeMillis();
                }

                // Process payment
                Payment payment = paymentService.processPayment(order, paymentMethod, transactionId);

                // DO NOT auto-confirm order - let seller confirm it manually
                // Order status remains PENDING until seller confirms
                // orderService.updateOrderStatus(orderId, "CONFIRMED");

                // Create notification
                notificationService.createNotification(user,
                        "Payment Successful",
                        "Your payment of $" + payment.getAmount() + " for order #" + orderId + " was successful.",
                        "PAYMENT",
                        payment.getId(),
                        "PAYMENT");

                // Redirect to payment success page with payment details
                model.addAttribute("payment", payment);
                model.addAttribute("order", order);
                model.addAttribute("pageTitle", "AD-X - Payment Successful");
                return "payment-success";

            } catch (Exception e) {
                model.addAttribute("error", "Payment processing failed: " + e.getMessage());
                model.addAttribute("order", orderOpt.get());
                return "payment-method";
            }
        }

        model.addAttribute("error", "Order not found.");
        return "redirect:/buyer/purchases";
    }

    // View payment details
    @GetMapping("/details/{paymentId}")
    public String viewPaymentDetails(@PathVariable Long paymentId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Payment> paymentOpt = paymentService.getPaymentById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();

            // Check if user has permission to view this payment
            if (!payment.getBuyer().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
                model.addAttribute("error", "You don't have permission to view this payment.");
                return "redirect:/dashboard";
            }

            model.addAttribute("payment", payment);
            model.addAttribute("pageTitle", "AD-X - Payment Details");
            return "payment-details";
        }

        model.addAttribute("error", "Payment not found.");
        return "redirect:/dashboard";
    }

    // View payment history for buyer
    @GetMapping("/history")
    public String viewPaymentHistory(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Payment> payments = paymentService.getPaymentsByBuyer(user);

        // Calculate total spent
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

    // Seller earnings dashboard
    @GetMapping("/earnings")
    public String viewEarnings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"SELLER".equals(user.getRole())) {
            return "redirect:/login";
        }

        // Calculate pending earnings
        BigDecimal pendingEarnings = payoutService.calculatePendingEarnings(user);

        // Get payout history
        List<Payout> payouts = payoutService.getPayoutsBySeller(user);

        // Calculate total earned
        BigDecimal totalEarned = BigDecimal.ZERO;
        for (Payout payout : payouts) {
            if ("PROCESSED".equals(payout.getStatus()) && payout.getAmount() != null) {
                totalEarned = totalEarned.add(payout.getAmount());
            }
        }

        model.addAttribute("pendingEarnings", pendingEarnings);
        model.addAttribute("payouts", payouts);
        model.addAttribute("totalEarned", totalEarned);
        model.addAttribute("pageTitle", "AD-X - My Earnings");
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

            // Create notification
            notificationService.createNotification(user,
                    "Payout Request Submitted",
                    "Your payout request of $" + amount + " has been submitted and is pending processing.",
                    "PAYOUT",
                    payout.getId(),
                    "PAYOUT");

            redirectAttributes.addFlashAttribute("success", "Payout request submitted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit payout request: " + e.getMessage());
        }

        return "redirect:/payment/earnings";
    }

    // Admin payout management
    @GetMapping("/payouts/manage")
    public String managePayouts(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<Payout> payouts = payoutService.getPayoutsByStatus("PENDING");
        model.addAttribute("payouts", payouts);
        model.addAttribute("pageTitle", "AD-X - Manage Payouts");
        return "admin-manage-payouts";
    }

    // Process payout (admin)
    @PostMapping("/payouts/process/{payoutId}")
    public String processPayout(@PathVariable Long payoutId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }

        try {
            boolean processed = payoutService.processPayout(payoutId);
            if (processed) {
                Optional<Payout> payoutOpt = payoutService.getPayoutById(payoutId);
                if (payoutOpt.isPresent()) {
                    Payout payout = payoutOpt.get();

                    // Create notification for seller
                    notificationService.createNotification(payout.getSeller(),
                            "Payout Processed",
                            "Your payout of $" + payout.getAmount() + " has been processed successfully.",
                            "PAYOUT",
                            payout.getId(),
                            "PAYOUT");
                }

                redirectAttributes.addFlashAttribute("success", "Payout processed successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to process payout.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing payout: " + e.getMessage());
        }

        return "redirect:/payment/payouts/manage";
    }
}