package com.adx.ad_x.repository;

import com.adx.ad_x.model.Order;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find orders by buyer
    List<Order> findByBuyerOrderByCreatedAtDesc(User buyer);

    // Find orders by buyer and status
    List<Order> findByBuyerAndStatus(User buyer, String status);

    // Count orders by buyer
    Long countByBuyer(User buyer);

    // Find all orders ordered by creation date
    List<Order> findAllByOrderByCreatedAtDesc();

    // Find orders by status ordered by creation date
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
}