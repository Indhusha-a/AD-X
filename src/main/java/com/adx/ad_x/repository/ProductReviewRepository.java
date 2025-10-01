package com.adx.ad_x.repository;

import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.ProductReview;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductAndIsActiveTrueOrderByCreatedAtDesc(Product product);

    @Query("SELECT AVG(pr.rating) FROM ProductReview pr WHERE pr.product = :product AND pr.status = 'APPROVED' AND pr.isActive = true")
    Double findAverageRatingByProduct(@Param("product") Product product, String approved);

    Long countByProductAndStatusAndIsActiveTrue(Product product, String status);

    List<ProductReview> findByBuyerAndProductAndIsActiveTrue(User buyer, Product product);

    // For analytics: Recent reviews
    @Query("SELECT pr FROM ProductReview pr WHERE pr.product.seller = :seller AND pr.isActive = true AND pr.status = 'APPROVED' ORDER BY pr.createdAt DESC LIMIT :limit")
    List<ProductReview> findRecentBySeller(@Param("seller") User seller, @Param("limit") int limit);

    @Query("SELECT r FROM ProductReview r WHERE r.status = :status AND r.isActive = true")
    List<ProductReview> findByStatusAndIsActiveTrue(@Param("status") String status);

    Optional<ProductReview> findByIdAndBuyer(Long id, User buyer);
}