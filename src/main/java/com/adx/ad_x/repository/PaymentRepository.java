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
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payments by buyer
    List<Payment> findByBuyerOrderByCreatedAtDesc(User buyer);

    // Find payments by order
    Optional<Payment> findByOrderId(Long orderId);

    // Find payments by status
    List<Payment> findByStatusOrderByCreatedAtDesc(String status);

    // Find payments by date range
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payment> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    // Find completed payments for a seller
    @Query("SELECT p FROM Payment p JOIN p.order o JOIN o.items i WHERE i.product.seller = :seller AND p.status = 'COMPLETED'")
    List<Payment> findCompletedPaymentsBySeller(@Param("seller") User seller);

    // Calculate total revenue for platform
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalRevenue(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // Calculate total commission for platform
    @Query("SELECT COALESCE(SUM(p.commissionAmount), 0) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalCommission(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Count payments by status
    Long countByStatus(String status);
}