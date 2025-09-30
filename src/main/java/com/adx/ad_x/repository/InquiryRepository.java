package com.adx.ad_x.repository;

import com.adx.ad_x.model.Inquiry;
import com.adx.ad_x.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // Buyer methods
    List<Inquiry> findByBuyerOrderByCreatedAtDesc(User buyer);
    Long countByBuyerAndIsReadFalse(User buyer);

    // Seller methods (Step 7)
    List<Inquiry> findBySellerOrderByCreatedAtDesc(User seller);
    Long countBySellerAndIsReadFalse(User seller);
}