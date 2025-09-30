package com.adx.ad_x.repository;

import com.adx.ad_x.model.SellerProfile;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {
    Optional<SellerProfile> findByUser(User user);
    Optional<SellerProfile> findByUserId(Long userId);
    boolean existsByUser(User user);
}