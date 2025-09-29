package com.adx.ad_x.service;

import com.adx.ad_x.model.Order;
import com.adx.ad_x.model.OrderItem;
import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    public List<Order> getUserOrders(User buyer) {
        return orderRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order createOrder(User buyer, List<Product> products) {
        Order order = new Order();
        order.setBuyer(buyer);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Product product : products) {
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(1);
            item.setPrice(product.getPrice());
            order.addItem(item);

            totalAmount = totalAmount.add(product.getPrice());
        }

        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }

    @Transactional
    public Order createSingleOrder(User buyer, Product product) {
        Order order = new Order();
        order.setBuyer(buyer);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setPrice(product.getPrice());
        order.addItem(item);

        order.setTotalAmount(product.getPrice());
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            return orderRepository.save(order);
        }
        return null;
    }

    @Transactional
    public boolean cancelOrder(Long orderId, User buyer) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent() && orderOpt.get().getBuyer().getId().equals(buyer.getId())) {
            Order order = orderOpt.get();
            if ("PENDING".equals(order.getStatus()) || "CONFIRMED".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                orderRepository.save(order);
                return true;
            }
        }
        return false;
    }

    public Long getUserOrderCount(User buyer) {
        return orderRepository.countByBuyer(buyer);
    }
}