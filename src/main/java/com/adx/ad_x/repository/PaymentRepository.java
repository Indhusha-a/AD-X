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

    // Find payments by buyer
    List<Payment> findByBuyerOrderByCreatedAtDesc(User buyer);

    // Find payments by status
    List<Payment> findByStatusOrderByCreatedAtDesc(String status);

    // Count payments by status
    Long countByStatus(String status);

    // Find payments by date range
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    // Calculate total revenue in date range
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalRevenue(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // Calculate total commission in date range
    @Query("SELECT COALESCE(SUM(p.commissionAmount), 0) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalCommission(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // NEW: Find completed payments by seller
    @Query("SELECT p FROM Payment p JOIN p.order o JOIN o.items i WHERE i.product.seller = :seller AND p.status = 'COMPLETED' ORDER BY p.createdAt DESC")
    List<Payment> findCompletedPaymentsBySeller(@Param("seller") User seller);

    // NEW: Calculate total earnings for a seller
    @Query("SELECT COALESCE(SUM(p.sellerEarnings), 0) FROM Payment p JOIN p.order o JOIN o.items i WHERE i.product.seller = :seller AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalEarningsBySeller(@Param("seller") User seller);
}