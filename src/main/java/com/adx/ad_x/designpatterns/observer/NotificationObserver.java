package com.adx.ad_x.designpatterns.observer;

import com.adx.ad_x.model.Order;
import org.springframework.stereotype.Component;

/**
 * Concrete Observer: Sends notifications when order events occur
 */
@Component
public class NotificationObserver implements OrderObserver {

    @Override
    public void onOrderEvent(String eventType, Order order) {
        switch (eventType) {
            case "ORDER_CREATED":
                System.out.println("[NOTIFICATION] New order created - ID: " + order.getId());
                break;
            case "ORDER_CONFIRMED":
                System.out.println("[NOTIFICATION] Order confirmed - ID: " + order.getId());
                break;
            case "ORDER_CANCELLED":
                System.out.println("[NOTIFICATION] Order cancelled - ID: " + order.getId());
                break;
            default:
                System.out.println("[NOTIFICATION] Order event: " + eventType);
        }
    }

    @Override
    public String getObserverName() {
        return "NotificationObserver";
    }
}
