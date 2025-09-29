package com.adx.ad_x.repository;

import com.adx.ad_x.model.Favorite;
import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // Find all favorites by user
    List<Favorite> findByUser(User user);

    // Find favorite by user and product
    Optional<Favorite> findByUserAndProduct(User user, Product product);

    // Check if product is favorited by user
    boolean existsByUserAndProduct(User user, Product product);

    // Count favorites by user
    Long countByUser(User user);

    // Delete favorite by user and product
    void deleteByUserAndProduct(User user, Product product);
}