// NotificationService.java
package com.adx.ad_x.service;

import com.adx.ad_x.model.Notification;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    // CREATE: Create a new notification
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification(user, title, message, type);
        return notificationRepository.save(notification);
    }

    public Notification createNotification(User user, String title, String message, String type,
                                           Long relatedEntityId, String relatedEntityType) {
        Notification notification = new Notification(user, title, message, type, relatedEntityId, relatedEntityType);
        return notificationRepository.save(notification);
    }

    // READ: Get all notifications for user
    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // READ: Get unread notifications for user
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    // READ: Get unread notification count
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    // UPDATE: Mark all notifications as read
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    // UPDATE: Mark single notification as read
    public boolean markAsRead(Long notificationId, User user) {
        int updated = notificationRepository.markAsRead(notificationId, user);
        return updated > 0;
    }

    // DELETE: Clear all read notifications
    public void clearReadNotifications(User user) {
        notificationRepository.deleteAllRead(user);
    }

    // DELETE: Delete specific notification
    public boolean deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getUser().getId().equals(user.getId())) {
            notificationRepository.delete(notification);
            return true;
        }
        return false;
    }

    // CREATE: Helper methods for common notification types
    public void notifyOrderPlaced(User user, Long orderId) {
        createNotification(user,
                "Order Placed Successfully",
                "Your order #" + orderId + " has been placed successfully.",
                "ORDER", orderId, "ORDER");
    }

    public void notifyPaymentReceived(User user, Long paymentId, String amount) {
        createNotification(user,
                "Payment Received",
                "Payment of $" + amount + " has been received successfully.",
                "PAYMENT", paymentId, "PAYMENT");
    }

    public void notifyOrderStatusUpdate(User user, Long orderId, String status) {
        createNotification(user,
                "Order Status Updated",
                "Your order #" + orderId + " status has been updated to: " + status,
                "ORDER", orderId, "ORDER");
    }

    public void notifyNewInquiry(User user, Long inquiryId) {
        if ("SELLER".equals(user.getRole())) {
            createNotification(user,
                    "New Customer Inquiry",
                    "You have received a new inquiry from a customer.",
                    "INQUIRY", inquiryId, "INQUIRY");
        }
    }

    
}