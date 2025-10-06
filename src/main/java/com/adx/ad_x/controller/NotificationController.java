package com.adx.ad_x.controller;

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

    // View all notifications - FIXED: Add user to model
    @GetMapping
    public String viewNotifications(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<com.adx.ad_x.model.Notification> notifications = notificationService.getUserNotifications(user);
        Long unreadCount = notificationService.getUnreadNotificationCount(user);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("user", user); // ADD THIS LINE - user was missing!
        model.addAttribute("pageTitle", "AD-X - Notifications");
        return "notifications";
    }

    // Mark notification as read
    @PostMapping("/mark-read/{notificationId}")
    public String markAsRead(@PathVariable Long notificationId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        notificationService.markAsRead(notificationId, user);
        return "redirect:/notifications";
    }

    // Mark all notifications as read
    @PostMapping("/mark-all-read")
    public String markAllAsRead(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        notificationService.markAllAsRead(user);
        return "redirect:/notifications";
    }

    // Delete notification
    @PostMapping("/delete/{notificationId}")
    public String deleteNotification(@PathVariable Long notificationId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        notificationService.deleteNotification(notificationId, user);
        return "redirect:/notifications";
    }

    // Clear all read notifications
    @PostMapping("/clear-read")
    public String clearReadNotifications(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<com.adx.ad_x.model.Notification> notifications = notificationService.getUserNotifications(user);
        notifications.stream()
                .filter(com.adx.ad_x.model.Notification::getIsRead)
                .forEach(notification -> notificationService.deleteNotification(notification.getId(), user));

        return "redirect:/notifications";
    }

    // Get unread notifications count (AJAX endpoint)
    @GetMapping("/unread-count")
    @ResponseBody
    public Long getUnreadCount(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return 0L;
        }

        return notificationService.getUnreadNotificationCount(user);
    }
}