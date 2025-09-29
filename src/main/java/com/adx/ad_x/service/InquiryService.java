package com.adx.ad_x.service;

import com.adx.ad_x.model.Inquiry;
import com.adx.ad_x.model.Product;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.InquiryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InquiryService {

    @Autowired
    private InquiryRepository inquiryRepository;

    public List<Inquiry> getBuyerInquiries(User buyer) {
        return inquiryRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    public List<Inquiry> getSellerInquiries(User seller) {
        return inquiryRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    public Inquiry createInquiry(User buyer, User seller, Product product, String message) {
        Inquiry inquiry = new Inquiry(buyer, seller, product, message);
        return inquiryRepository.save(inquiry);
    }

    public Optional<Inquiry> getInquiryById(Long id) {
        return inquiryRepository.findById(id);
    }

    public Inquiry markAsRead(Long inquiryId) {
        Optional<Inquiry> inquiry = inquiryRepository.findById(inquiryId);
        if (inquiry.isPresent()) {
            Inquiry existingInquiry = inquiry.get();
            existingInquiry.setIsRead(true);
            return inquiryRepository.save(existingInquiry);
        }
        return null;
    }

    public Long getUnreadInquiryCountForBuyer(User buyer) {
        return inquiryRepository.countByBuyerAndIsReadFalse(buyer);
    }

    public Long getUnreadInquiryCountForSeller(User seller) {
        return inquiryRepository.countBySellerAndIsReadFalse(seller);
    }
}