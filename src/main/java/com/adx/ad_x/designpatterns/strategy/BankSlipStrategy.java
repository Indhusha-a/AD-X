package com.adx.ad_x.designpatterns.strategy;

import com.adx.ad_x.model.Payment;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Concrete Strategy: Bank Slip Payment Processing
 */
@Component
public class BankSlipStrategy implements PaymentStrategy {

    @Override
    public boolean processPayment(Payment payment) {
        if (!validatePayment(payment)) {
            return false;
        }
        
        System.out.println("Processing Bank Slip payment: $" + payment.getAmount());
        payment.setStatus("COMPLETED");
        payment.setPaymentMethod("BANK_SLIP");
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
        return "BANK_SLIP";
    }

    @Override
    public BigDecimal calculateTransactionFee(BigDecimal amount) {
        // Bank slip fee: free
        return BigDecimal.ZERO;
    }
}
