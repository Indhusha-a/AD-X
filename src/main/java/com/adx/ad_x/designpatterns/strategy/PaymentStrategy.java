package com.adx.ad_x.designpatterns.strategy;

import com.adx.ad_x.model.Payment;
import java.math.BigDecimal;

/**
 * DESIGN PATTERN 3: STRATEGY PATTERN
 * 
 * Purpose: Defines a family of payment processing algorithms and makes them interchangeable
 * Usage: Different payment methods (Credit Card, PayPal, Bank Transfer) implement this interface
 * 
 * Benefits: Easy to add new payment methods without modifying existing code
 */
public interface PaymentStrategy {
    
    /**
     * Process payment using specific payment method
     */
    boolean processPayment(Payment payment);
    
    /**
     * Validate payment before processing
     */
    boolean validatePayment(Payment payment);
    
    /**
     * Get payment method name
     */
    String getPaymentMethodName();
    
    /**
     * Calculate transaction fee
     */
    BigDecimal calculateTransactionFee(BigDecimal amount);
}
