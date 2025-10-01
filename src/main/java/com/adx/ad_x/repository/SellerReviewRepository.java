package com.adx.ad_x.repository;

import com.adx.ad_x.model.SellerReview;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerReviewRepository extends JpaRepository<SellerReview, Long> {

    List<SellerReview> findBySellerAndIsActiveTrueOrderByCreatedAtDesc(User seller);

    // Fix: Full @Query with both params and @Param for :status binding
    @Query("SELECT AVG(sr.rating) FROM SellerReview sr WHERE sr.seller = :seller AND sr.status = :status AND sr.isActive = true")
    Double findAverageRatingBySeller(@Param("seller") User seller, @Param("status") String status);

    @Query("SELECT COUNT(sr) FROM SellerReview sr WHERE sr.seller = :seller AND sr.isActive = true AND sr.status = :status")
    Long countBySellerAndIsActiveTrue(@Param("seller") User seller, @Param("status") String status);

    List<SellerReview> findByBuyerAndSellerAndIsActiveTrue(User buyer, User seller);

    Optional<SellerReview> findByIdAndBuyer(Long id, User buyer);

    @Query("SELECT sr FROM SellerReview sr WHERE sr.status = :status AND sr.isActive = true")
    List<SellerReview> findByStatusAndIsActiveTrue(@Param("status") String status);

    @Query("SELECT sr FROM SellerReview sr WHERE sr.seller = :seller AND sr.isActive = true AND sr.status = 'APPROVED' ORDER BY sr.createdAt DESC")
    List<SellerReview> findRecentBySeller(@Param("seller") User seller);
}