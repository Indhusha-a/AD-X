package com.adx.ad_x.service;

import com.adx.ad_x.model.SellerAnalytics;
import com.adx.ad_x.model.User;
import com.adx.ad_x.repository.SellerAnalyticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SellerAnalyticsService {

    @Autowired
    private SellerAnalyticsRepository sellerAnalyticsRepository;

    public SellerAnalytics getOrCreateDailyAnalytics(User seller) {
        LocalDate today = LocalDate.now();
        Optional<SellerAnalytics> existingAnalytics = sellerAnalyticsRepository.findBySellerAndAnalyticsDate(seller, today);

        if (existingAnalytics.isPresent()) {
            return existingAnalytics.get();
        }

        SellerAnalytics newAnalytics = new SellerAnalytics(seller);
        return sellerAnalyticsRepository.save(newAnalytics);
    }

    public void recordProductView(User seller) {
        SellerAnalytics analytics = getOrCreateDailyAnalytics(seller);
        analytics.setProductViews(analytics.getProductViews() + 1);
        analytics.setTotalViews(analytics.getTotalViews() + 1);
        sellerAnalyticsRepository.save(analytics);
    }

    public void recordInquiry(User seller) {
        SellerAnalytics analytics = getOrCreateDailyAnalytics(seller);
        analytics.setInquiriesReceived(analytics.getInquiriesReceived() + 1);
        sellerAnalyticsRepository.save(analytics);
    }

    public void recordOrder(User seller, BigDecimal revenue) {
        SellerAnalytics analytics = getOrCreateDailyAnalytics(seller);
        analytics.setOrdersReceived(analytics.getOrdersReceived() + 1);
        analytics.setRevenueGenerated(analytics.getRevenueGenerated().add(revenue));

        // Calculate conversion rate
        if (analytics.getProductViews() > 0) {
            BigDecimal conversionRate = BigDecimal.valueOf(analytics.getOrdersReceived())
                    .divide(BigDecimal.valueOf(analytics.getProductViews()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            analytics.setConversionRate(conversionRate);
        }

        sellerAnalyticsRepository.save(analytics);
    }

    public List<SellerAnalytics> getSellerAnalytics(User seller, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        LocalDate endDate = LocalDate.now();
        return sellerAnalyticsRepository.findBySellerAndDateRange(seller, startDate, endDate);
    }

    public SellerAnalytics getSellerSummary(User seller) {
        // Get analytics for the last 30 days to calculate summary
        List<SellerAnalytics> recentAnalytics = getSellerAnalytics(seller, 30);

        // If no analytics exist, return a default summary
        if (recentAnalytics.isEmpty()) {
            SellerAnalytics defaultSummary = new SellerAnalytics(seller);
            defaultSummary.setTotalViews(0);
            defaultSummary.setInquiriesReceived(0);
            defaultSummary.setOrdersReceived(0);
            defaultSummary.setRevenueGenerated(BigDecimal.ZERO);
            defaultSummary.setConversionRate(BigDecimal.ZERO);
            return defaultSummary;
        }

        SellerAnalytics summary = new SellerAnalytics(seller);

        int totalViews = 0;
        int totalInquiries = 0;
        int totalOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (SellerAnalytics analytics : recentAnalytics) {
            totalViews += analytics.getTotalViews();
            totalInquiries += analytics.getInquiriesReceived();
            totalOrders += analytics.getOrdersReceived();
            totalRevenue = totalRevenue.add(analytics.getRevenueGenerated());
        }

        summary.setTotalViews(totalViews);
        summary.setInquiriesReceived(totalInquiries);
        summary.setOrdersReceived(totalOrders);
        summary.setRevenueGenerated(totalRevenue);

        // Calculate overall conversion rate
        if (totalViews > 0) {
            BigDecimal conversionRate = BigDecimal.valueOf(totalOrders)
                    .divide(BigDecimal.valueOf(totalViews), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            summary.setConversionRate(conversionRate);
        } else {
            summary.setConversionRate(BigDecimal.ZERO);
        }

        return summary;
    }
}