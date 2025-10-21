package com.adx.ad_x.designpatterns.strategy;

import com.adx.ad_x.model.Payment;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Concrete Strategy: Bank Transfer Payment Processing
 */
@Component
public class BankTransferStrategy implements PaymentStrategy {

    @Override
    public boolean processPayment(Payment payment) {
        if (!validatePayment(payment)) {
            return false;
        }
        
        System.out.println("Processing Bank Transfer payment: $" + payment.getAmount());
        payment.setStatus("COMPLETED");
        payment.setPaymentMethod("BANK_TRANSFER");
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
        return "BANK_TRANSFER";
    }

    @Override
    public BigDecimal calculateTransactionFee(BigDecimal amount) {
        // Bank transfer fee: flat $2.00
        return new BigDecimal("2.00");
    }
}
