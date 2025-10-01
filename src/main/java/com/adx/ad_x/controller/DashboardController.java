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
            model.addAttribute("user", user);
            model.addAttribute("pageTitle", "AD-X - Dashboard");
            return "dashboard";
        }
    }

    // Generic fallback dashboard (optional)
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