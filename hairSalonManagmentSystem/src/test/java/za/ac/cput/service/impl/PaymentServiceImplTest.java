package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.*;
import za.ac.cput.domain.enums.*;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.AppointmentFactory;
import za.ac.cput.factory.CustomerFactory;
import za.ac.cput.factory.PromotionFactory;
import za.ac.cput.factory.SalonServiceFactory;
import za.ac.cput.factory.StylistFactory;
import za.ac.cput.repository.AppointmentRepository;
import za.ac.cput.repository.PaymentRepository;
import za.ac.cput.service.IPromotionService;
import za.ac.cput.service.impl.PaymentServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PaymentServiceImpl depends on another SERVICE (IPromotionService), not just repositories -
 * Mockito mocks interfaces just as easily as classes, so this is tested the same way.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Tests")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository repository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private IPromotionService promotionService;

    @InjectMocks
    private PaymentServiceImpl service;

    private Appointment appointment;

    @BeforeEach
    void setUp() {
        Customer customer = CustomerFactory.buildCustomer("Jane", "Doe", "jane@example.com", "0821234567");
        Stylist stylist = StylistFactory.buildStylist("Lebo", "Mokoena", "lebo@salon.com", "0711234567", "Colouring");
        SalonService salonService = SalonServiceFactory.buildService(
                "Blowout", "Full blowout and style", 60, new BigDecimal("200.00"), ServiceCategory.STYLING);
        appointment = AppointmentFactory.buildAppointment(
                customer, stylist, salonService, LocalDate.now().plusDays(1), LocalTime.of(10, 0), null);
    }

    @Test
    @DisplayName("read() returns the payment when found")
    void read_returnsPayment_whenFound() {
        Payment payment = buildPayment();
        when(repository.findById(payment.getPaymentId())).thenReturn(Optional.of(payment));

        assertEquals(payment, service.read(payment.getPaymentId()));
    }

    @Test
    @DisplayName("read() throws ResourceNotFoundException when missing")
    void read_throws_whenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.read("missing"));
    }

    @Test
    @DisplayName("processPayment() with no promo code charges the full amount")
    void processPayment_withoutPromoCode_chargesFullAmount() {
        when(appointmentRepository.findById(appointment.getAppointmentId())).thenReturn(Optional.of(appointment));
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = service.processPayment(
                appointment.getAppointmentId(), new BigDecimal("200.00"), PaymentMethod.CARD, null);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getFinalAmount().getValue()));
        verifyNoInteractions(promotionService); // no promo code was given, so the service should never be touched
    }

    @Test
    @DisplayName("processPayment() with a promo code validates it, applies the discount, and records usage")
    void processPayment_withPromoCode_appliesDiscountAndRecordsUsage() {
        Promotion promo = PromotionFactory.buildPromotion(
                "SAVE50", "Save R50", DiscountType.FIXED_AMOUNT, new BigDecimal("50.00"),
                LocalDate.now(), LocalDate.now().plusDays(10), 100);
        when(appointmentRepository.findById(appointment.getAppointmentId())).thenReturn(Optional.of(appointment));
        when(promotionService.validate("SAVE50")).thenReturn(promo);
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = service.processPayment(
                appointment.getAppointmentId(), new BigDecimal("200.00"), PaymentMethod.CARD, "SAVE50");

        assertEquals(0, new BigDecimal("150.00").compareTo(result.getFinalAmount().getValue()));
        verify(promotionService).validate("SAVE50");
        verify(promotionService).recordUsage(promo);
    }

    @Test
    @DisplayName("processPayment() throws ResourceNotFoundException when the appointment doesn't exist")
    void processPayment_throws_whenAppointmentMissing() {
        when(appointmentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.processPayment(
                "missing", new BigDecimal("200.00"), PaymentMethod.CARD, null));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("processPayment() throws InvalidOperationException for a non-positive amount")
    void processPayment_throws_whenAmountInvalid() {
        when(appointmentRepository.findById(appointment.getAppointmentId())).thenReturn(Optional.of(appointment));

        assertThrows(InvalidOperationException.class, () -> service.processPayment(
                appointment.getAppointmentId(), BigDecimal.ZERO, PaymentMethod.CARD, null));
    }

    @Test
    @DisplayName("findByAppointment() returns the payment linked to that appointment")
    void findByAppointment_returnsPayment_whenFound() {
        Payment payment = buildPayment();
        when(repository.findByAppointment_AppointmentId(appointment.getAppointmentId()))
                .thenReturn(Optional.of(payment));

        assertEquals(payment, service.findByAppointment(appointment.getAppointmentId()));
    }

    @Test
    @DisplayName("findByAppointment() throws ResourceNotFoundException when no payment exists for it")
    void findByAppointment_throws_whenNotFound() {
        when(repository.findByAppointment_AppointmentId(appointment.getAppointmentId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.findByAppointment(appointment.getAppointmentId()));
    }

    @Test
    @DisplayName("getAll() returns every payment")
    void getAll_returnsAllPayments() {
        when(repository.findAll()).thenReturn(List.of(buildPayment()));

        assertEquals(1, service.getAll().size());
    }

    private Payment buildPayment() {
        return za.ac.cput.factory.PaymentFactory.buildPayment(appointment, new BigDecimal("200.00"), PaymentMethod.CARD);
    }
}

