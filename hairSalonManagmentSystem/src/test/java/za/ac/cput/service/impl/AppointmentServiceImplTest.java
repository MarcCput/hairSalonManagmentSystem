package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Appointment;
import za.ac.cput.domain.Customer;
import za.ac.cput.domain.SalonService;
import za.ac.cput.domain.Stylist;
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
import za.ac.cput.repository.SalonServiceRepository;
import za.ac.cput.repository.StylistRepository;
import za.ac.cput.service.INotificationService;
import za.ac.cput.service.impl.AppointmentServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The richest service test in this codebase: AppointmentServiceImpl depends on FOUR repositories
 * and enforces a real business rule (no double-booking a stylist), which is exactly the kind of
 * logic Mockito tests are best at proving without needing a real database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentServiceImpl Tests")
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository repository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private StylistRepository stylistRepository;
    @Mock
    private SalonServiceRepository salonServiceRepository;
    @Mock
    private INotificationService notificationService;
    @InjectMocks
    private AppointmentServiceImpl service;

    private Customer customer;
    private Stylist stylist;
    private SalonService salonService;
    private Appointment existingAppointment;

    @BeforeEach
    void setUp() {
        customer = CustomerFactory.buildCustomer("Jane", "Doe", "jane@example.com", "0821234567");
        stylist = StylistFactory.buildStylist("Lebo", "Mokoena", "lebo@salon.com", "0711234567", "Colouring");
        salonService = SalonServiceFactory.buildService(
                "Blowout", "Full blowout and style", 60, new BigDecimal("200.00"), ServiceCategory.STYLING);
        existingAppointment = AppointmentFactory.buildAppointment(
                customer, stylist, salonService, LocalDate.now().plusDays(1), LocalTime.of(10, 0), null);
    }

    @Test
    @DisplayName("read() returns the appointment when found")
    void read_returnsAppointment_whenFound() {
        when(repository.findById(existingAppointment.getAppointmentId())).thenReturn(Optional.of(existingAppointment));

        assertEquals(existingAppointment, service.read(existingAppointment.getAppointmentId()));
    }

    @Test
    @DisplayName("read() throws ResourceNotFoundException when missing")
    void read_throws_whenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.read("missing"));
    }

    @Test
    @DisplayName("delete() delegates to the repository")
    void delete_callsRepository() {
        service.delete(existingAppointment.getAppointmentId());

        verify(repository).deleteById(existingAppointment.getAppointmentId());
    }

    @Test
    @DisplayName("getAll() returns every appointment")
    void getAll_returnsAllAppointments() {
        when(repository.findAll()).thenReturn(List.of(existingAppointment));

        assertEquals(1, service.getAll().size());
    }

    @Test
    @DisplayName("bookAppointment() succeeds when the stylist has no conflicting appointment")
    void bookAppointment_succeeds_whenSlotIsFree() {
        LocalDate date = LocalDate.now().plusDays(2);
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
        when(stylistRepository.findById(stylist.getStylistId())).thenReturn(Optional.of(stylist));
        when(salonServiceRepository.findById(salonService.getServiceId())).thenReturn(Optional.of(salonService));
        when(repository.findByStylist_StylistIdAndAppointmentDate(stylist.getStylistId(), date))
                .thenReturn(List.of()); // no existing appointments that day
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = service.bookAppointment(
                customer.getCustomerId(), stylist.getStylistId(), salonService.getServiceId(),
                date, LocalTime.of(9, 0), "first visit");

        assertNotNull(result);
        assertEquals(AppointmentStatus.PENDING, result.getStatus());
        assertEquals(LocalTime.of(10, 0), result.getEndTime()); // 9:00 + 60 min duration from the service
    }

    @Test
    @DisplayName("bookAppointment() rejects an overlapping slot with the same stylist")
    void bookAppointment_throws_whenSlotOverlaps() {
        LocalDate date = existingAppointment.getAppointmentDate();
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
        when(stylistRepository.findById(stylist.getStylistId())).thenReturn(Optional.of(stylist));
        when(salonServiceRepository.findById(salonService.getServiceId())).thenReturn(Optional.of(salonService));
        // existingAppointment runs 10:00-11:00; a request for 10:30 overlaps it
        when(repository.findByStylist_StylistIdAndAppointmentDate(stylist.getStylistId(), date))
                .thenReturn(List.of(existingAppointment));

        assertThrows(InvalidOperationException.class, () -> service.bookAppointment(
                customer.getCustomerId(), stylist.getStylistId(), salonService.getServiceId(),
                date, LocalTime.of(10, 30), null));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("bookAppointment() ignores CANCELLED appointments when checking for overlaps")
    void bookAppointment_ignoresCancelledAppointments() {
        LocalDate date = existingAppointment.getAppointmentDate();
        Appointment cancelled = new Appointment.Builder()
                .setAppointmentId(existingAppointment.getAppointmentId())
                .setCustomer(existingAppointment.getCustomer())
                .setStylist(existingAppointment.getStylist())
                .setSalonService(existingAppointment.getSalonService())
                .setAppointmentDate(existingAppointment.getAppointmentDate())
                .setTimeSlot(existingAppointment.getTimeSlot())
                .setCreatedAt(existingAppointment.getCreatedAt())
                .setStatus(AppointmentStatus.CANCELLED)
                .build();

        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
        when(stylistRepository.findById(stylist.getStylistId())).thenReturn(Optional.of(stylist));
        when(salonServiceRepository.findById(salonService.getServiceId())).thenReturn(Optional.of(salonService));
        when(repository.findByStylist_StylistIdAndAppointmentDate(stylist.getStylistId(), date))
                .thenReturn(List.of(cancelled)); // same slot, but cancelled - should NOT block the new booking
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = service.bookAppointment(
                customer.getCustomerId(), stylist.getStylistId(), salonService.getServiceId(),
                date, LocalTime.of(10, 30), null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("bookAppointment() throws ResourceNotFoundException when the customer doesn't exist")
    void bookAppointment_throws_whenCustomerMissing() {
        when(customerRepository.findById("missing-customer")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.bookAppointment(
                "missing-customer", stylist.getStylistId(), salonService.getServiceId(),
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), null));

        verifyNoInteractions(stylistRepository, salonServiceRepository);
    }

    @Test
    @DisplayName("bookAppointment() throws ResourceNotFoundException when the stylist doesn't exist")
    void bookAppointment_throws_whenStylistMissing() {
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
        when(stylistRepository.findById("missing-stylist")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.bookAppointment(
                customer.getCustomerId(), "missing-stylist", salonService.getServiceId(),
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), null));
    }

    @Test
    @DisplayName("bookAppointment() throws ResourceNotFoundException when the salon service doesn't exist")
    void bookAppointment_throws_whenSalonServiceMissing() {
        when(customerRepository.findById(customer.getCustomerId())).thenReturn(Optional.of(customer));
        when(stylistRepository.findById(stylist.getStylistId())).thenReturn(Optional.of(stylist));
        when(salonServiceRepository.findById("missing-service")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.bookAppointment(
                customer.getCustomerId(), stylist.getStylistId(), "missing-service",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), null));
    }

    @Test
    @DisplayName("cancelAppointment() rebuilds the appointment with CANCELLED status and saves it")
    void cancelAppointment_savesWithCancelledStatus() {
        when(repository.findById(existingAppointment.getAppointmentId())).thenReturn(Optional.of(existingAppointment));
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);

        Appointment result = service.cancelAppointment(existingAppointment.getAppointmentId());

        verify(repository).save(captor.capture());
        assertEquals(AppointmentStatus.CANCELLED, captor.getValue().getStatus());
        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        // everything else about the appointment should be untouched
        assertEquals(existingAppointment.getCustomer(), result.getCustomer());
        assertEquals(existingAppointment.getTimeSlot(), result.getTimeSlot());
    }


    @Test
    @DisplayName("completeAppointment() rebuilds the appointment with COMPLETED status and saves it")
    void completeAppointment_savesWithCompletedStatus() {
        when(repository.findById(existingAppointment.getAppointmentId())).thenReturn(Optional.of(existingAppointment));
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = service.completeAppointment(existingAppointment.getAppointmentId());

        assertEquals(AppointmentStatus.COMPLETED, result.getStatus());
    }

    @Test
    @DisplayName("findByCustomer() delegates to the repository's derived query")
    void findByCustomer_delegatesToRepository() {
        when(repository.findByCustomer_CustomerId(customer.getCustomerId())).thenReturn(List.of(existingAppointment));

        List<Appointment> result = service.findByCustomer(customer.getCustomerId());

        assertEquals(1, result.size());
    }
    @Test
    @DisplayName("confirmAppointment() rebuilds the appointment with CONFIRMED status and notifies the customer")
    void confirmAppointment_savesWithConfirmedStatus_andNotifies() {
        when(repository.findById(existingAppointment.getAppointmentId()))
                .thenReturn(Optional.of(existingAppointment));
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Appointment result = service.confirmAppointment(existingAppointment.getAppointmentId());
        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        verify(notificationService).notifyAppointmentConfirmed(result);
    }
    @Test
    @DisplayName("findByStylist() delegates to the repository's derived query")
    void findByStylist_delegatesToRepository() {
        when(repository.findByStylist_StylistId(stylist.getStylistId()))
                .thenReturn(List.of(existingAppointment));
        List<Appointment> result = service.findByStylist(stylist.getStylistId());
        assertEquals(1, result.size());
    }

}

