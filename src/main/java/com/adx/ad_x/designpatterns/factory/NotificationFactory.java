package com.adx.ad_x.designpatterns.factory;

import com.adx.ad_x.model.Notification;
import com.adx.ad_x.model.User;
import org.springframework.stereotype.Component;

/**
 * DESIGN PATTERN 2: FACTORY PATTERN
 * 
 * Purpose: Creates different types of notification objects without exposing creation logic
 * Usage: Generates notifications for orders, payments, inquiries, and system messages
 * 
 * Benefits: Centralizes object creation, easy to extend with new notification types
 */
@Component
public class NotificationFactory {

    /**
     * Factory method to create appropriate notification based on type
     */
    public Notification createNotification(String type, User user, String title, String message,
                                          Long relatedEntityId, String relatedEntityType) {
        
        switch (type.toUpperCase()) {
            case "ORDER":
                return createOrderNotification(user, title, message, relatedEntityId, relatedEntityType);
                
            case "PAYMENT":
                return createPaymentNotification(user, title, message, relatedEntityId, relatedEntityType);
                
            case "INQUIRY":
                return createInquiryNotification(user, title, message, relatedEntityId, relatedEntityType);
                
            case "SYSTEM":
                return createSystemNotification(user, title, message, relatedEntityId, relatedEntityType);
                
            default:
                return createGeneralNotification(user, title, message, relatedEntityId, relatedEntityType);
        }
    }

    private Notification createOrderNotification(User user, String title, String message,
                                                 Long relatedEntityId, String relatedEntityType) {
        return new Notification(user, title, message, "ORDER", relatedEntityId, relatedEntityType);
    }

    private Notification createPaymentNotification(User user, String title, String message,
                                                   Long relatedEntityId, String relatedEntityType) {
        return new Notification(user, title, message, "PAYMENT", relatedEntityId, relatedEntityType);
    }

    private Notification createInquiryNotification(User user, String title, String message,
                                                   Long relatedEntityId, String relatedEntityType) {
        return new Notification(user, title, message, "INQUIRY", relatedEntityId, relatedEntityType);
    }

    private Notification createSystemNotification(User user, String title, String message,
                                                  Long relatedEntityId, String relatedEntityType) {
        return new Notification(user, title, message, "SYSTEM", relatedEntityId, relatedEntityType);
    }

    private Notification createGeneralNotification(User user, String title, String message,
                                                   Long relatedEntityId, String relatedEntityType) {
        return new Notification(user, title, message, "GENERAL", relatedEntityId, relatedEntityType);
    }
}
