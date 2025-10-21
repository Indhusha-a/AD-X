package com.adx.ad_x.designpatterns.observer;

import com.adx.ad_x.model.Order;
import org.springframework.stereotype.Component;

/**
 * Concrete Observer: Tracks analytics when order events occur
 */
@Component
public class AnalyticsObserver implements OrderObserver {

    @Override
    public void onOrderEvent(String eventType, Order order) {
        switch (eventType) {
            case "ORDER_CREATED":
                System.out.println("[ANALYTICS] Recording new order - Amount: $" + order.getTotalAmount());
                break;
            case "ORDER_CONFIRMED":
                System.out.println("[ANALYTICS] Order confirmed - Revenue: $" + order.getTotalAmount());
                break;
            case "ORDER_CANCELLED":
                System.out.println("[ANALYTICS] Order cancelled - Lost revenue: $" + order.getTotalAmount());
                break;
            default:
                System.out.println("[ANALYTICS] Order event tracked: " + eventType);
        }
    }

    @Override
    public String getObserverName() {
        return "AnalyticsObserver";
    }
}
