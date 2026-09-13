package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.*;
import za.ac.cput.domain.enums.AppointmentStatus;
import za.ac.cput.domain.enums.ServiceCategory;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.AppointmentFactory;
import za.ac.cput.factory.CustomerFactory;
import za.ac.cput.factory.SalonServiceFactory;
import za.ac.cput.factory.StylistFactory;
import za.ac.cput.repository.AppointmentRepository;
import za.ac.cput.repository.CustomerRepository;
import za.ac.cput.repository.FeedbackRepository;
import za.ac.cput.service.impl.FeedbackServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackServiceImpl Tests")
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRepository repository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private FeedbackServiceImpl service;

    private Customer customer;
    private Appointment completedAppointment;

    @BeforeEach
    void setUp() {
        customer = CustomerFactory.buildCustomer("Jane", "Doe", "jane@example.com", "0821234567");
        Stylist stylist = StylistFactory.buildStylist("Lebo", "Mokoena", "lebo@salon.com", "0711234567", "Colouring");
        SalonService salonService = SalonServiceFactory.buildService(
                "Blowout", "Full blowout and style", 60, new BigDecimal("200.00"), ServiceCategory.STYLING);
        Appointment appointment = AppointmentFactory.buildAppointment(
                customer, stylist, salonService, LocalDate.now().plusDays(1), LocalTime.of(10, 0), null);
        // Feedback is only valid on a COMPLETED appointment - rebuild with that status.
        completedAppointment = new Appointment.Builder()
                .setAppointmentId(appointment.getAppointmentId())
                .setCustomer(appointment.getCustomer())
                .setStylist(appointment.getStylist())
                .setSalonService(appointment.getSalonService())
                .setAppointmentDate(appointment.getAppointmentDate())
                .setTimeSlot(appointment.getTimeSlot())
                .setCreatedAt(appointment.getCreatedAt())
                .setStatus(AppointmentStatus.COMPLETED)
                .build();
    }

    @Test
    @DisplayName("submitFeedback() saves valid feedback for a completed appointment")
    void submitFeedback_savesValidFeedback() {
        when(appointmentRepository.findById(completedAppointment.getAppointmentId()))
                .thenReturn(Optional.of(completedAppointment));
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
        when(repository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Feedback result = service.submitFeedback(
                completedAppointment.getAppointmentId(), customer.getCustomerId(), 5, "Amazing service!");

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertFalse(result.isVerified());
    }

    @Test
    @DisplayName("submitFeedback() throws ResourceNotFoundException when the appointment doesn't exist")
    void submitFeedback_throws_whenAppointmentMissing() {
        when(appointmentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitFeedback("missing", customer.getCustomerId(), 5, "Great"));

        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("submitFeedback() throws ResourceNotFoundException when the customer doesn't exist")
    void submitFeedback_throws_whenCustomerMissing() {
        when(appointmentRepository.findById(completedAppointment.getAppointmentId()))
                .thenReturn(Optional.of(completedAppointment));
        when(customerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitFeedback(completedAppointment.getAppointmentId(), "missing", 5, "Great"));
    }

    @Test
    @DisplayName("submitFeedback() throws InvalidOperationException for a rating outside 1-5")
    void submitFeedback_throws_whenRatingOutOfRange() {
        when(appointmentRepository.findById(completedAppointment.getAppointmentId()))
                .thenReturn(Optional.of(completedAppointment));
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));

        assertThrows(InvalidOperationException.class, () -> service.submitFeedback(
                completedAppointment.getAppointmentId(), customer.getCustomerId(), 6, "Too high"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("read() returns feedback when found")
    void read_returnsFeedback_whenFound() {
        Feedback feedback = za.ac.cput.factory.FeedbackFactory.buildFeedback(
                completedAppointment, customer, 5, "Great");
        when(repository.findById(feedback.getReviewId())).thenReturn(Optional.of(feedback));

        assertEquals(feedback, service.read(feedback.getReviewId()));
    }

    @Test
    @DisplayName("getAll() returns every review")
    void getAll_returnsAllFeedback() {
        Feedback feedback = za.ac.cput.factory.FeedbackFactory.buildFeedback(
                completedAppointment, customer, 5, "Great");
        when(repository.findAll()).thenReturn(List.of(feedback));

        assertEquals(1, service.getAll().size());
    }
}

