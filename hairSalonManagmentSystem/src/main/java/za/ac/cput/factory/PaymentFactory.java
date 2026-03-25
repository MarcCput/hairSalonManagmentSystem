package za.ac.cput.factory;

import org.junit.jupiter.api.Test;
import za.ac.cput.domain.Payment;

import static org.junit.jupiter.api.Assertions.*;

class PaymentFactoryTest {


    @Test
    void testCreatePaymentSuccess() {
        Payment p = PaymentFactory.createPayment("PAY-101", 500.0, "Credit Card");
        assertNotNull(p);
        System.out.println(p.toString());
    }

    @Test
    void testCreatePaymentFail() {
        Payment p = PaymentFactory.createPayment("", 0.0, ""); // Invalid data
        assertNull(p);
    }
}