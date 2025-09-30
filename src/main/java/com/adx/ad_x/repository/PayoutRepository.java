package com.adx.ad_x.repository;

import com.adx.ad_x.model.Payout;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    // Find payouts by seller
    List<Payout> findBySellerOrderByCreatedAtDesc(User seller);

    // Find payouts by status
    List<Payout> findByStatusOrderByCreatedAtDesc(String status);

    // Find pending payouts for a seller
    List<Payout> findBySellerAndStatus(User seller, String status);

    // Calculate total payouts for a seller
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payout p WHERE p.seller = :seller AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalPayoutsBySeller(@Param("seller") User seller);

    // Calculate pending earnings for a seller (completed payments minus completed payouts)
    @Query("SELECT COALESCE(SUM(p.sellerEarnings), 0) - COALESCE((SELECT SUM(po.amount) FROM Payout po WHERE po.seller = :seller AND po.status = 'COMPLETED'), 0) " +
            "FROM Payment p JOIN p.order o JOIN o.items i WHERE i.product.seller = :seller AND p.status = 'COMPLETED'")
    BigDecimal calculatePendingEarnings(@Param("seller") User seller);

    // Find payouts by date range
    @Query("SELECT p FROM Payout p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payout> findByDateRange(@Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
}