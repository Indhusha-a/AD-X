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
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find all active products
    List<Product> findByActiveTrue();

    // Find products by category
    List<Product> findByCategoryAndActiveTrue(String category);

    // Find products by seller
    List<Product> findBySellerAndActiveTrue(User seller);

    // Find products by seller ID
    List<Product> findBySellerIdAndActiveTrue(Long sellerId);

    // Search products by title
    List<Product> findByTitleContainingIgnoreCaseAndActiveTrue(String title);

    // Count products by seller
    Long countBySeller(User seller);

    // Find product by ID and seller (for security) - Step 7
    Optional<Product> findByIdAndSeller(Long id, User seller);

    // Find products by multiple categories
    List<Product> findByCategoryInAndActiveTrue(List<String> categories);

    // Find products by price range
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findByPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);

    @Query("SELECT r FROM ProductReview r WHERE r.status = :status AND r.isActive = true")
    List<ProductReview> findByStatusAndIsActiveTrue(@Param("status") String status);
}