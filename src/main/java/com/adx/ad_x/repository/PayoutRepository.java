package com.adx.ad_x.repository;

import com.adx.ad_x.model.Payout;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    // Find by seller
    List<Payout> findBySellerOrderByCreatedAtDesc(User seller);

    // Find by status
    List<Payout> findByStatus(String status);

    // Find by seller and status
    List<Payout> findBySellerAndStatus(User seller, String status);

    // Find all ordered by date
    List<Payout> findAllByOrderByCreatedAtDesc();

    // Calculate total pending payouts
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payout p WHERE p.status = 'PENDING'")
    BigDecimal calculateTotalPendingPayouts();
}