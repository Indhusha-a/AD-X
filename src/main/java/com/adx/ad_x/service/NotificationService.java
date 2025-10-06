package com.adx.ad_x.service;

import com.adx.ad_x.model.*;
import com.adx.ad_x.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ADD THIS IMPORT

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    // Create a notification
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification(user, title, message, type);
        return notificationRepository.save(notification);
    }

    // Create a notification with related entity
    public Notification createNotification(User user, String title, String message, String type,
                                           Long relatedEntityId, String relatedEntityType) {
        Notification notification = new Notification(user, title, message, type, relatedEntityId, relatedEntityType);
        return notificationRepository.save(notification);
    }

    // Get user notifications
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserAndActiveTrueOrderByCreatedAtDesc(user);
    }

    // Get unread notifications
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseAndActiveTrueOrderByCreatedAtDesc(user);
    }

    // Get unread notification count
    public Long getUnreadNotificationCount(User user) {
        return notificationRepository.countByUserAndIsReadFalseAndActiveTrue(user);
    }

    // Mark notification as read
    public boolean markAsRead(Long notificationId, User user) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    if (notification.getUser().getId().equals(user.getId())) {
                        notification.setIsRead(true);
                        notificationRepository.save(notification);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    // Mark all notifications as read - FIXED: Added @Transactional
    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    // Delete notification (soft delete)
    public boolean deleteNotification(Long notificationId, User user) {
        return notificationRepository.findById(notificationId)
                .map(notification -> {
                    if (notification.getUser().getId().equals(user.getId())) {
                        notification.setActive(false);
                        notificationRepository.save(notification);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    // Create order notification for buyer
    public void createOrderNotificationForBuyer(Order order, String title, String message) {
        createNotification(order.getBuyer(), title, message, "ORDER", order.getId(), "ORDER");
    }

    // Create order notification for seller
    public void createOrderNotificationForSeller(Order order, String title, String message) {
        // Get seller from order items (assuming single seller per order for simplicity)
        if (!order.getItems().isEmpty()) {
            User seller = order.getItems().get(0).getProduct().getSeller();
            createNotification(seller, title, message, "ORDER", order.getId(), "ORDER");
        }
    }

    // Create payment notification
    public void createPaymentNotification(Payment payment, String title, String message) {
        createNotification(payment.getBuyer(), title, message, "PAYMENT", payment.getId(), "PAYMENT");

        // Also notify seller about payment
        if (!payment.getOrder().getItems().isEmpty()) {
            User seller = payment.getOrder().getItems().get(0).getProduct().getSeller();
            createNotification(seller, "Payment Received",
                    "Payment completed for order #" + payment.getOrder().getId(),
                    "PAYMENT", payment.getId(), "PAYMENT");
        }
    }

    // Create inquiry notification - SIMPLIFIED VERSION (no parent inquiry check)
    public void createInquiryNotification(Inquiry inquiry, String title, String message) {
        // Always notify seller for new inquiries
        createNotification(inquiry.getSeller(), title, message, "INQUIRY", inquiry.getId(), "INQUIRY");
    }

    // Create inquiry response notification for buyer
    public void createInquiryResponseNotification(Inquiry inquiry, String title, String message) {
        createNotification(inquiry.getBuyer(), title, message, "INQUIRY", inquiry.getId(), "INQUIRY");
    }
}