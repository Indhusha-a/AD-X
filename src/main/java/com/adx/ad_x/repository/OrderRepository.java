package com.adx.ad_x.repository;

import com.adx.ad_x.model.Order;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerOrderByCreatedAtDesc(User buyer);
    List<Order> findByBuyerAndStatus(User buyer, String status);
    Long countByBuyer(User buyer);
}