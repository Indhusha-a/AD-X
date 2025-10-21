package com.adx.ad_x.designpatterns.observer;

import com.adx.ad_x.model.Order;

/**
 * DESIGN PATTERN 4: OBSERVER PATTERN
 * 
 * Purpose: Defines a one-to-many dependency where observers are notified of state changes
 * Usage: Notifies observers when order events occur (created, confirmed, cancelled)
 * 
 * Benefits: Loose coupling between order system and notification/analytics systems
 */
public interface OrderObserver {
    
    /**
     * Called when an order event occurs
     */
    void onOrderEvent(String eventType, Order order);
    
    /**
     * Get observer name
     */
    String getObserverName();
}
