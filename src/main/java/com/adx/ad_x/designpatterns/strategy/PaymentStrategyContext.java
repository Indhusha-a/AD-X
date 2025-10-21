package com.adx.ad_x.designpatterns.strategy;

import com.adx.ad_x.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy Context: Manages different payment strategies
 */
@Component
public class PaymentStrategyContext {

    private final Map<String, PaymentStrategy> strategies = new HashMap<>();

    @Autowired
    public PaymentStrategyContext(CreditCardStrategy creditCardStrategy, 
                                 BankTransferStrategy bankTransferStrategy,
                                 BankSlipStrategy bankSlipStrategy) {
        strategies.put("CREDIT_CARD", creditCardStrategy);
        strategies.put("BANK_TRANSFER", bankTransferStrategy);
        strategies.put("BANK_SLIP", bankSlipStrategy);
    }

    public boolean executePayment(Payment payment, String paymentMethod) {
        PaymentStrategy strategy = strategies.get(paymentMethod.toUpperCase());
        
        if (strategy == null) {
            System.out.println("Unsupported payment method: " + paymentMethod);
            return false;
        }
        
        return strategy.processPayment(payment);
    }
}
