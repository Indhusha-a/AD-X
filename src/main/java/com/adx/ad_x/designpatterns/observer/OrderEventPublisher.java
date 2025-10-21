package com.adx.ad_x.designpatterns.observer;

import com.adx.ad_x.model.Order;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Subject/Observable: Manages observers and notifies them of order events
 */
@Component
public class OrderEventPublisher {

    private final List<OrderObserver> observers = new ArrayList<>();

    public void registerObserver(OrderObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("Observer registered: " + observer.getObserverName());
        }
    }

    public void removeObserver(OrderObserver observer) {
        observers.remove(observer);
        System.out.println("Observer removed: " + observer.getObserverName());
    }

    public void notifyObservers(String eventType, Order order) {
        System.out.println("Notifying " + observers.size() + " observers of event: " + eventType);
        for (OrderObserver observer : observers) {
            observer.onOrderEvent(eventType, order);
        }
    }
}
