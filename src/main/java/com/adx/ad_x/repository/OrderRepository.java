package com.adx.ad_x.repository;

import com.adx.ad_x.model.Order;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Buyer methods
    List<Order> findByBuyerOrderByCreatedAtDesc(User buyer);
    List<Order> findByBuyerAndStatus(User buyer, String status);
    Long countByBuyer(User buyer);

    // Fix: Add @Query for findByBuyer to return List<Order>
    @Query("SELECT o FROM Order o WHERE o.buyer = :buyer")
    List<Order> findByBuyer(@Param("buyer") User buyer);

    // Note: Seller-specific order queries are handled in OrderService
    // since they require filtering by product seller relationship
}