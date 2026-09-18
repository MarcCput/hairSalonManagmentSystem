package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Appointment;
import za.ac.cput.domain.Customer;
import za.ac.cput.domain.Notification;
import za.ac.cput.domain.SalonService;
import za.ac.cput.domain.Stylist;
import za.ac.cput.domain.enums.NotificationChannel;
import za.ac.cput.domain.enums.NotificationType;
import za.ac.cput.domain.enums.ServiceCategory;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.AppointmentFactory;
import za.ac.cput.factory.CustomerFactory;
import za.ac.cput.factory.SalonServiceFactory;
import za.ac.cput.factory.StylistFactory;
import za.ac.cput.repository.AppointmentRepository;
import za.ac.cput.repository.NotificationRepository;
import za.ac.cput.service.impl.NotificationServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;
    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private NotificationServiceImpl service;

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
    @DisplayName("scheduleAppointmentReminder() builds and saves a reminder for a real appointment")
    void scheduleAppointmentReminder_savesReminder() {
        when(appointmentRepository.findById(appointment.getAppointmentId())).thenReturn(Optional.of(appointment));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = service.scheduleAppointmentReminder(appointment.getAppointmentId(), NotificationChannel.SMS);

        assertNotNull(result);
        assertEquals(NotificationType.APPOINTMENT_REMINDER, result.getType());
        assertEquals(NotificationChannel.SMS, result.getChannel());
    }

    @Test
    @DisplayName("scheduleAppointmentReminder() throws ResourceNotFoundException when the appointment doesn't exist")
    void scheduleAppointmentReminder_throws_whenAppointmentMissing() {
        when(appointmentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.scheduleAppointmentReminder("missing", NotificationChannel.SMS));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("sendPromotionNotification() builds and saves a promotion notification")
    void sendPromotionNotification_savesNotification() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = service.sendPromotionNotification("20% off this weekend!", NotificationChannel.EMAIL);

        assertNotNull(result);
        assertEquals(NotificationType.PROMOTION, result.getType());
        assertEquals("20% off this weekend!", result.getMessage());
    }

    @Test
    @DisplayName("sendPromotionNotification() throws InvalidOperationException for a blank message")
    void sendPromotionNotification_throws_whenMessageBlank() {
        assertThrows(InvalidOperationException.class,
                () -> service.sendPromotionNotification("", NotificationChannel.EMAIL));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("read() returns the notification when found")
    void read_returnsNotification_whenFound() {
        Notification notification = za.ac.cput.factory.NotificationFactory.buildAppointmentReminder(
                appointment, NotificationChannel.SMS);
        when(repository.findById(notification.getNotificationId())).thenReturn(Optional.of(notification));

        assertEquals(notification, service.read(notification.getNotificationId()));
    }

    @Test
    @DisplayName("getAll() returns every notification")
    void getAll_returnsAllNotifications() {
        Notification notification = za.ac.cput.factory.NotificationFactory.buildAppointmentReminder(
                appointment, NotificationChannel.SMS);
        when(repository.findAll()).thenReturn(List.of(notification));

        assertEquals(1, service.getAll().size());
    }
}

