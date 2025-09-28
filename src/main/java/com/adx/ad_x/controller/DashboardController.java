package com.adx.ad_x.controller;

import com.adx.ad_x.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Redirect based on user role
        if ("BUYER".equals(user.getRole())) {
            return "redirect:/buyer/dashboard";
        } else if ("SELLER".equals(user.getRole())) {
            return "redirect:/seller/dashboard";
        } else if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        } else {
            // Fallback for unknown roles - show generic dashboard
            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "AD-X - Dashboard");
            return "dashboard";
        }
    }

    @GetMapping("/buyer/dashboard")
    public String buyerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Allow only BUYER role to access buyer dashboard
        if (!"BUYER".equals(user.getRole())) {
            return "redirect:/dashboard"; // Redirect to role-appropriate dashboard
        }

        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "AD-X - Buyer Dashboard");
        return "buyer-dashboard";
    }

    @GetMapping("/seller/dashboard")
    public String sellerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // Allow only SELLER role to access seller dashboard
        if (!"SELLER".equals(user.getRole())) {
            return "redirect:/dashboard"; // Redirect to role-appropriate dashboard
        }

        // For now, we'll set productCount to 0 since we don't have ProductService in IAM
        // This will be updated when we implement the Seller Interface
        Long productCount = 0L;

        model.addAttribute("user", user);
        model.addAttribute("productCount", productCount);
        model.addAttribute("pageTitle", "AD-X - Seller Dashboard");
        return "seller-dashboard";
    }

    // Generic dashboard for any role (fallback)
    @GetMapping("/generic-dashboard")
    public String genericDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("pageTitle", "AD-X - Dashboard");
        return "dashboard";
    }
}