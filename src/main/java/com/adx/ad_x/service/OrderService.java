package com.adx.ad_x.service;

import com.adx.ad_x.model.Order;
import com.adx.ad_x.model.OrderItem;
import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

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

    // Seller-specific methods (Step 6)
    public List<Order> getOrdersBySeller(User seller) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getItems().stream()
                        .anyMatch(item -> item.getProduct().getSeller().getId().equals(seller.getId())))
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

    public boolean updateOrderStatus(Long orderId, String status, User seller) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Verify that the seller owns at least one product in this order
            boolean sellerOwnsProduct = order.getItems().stream()
                    .anyMatch(item -> item.getProduct().getSeller().getId().equals(seller.getId()));

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

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}