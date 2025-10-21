package com.adx.ad_x.service;

import com.adx.ad_x.model.*;
import com.adx.ad_x.repository.OrderRepository;
import com.adx.ad_x.repository.ProductRepository;
import com.adx.ad_x.repository.UserRepository;
import com.adx.ad_x.designpatterns.observer.OrderEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderEventPublisher orderEventPublisher; // DESIGN PATTERN: Observer Pattern

    // Create a single product order
    public Order createSingleOrder(User buyer, Product product) {
        Order order = new Order();
        order.setBuyer(buyer);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        // Create order item
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(1);
        orderItem.setPrice(product.getPrice());
        orderItem.setOrder(order);

        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);
        order.setItems(items);

        // Calculate total amount
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        // DESIGN PATTERN: Observer Pattern - notify observers of order creation
        orderEventPublisher.notifyObservers("ORDER_CREATED", savedOrder);

        // Create notification for buyer
        notificationService.createOrderNotificationForBuyer(
                savedOrder,
                "Order Placed",
                "Your order #" + savedOrder.getId() + " has been placed successfully."
        );

        // Create notification for seller
        notificationService.createOrderNotificationForSeller(
                savedOrder,
                "New Order Received",
                "You have received a new order #" + savedOrder.getId() + " for " + product.getTitle()
        );

        return savedOrder;
    }

    // Get order by ID
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public Long getTotalOrderCount() {
        return orderRepository.count();
    }

    // Get user orders
    public List<Order> getUserOrders(User buyer) {
        return orderRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    // Get user order count
    public Long getUserOrderCount(User buyer) {
        return orderRepository.countByBuyer(buyer);
    }

    // Get orders by seller
    public List<Order> getOrdersBySeller(User seller) {
        List<Order> allOrders = orderRepository.findAll();
        List<Order> sellerOrders = new ArrayList<>();

        for (Order order : allOrders) {
            if (order.containsSellerProducts(seller)) {
                sellerOrders.add(order);
            }
        }

        return sellerOrders;
    }

    // Get order count by seller
    public Long getOrderCountBySeller(User seller) {
        return (long) getOrdersBySeller(seller).size();
    }

    // Get recent orders by seller
    public List<Order> getRecentOrdersBySeller(User seller, int limit) {
        List<Order> sellerOrders = getOrdersBySeller(seller);
        return sellerOrders.stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(limit)
                .toList();
    }

    // Update order status (for sellers with verification)
    public boolean updateOrderStatus(Long orderId, String status, User seller) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Verify seller owns products in this order
            if (!order.containsSellerProducts(seller)) {
                return false;
            }

            order.setStatus(status);
            orderRepository.save(order);

            // DESIGN PATTERN: Observer Pattern - notify observers of status change
            if ("CONFIRMED".equals(status)) {
                orderEventPublisher.notifyObservers("ORDER_CONFIRMED", order);
            } else if ("CANCELLED".equals(status)) {
                orderEventPublisher.notifyObservers("ORDER_CANCELLED", order);
            }

            // Create notification for buyer
            notificationService.createOrderNotificationForBuyer(
                    order,
                    "Order Status Updated",
                    "Your order #" + orderId + " status has been updated to: " + status
            );

            return true;
        }

        return false;
    }

    // NEW: Update order status without seller verification (for system/payment processing)
    public boolean updateOrderStatus(Long orderId, String status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            orderRepository.save(order);

            // DESIGN PATTERN: Observer Pattern - notify observers of status change
            if ("CONFIRMED".equals(status)) {
                orderEventPublisher.notifyObservers("ORDER_CONFIRMED", order);
            } else if ("COMPLETED".equals(status)) {
                orderEventPublisher.notifyObservers("ORDER_COMPLETED", order);
            } else if ("CANCELLED".equals(status)) {
                orderEventPublisher.notifyObservers("ORDER_CANCELLED", order);
            }

            // Create notification for buyer
            notificationService.createOrderNotificationForBuyer(
                    order,
                    "Order Status Updated",
                    "Your order #" + orderId + " status has been updated to: " + status
            );

            // Also notify seller if status is confirmed/completed
            if ("CONFIRMED".equals(status) || "COMPLETED".equals(status)) {
                notificationService.createOrderNotificationForSeller(
                        order,
                        "Order Status Updated",
                        "Order #" + orderId + " status has been updated to: " + status
                );
            }

            return true;
        }

        return false;
    }

    // Cancel order
    public boolean cancelOrder(Long orderId, User buyer) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Verify ownership
            if (!order.getBuyer().getId().equals(buyer.getId())) {
                return false;
            }

            // Only allow cancellation of pending orders
            if (!"PENDING".equals(order.getStatus())) {
                return false;
            }

            order.setStatus("CANCELLED");
            orderRepository.save(order);

            // DESIGN PATTERN: Observer Pattern - notify observers
            orderEventPublisher.notifyObservers("ORDER_CANCELLED", order);

            notificationService.createOrderNotificationForBuyer(
                    order,
                    "Order Cancelled",
                    "Your order #" + orderId + " has been cancelled successfully."
            );

            // Notify seller about cancellation
            notificationService.createOrderNotificationForSeller(
                    order,
                    "Order Cancelled",
                    "Order #" + orderId + " has been cancelled by the buyer."
            );

            return true;
        }

        return false;
    }

    // Get total revenue by seller
    public BigDecimal getTotalRevenueBySeller(User seller) {
        List<Order> sellerOrders = getOrdersBySeller(seller);
        return sellerOrders.stream()
                .filter(order -> "CONFIRMED".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Get orders by status for buyer
    public List<Order> getUserOrdersByStatus(User buyer, String status) {
        return orderRepository.findByBuyerAndStatus(buyer, status);
    }

    // Get all orders (for admin)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get orders by status (for admin)
    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }
}