package com.adx.ad_x.repository;

import com.adx.ad_x.model.Payment;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by buyer
    List<Payment> findByBuyerOrderByCreatedAtDesc(User buyer);

    // Find by status
    List<Payment> findByStatus(String status);

    // Find by date range and status
    List<Payment> findByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, String status);

    // Find by date range
    List<Payment> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    // Find all ordered by date
    List<Payment> findAllByOrderByCreatedAtDesc();

    // Calculate total revenue
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal calculateTotalRevenue();

    // Calculate total revenue by date range
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.createdAt BETWEEN :start AND :end AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalRevenueByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Calculate total commission
    @Query("SELECT COALESCE(SUM(p.amount * 0.10), 0) FROM Payment p WHERE p.createdAt BETWEEN :start AND :end AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalCommission(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Find completed payments by seller - FIXED: Return List<Payment> instead of Object[]
   

    // In PaymentRepository.java - KEEP THIS METHOD AS IS:
    @Query("SELECT p.amount FROM Payment p JOIN p.order o JOIN o.items oi WHERE oi.product.seller = :seller AND p.status = 'COMPLETED'")
    List<Object[]> findCompletedPaymentsBySeller(@Param("seller") User seller);
}