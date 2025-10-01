// NotificationController.java
package com.adx.ad_x.controller;

import com.adx.ad_x.model.Notification;
import com.adx.ad_x.model.User;
import com.adx.ad_x.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    private boolean isAuthenticated(HttpSession session) {
        return session.getAttribute("user") != null;
    }

    // READ: View all notifications
    @GetMapping
    public String viewNotifications(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        User user = (User) session.getAttribute("user");
        List<Notification> notifications = notificationService.getUserNotifications(user);
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("pageTitle", "AD-X - Notifications");
        return "notifications";
    }

    // UPDATE: Mark all notifications as read
    @PostMapping("/mark-all-read")
    public String markAllAsRead(HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        User user = (User) session.getAttribute("user");
        notificationService.markAllAsRead(user);

        return "redirect:/notifications";
    }

    // UPDATE: Mark single notification as read
    @PostMapping("/mark-read/{id}")
    public String markAsRead(@PathVariable Long id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        User user = (User) session.getAttribute("user");
        notificationService.markAsRead(id, user);

        return "redirect:/notifications";
    }

    // DELETE: Clear all read notifications
    @PostMapping("/clear-read")
    public String clearReadNotifications(HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        User user = (User) session.getAttribute("user");
        notificationService.clearReadNotifications(user);

        return "redirect:/notifications";
    }

    // DELETE: Delete specific notification
    @PostMapping("/delete/{id}")
    public String deleteNotification(@PathVariable Long id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/login";
        }

        User user = (User) session.getAttribute("user");
        notificationService.deleteNotification(id, user);

        return "redirect:/notifications";
    }

    // AJAX endpoint to get unread count (for navbar updates)
    @GetMapping("/unread-count")
    @ResponseBody
    public long getUnreadCount(HttpSession session) {
        if (!isAuthenticated(session)) {
            return 0;
        }
        User user = (User) session.getAttribute("user");
        return notificationService.getUnreadCount(user);
    }
}