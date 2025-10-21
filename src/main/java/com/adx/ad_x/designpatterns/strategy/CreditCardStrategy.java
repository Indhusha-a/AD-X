package com.adx.ad_x.designpatterns.strategy;

import com.adx.ad_x.model.Payment;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Concrete Strategy: Credit Card Payment Processing
 */
@Component
public class CreditCardStrategy implements PaymentStrategy {

    @Override
    public boolean processPayment(Payment payment) {
        if (!validatePayment(payment)) {
            return false;
        }
        
        System.out.println("Processing Credit Card payment: $" + payment.getAmount());
        payment.setStatus("COMPLETED");
        payment.setPaymentMethod("CREDIT_CARD");
        return true;
    }

    @Override
    public boolean validatePayment(Payment payment) {
        return payment != null && 
               payment.getAmount() != null && 
               payment.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
               payment.getBuyer() != null &&
               payment.getOrder() != null;
    }

    @Override
    public String getPaymentMethodName() {
        return "CREDIT_CARD";
    }

    @Override
    public BigDecimal calculateTransactionFee(BigDecimal amount) {
        // 2.9% + $0.30 fee
        return amount.multiply(new BigDecimal("0.029")).add(new BigDecimal("0.30"));
    }
}
