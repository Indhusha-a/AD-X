package com.adx.ad_x.service;

import com.adx.ad_x.model.SellerProfile;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.SellerProfileRepository;
import com.adx.ad_x.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SellerProfileService {

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    public SellerProfile getOrCreateSellerProfile(User user) {
        Optional<SellerProfile> existingProfile = sellerProfileRepository.findByUser(user);
        if (existingProfile.isPresent()) {
            return existingProfile.get();
        }

        // Create new profile
        SellerProfile newProfile = new SellerProfile(user);
        return sellerProfileRepository.save(newProfile);
    }

    public SellerProfile updateSellerProfile(User user, SellerProfile profileDetails) {
        SellerProfile profile = getOrCreateSellerProfile(user);

        if (profileDetails.getBusinessName() != null) {
            profile.setBusinessName(profileDetails.getBusinessName());
        }
        if (profileDetails.getBusinessDescription() != null) {
            profile.setBusinessDescription(profileDetails.getBusinessDescription());
        }
        if (profileDetails.getPhoneNumber() != null) {
            profile.setPhoneNumber(profileDetails.getPhoneNumber());
        }
        if (profileDetails.getBusinessAddress() != null) {
            profile.setBusinessAddress(profileDetails.getBusinessAddress());
        }
        if (profileDetails.getWebsiteUrl() != null) {
            profile.setWebsiteUrl(profileDetails.getWebsiteUrl());
        }
        if (profileDetails.getBusinessLogoUrl() != null) {
            profile.setBusinessLogoUrl(profileDetails.getBusinessLogoUrl());
        }

        return sellerProfileRepository.save(profile);
    }

    public Optional<SellerProfile> getSellerProfile(User user) {
        return sellerProfileRepository.findByUser(user);
    }

    public void updateRevenueAndOrders(User seller, Double revenue, boolean incrementOrders) {
        Optional<SellerProfile> profileOpt = sellerProfileRepository.findByUser(seller);
        if (profileOpt.isPresent()) {
            SellerProfile profile = profileOpt.get();

            if (revenue != null) {
                profile.setTotalRevenue(profile.getTotalRevenue().add(BigDecimal.valueOf(revenue)));
            }

            if (incrementOrders) {
                profile.setTotalOrders(profile.getTotalOrders() + 1);
            }

            sellerProfileRepository.save(profile);
        }
    }

    // NEW METHOD: Get all sellers
    public List<User> getAllSellers() {
        return userRepository.findByRole("SELLER");
    }
    public void updateAverageRating(User seller, Double rating) {
        Optional<SellerProfile> profileOpt = sellerProfileRepository.findByUser(seller);
        if (profileOpt.isPresent()) {
            SellerProfile profile = profileOpt.get();
            profile.setAverageRating(BigDecimal.valueOf(rating));
            sellerProfileRepository.save(profile);
        }
    }
}