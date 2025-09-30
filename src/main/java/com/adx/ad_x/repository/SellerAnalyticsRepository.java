package com.adx.ad_x.repository;

import com.adx.ad_x.model.SellerAnalytics;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SellerAnalyticsRepository extends JpaRepository<SellerAnalytics, Long> {
    List<SellerAnalytics> findBySellerOrderByAnalyticsDateDesc(User seller);
    Optional<SellerAnalytics> findBySellerAndAnalyticsDate(User seller, LocalDate date);

    @Query("SELECT sa FROM SellerAnalytics sa WHERE sa.seller = :seller AND sa.analyticsDate BETWEEN :startDate AND :endDate ORDER BY sa.analyticsDate")
    List<SellerAnalytics> findBySellerAndDateRange(@Param("seller") User seller,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
}