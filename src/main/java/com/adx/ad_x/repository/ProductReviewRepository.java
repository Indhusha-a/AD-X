
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

    // Find reviews by product
    List<ProductReview> findByProductAndActiveTrueOrderByCreatedAtDesc(Product product);

    // Find reviews by seller
    List<ProductReview> findBySellerAndActiveTrueOrderByCreatedAtDesc(User seller);

    // Find reviews by buyer
    List<ProductReview> findByBuyerAndActiveTrueOrderByCreatedAtDesc(User buyer);

    // Find specific review by buyer and product
    Optional<ProductReview> findByBuyerAndProduct(User buyer, Product product);

    // Calculate average rating for a product
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ProductReview r WHERE r.product = :product AND r.active = true")
    Double calculateAverageRatingByProduct(@Param("product") Product product);

    // Count reviews for a product
    Long countByProductAndActiveTrue(Product product);

    // Count reviews for a seller
    Long countBySellerAndActiveTrue(User seller);

    // Get recent reviews with limit
    List<ProductReview> findTop5ByActiveTrueOrderByCreatedAtDesc();

    // Check if buyer has purchased the product (for validation)
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items oi WHERE o.buyer = :buyer AND oi.product = :product AND o.status = 'CONFIRMED'")
    boolean hasBuyerPurchasedProduct(@Param("buyer") User buyer, @Param("product") Product product);
}
