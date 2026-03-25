package za.ac.cput.factory;

import za.ac.cput.domain.Payment;

public class PaymentFactory {

    public static Payment createPayment(String paymentId, double amount, String paymentMethod){
        if(paymentId == null || paymentId.isEmpty()){
            return null;
        }

        if(amount <= 0){
            return null;
        }

        if(paymentMethod == null || paymentMethod.isEmpty()){
            return null;
        }

        return new Payment.Builder()
                .setPaymentId(paymentId)
                .setAmount(amount)
                .setPaymentMethod(paymentMethod)
                .build();
    }
}