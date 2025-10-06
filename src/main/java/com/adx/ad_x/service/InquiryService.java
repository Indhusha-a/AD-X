package com.adx.ad_x.service;

import com.adx.ad_x.model.*;
import com.adx.ad_x.repository.InquiryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InquiryService {

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private NotificationService notificationService;

    // Create a new inquiry
    public Inquiry createInquiry(User buyer, User seller, Product product, String message) {
        Inquiry inquiry = new Inquiry();
        inquiry.setBuyer(buyer);
        inquiry.setSeller(seller);
        inquiry.setProduct(product);
        inquiry.setMessage(message);
        inquiry.setIsRead(false);
        inquiry.setCreatedAt(LocalDateTime.now());

        Inquiry savedInquiry = inquiryRepository.save(inquiry);

        // Create notification for seller
        notificationService.createInquiryNotification(
                savedInquiry,
                "New Customer Inquiry",
                buyer.getFirstName() + " sent you a message about: " + product.getTitle()
        );

        return savedInquiry;
    }

    // Create inquiry response - SIMPLIFIED VERSION
    public Inquiry createInquiryResponse(Long parentInquiryId, String response, User seller) {
        Optional<Inquiry> parentInquiryOpt = inquiryRepository.findById(parentInquiryId);

        if (parentInquiryOpt.isPresent()) {
            Inquiry parentInquiry = parentInquiryOpt.get();

            // Verify seller owns this inquiry
            if (!parentInquiry.getSeller().getId().equals(seller.getId())) {
                throw new IllegalArgumentException("You can only respond to your own inquiries");
            }

            // Create a new inquiry as response (no parent-child relationship)
            Inquiry responseInquiry = new Inquiry();
            responseInquiry.setBuyer(parentInquiry.getBuyer());
            responseInquiry.setSeller(seller);
            responseInquiry.setProduct(parentInquiry.getProduct());
            responseInquiry.setMessage("RESPONSE: " + response);
            responseInquiry.setIsRead(false);
            responseInquiry.setCreatedAt(LocalDateTime.now());

            // Mark parent inquiry as read
            parentInquiry.setIsRead(true);
            inquiryRepository.save(parentInquiry);

            Inquiry savedResponse = inquiryRepository.save(responseInquiry);

            // Create notification for buyer
            notificationService.createInquiryResponseNotification(
                    savedResponse,
                    "Seller Response",
                    "Seller responded to your inquiry about: " + parentInquiry.getProduct().getTitle()
            );

            return savedResponse;
        }

        throw new IllegalArgumentException("Parent inquiry not found");
    }

    // Get inquiry by ID
    public Optional<Inquiry> getInquiryById(Long inquiryId) {
        return inquiryRepository.findById(inquiryId);
    }

    // Get buyer inquiries
    public List<Inquiry> getBuyerInquiries(User buyer) {
        return inquiryRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    // Get seller inquiries
    public List<Inquiry> getSellerInquiries(User seller) {
        return inquiryRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    // Get unread inquiry count for buyer
    public Long getUnreadInquiryCountForBuyer(User buyer) {
        return inquiryRepository.countByBuyerAndIsReadFalse(buyer);
    }

    // Get unread inquiry count for seller
    public Long getUnreadInquiryCountForSeller(User seller) {
        return inquiryRepository.countBySellerAndIsReadFalse(seller);
    }

    // Mark inquiry as read
    public boolean markInquiryAsRead(Long inquiryId, User user) {
        Optional<Inquiry> inquiryOpt = inquiryRepository.findById(inquiryId);

        if (inquiryOpt.isPresent()) {
            Inquiry inquiry = inquiryOpt.get();

            // Verify user has access to this inquiry
            if (!inquiry.getBuyer().getId().equals(user.getId()) &&
                    !inquiry.getSeller().getId().equals(user.getId())) {
                return false;
            }

            inquiry.setIsRead(true);
            inquiryRepository.save(inquiry);
            return true;
        }

        return false;
    }

    // Get inquiry thread - SIMPLIFIED: just get inquiries for the product
    public List<Inquiry> getInquiryThread(Long productId, User buyer, User seller) {
        // Get buyer inquiries for this product and seller
        List<Inquiry> buyerInquiries = inquiryRepository.findByBuyerOrderByCreatedAtDesc(buyer);
        List<Inquiry> sellerInquiries = inquiryRepository.findBySellerOrderByCreatedAtDesc(seller);

        // Combine and filter by product
        List<Inquiry> allInquiries = new ArrayList<>();
        allInquiries.addAll(buyerInquiries);
        allInquiries.addAll(sellerInquiries);

        return allInquiries.stream()
                .filter(inquiry -> inquiry.getProduct().getId().equals(productId))
                .sorted((i1, i2) -> i1.getCreatedAt().compareTo(i2.getCreatedAt()))
                .toList();
    }
}