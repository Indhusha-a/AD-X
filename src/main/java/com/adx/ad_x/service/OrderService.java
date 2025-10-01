package com.adx.ad_x.service;

import com.adx.ad_x.model.Order;
import com.adx.ad_x.model.OrderItem;
import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.User;
import com.adx.ad_x.model.Payment;
import com.adx.ad_x.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class OrderService {

    @Autowired
    @SuppressWarnings("unused") // Used in createSingleOrder
    private ProductService productService;

    @Autowired
    private OrderRepository orderRepository;

    // Existing methods...
    public Order createSingleOrder(User buyer, Product product) {
        Order order = new Order();
        order.setBuyer(buyer);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(1);
        orderItem.setPrice(product.getPrice());
        orderItem.setOrder(order);

        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);
        order.setItems(items);
        order.setTotalAmount(product.getPrice());

        return orderRepository.save(order);
    }

    public List<Order> getUserOrders(User buyer) {
        return orderRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    public Long getUserOrderCount(User buyer) {
        return orderRepository.countByBuyer(buyer);
    }

    @SuppressWarnings("unused") // Called from BuyerController
    public boolean cancelOrder(Long orderId, User buyer) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.getBuyer().getId().equals(buyer.getId()) &&
                    "PENDING".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                orderRepository.save(order);
                return true;
            }
        }
        return false;
    }

    // Seller-specific methods
    public List<Order> getOrdersBySeller(User seller) {
        return orderRepository.findAll().stream()
                .filter(order -> Optional.ofNullable(order.getItems()).orElse(new ArrayList<>()).stream()
                        .anyMatch(item -> Optional.ofNullable(item.getProduct())
                                .map(p -> p.getSeller().getId().equals(seller.getId())).orElse(false)))
                .collect(Collectors.toList());
    }

    public List<Order> getRecentOrdersBySeller(User seller, int limit) {
        return getOrdersBySeller(seller).stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Long getOrderCountBySeller(User seller) {
        return (long) getOrdersBySeller(seller).size();
    }

    public BigDecimal getTotalRevenueBySeller(User seller) {
        return getOrdersBySeller(seller).stream()
                .filter(order -> "CONFIRMED".equals(order.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @SuppressWarnings("unused") // Called from SellerController
    public boolean updateOrderStatus(Long orderId, String status, User seller) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Verify seller owns product with null-safety
            boolean sellerOwnsProduct = Optional.ofNullable(order.getItems())
                    .orElse(new ArrayList<>()).stream()
                    .anyMatch(item -> Optional.ofNullable(item.getProduct())
                            .map(p -> p.getSeller().getId().equals(seller.getId())).orElse(false));

            if (sellerOwnsProduct) {
                order.setStatus(status);
                orderRepository.save(order);
                return true;
            }
        }
        return false;
    }

    // NEW METHODS ADDED FOR PAYMENT INTEGRATION
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    @SuppressWarnings("unused") // Internal use
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // Helper for review eligibility
    @SuppressWarnings("unused") // Called from ReviewService
    public List<OrderItem> getPurchasedItemsForProduct(User buyer, Product product) {
        List<Order> orders = orderRepository.findByBuyer(buyer);
        Stream<Order> orderStream = orders.stream(); // Explicit Stream<Order>
        return orderStream
                .filter(order -> Optional.ofNullable(order.getPayment())
                        .map(payment -> "COMPLETED".equals(payment.getStatus()))
                        .orElse(false))
                .flatMap(order -> Optional.ofNullable(order.getItems())
                        .orElse(new ArrayList<>()).stream())
                .filter(item -> Optional.ofNullable(item.getProduct())
                        .map(p -> p.getId().equals(product.getId()))
                        .orElse(false))
                .collect(Collectors.toList());
    }
}