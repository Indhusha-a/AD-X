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

    // Existing Buyer Methods
    public Inquiry createInquiry(User buyer, User seller, Product product, String message) {
        Inquiry inquiry = new Inquiry(buyer, seller, product, message);
        return inquiryRepository.save(inquiry);
    }

    public List<Inquiry> getBuyerInquiries(User buyer) {
        return inquiryRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    public Long getUnreadInquiryCountForBuyer(User buyer) {
        return inquiryRepository.countByBuyerAndIsReadFalse(buyer);
    }

    public Optional<Inquiry> getInquiryById(Long id) {
        return inquiryRepository.findById(id);
    }

    // Seller-specific methods (Step 7)
    public List<Inquiry> getSellerInquiries(User seller) {
        return inquiryRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    public Long getUnreadInquiryCountForSeller(User seller) {
        return inquiryRepository.countBySellerAndIsReadFalse(seller);
    }

    public boolean markInquiryAsRead(Long inquiryId, User seller) {
        Optional<Inquiry> inquiryOpt = inquiryRepository.findById(inquiryId);
        if (inquiryOpt.isPresent()) {
            Inquiry inquiry = inquiryOpt.get();
            if (inquiry.getSeller().getId().equals(seller.getId())) {
                inquiry.setIsRead(true);
                inquiryRepository.save(inquiry);
                return true;
            }
        }
        return false;
    }

    public Inquiry createInquiryResponse(Long inquiryId, String response, User seller) {
        Optional<Inquiry> inquiryOpt = inquiryRepository.findById(inquiryId);
        if (inquiryOpt.isPresent()) {
            Inquiry originalInquiry = inquiryOpt.get();

            if (originalInquiry.getSeller().getId().equals(seller.getId())) {
                // Create a new inquiry as a response
                Inquiry responseInquiry = new Inquiry();
                responseInquiry.setSeller(seller);
                responseInquiry.setBuyer(originalInquiry.getBuyer());
                responseInquiry.setProduct(originalInquiry.getProduct());
                responseInquiry.setMessage(response);
                responseInquiry.setIsRead(false);

                // Mark original as read
                originalInquiry.setIsRead(true);
                inquiryRepository.save(originalInquiry);

                return inquiryRepository.save(responseInquiry);
            }
        }
        return null;
    }

    public void createInquiry(Inquiry inquiry) {
    }

    public void clone(Inquiry inquiry, String response, User seller) {
    }
}