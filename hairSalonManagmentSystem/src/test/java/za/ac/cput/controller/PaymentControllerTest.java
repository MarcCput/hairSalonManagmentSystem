package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Payment;
import za.ac.cput.domain.enums.PaymentMethod;
import za.ac.cput.service.IPaymentService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Tests")
class PaymentControllerTest {

    @Mock
    private IPaymentService service;

    @InjectMocks
    private PaymentController controller;

    @Test
    @DisplayName("process() delegates to service.processPayment()")
    void process_delegatesToService() {
        Payment payment = mock(Payment.class);
        BigDecimal amount = new BigDecimal("150.00");
        when(service.processPayment("a1", amount, PaymentMethod.CARD, "WELCOME10")).thenReturn(payment);

        PaymentController.ProcessPaymentRequest request =
                new PaymentController.ProcessPaymentRequest("a1", amount, PaymentMethod.CARD, "WELCOME10");

        assertEquals(payment, controller.process(request));
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService() {
        Payment payment = mock(Payment.class);
        when(service.read("p1")).thenReturn(payment);

        assertEquals(payment, controller.read("p1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<Payment> payments = List.of(mock(Payment.class));
        when(service.getAll()).thenReturn(payments);

        assertEquals(payments, controller.getAll());
    }

    @Test
    @DisplayName("findByAppointment() delegates to service.findByAppointment()")
    void findByAppointment_delegatesToService() {
        Payment payment = mock(Payment.class);
        when(service.findByAppointment("a1")).thenReturn(payment);

        assertEquals(payment, controller.findByAppointment("a1"));
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("p1");

        verify(service).delete("p1");
    }
}

