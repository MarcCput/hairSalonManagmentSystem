package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Appointment;
import za.ac.cput.service.IAppointmentService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentController Tests")
public class AppointmentControllerTest {

    @Mock
    private IAppointmentService service;

    @InjectMocks
    private AppointmentController controller;

    @Test
    @DisplayName("book() delegates to service.bookAppointment()")
    void book_delegatesToService(){
        Appointment appointment = mock(Appointment.class);
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(10, 0);
        when(service.bookAppointment("c1", "s1", "svc1", date, time,
                "first visit")).thenReturn(appointment);

        AppointmentController.BookAppointmentRequest request =
                new AppointmentController.BookAppointmentRequest("c1", "s1",
                        "svc1", date, time, "first visit");

        assertEquals(appointment, controller.book(request));
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService(){
        Appointment appointment = mock(Appointment.class);
        when(service.read("a1")).thenReturn(appointment);

        assertEquals(appointment, controller.read("a1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService(){
        List<Appointment> appointments = List.of(mock(Appointment.class));
        when(service.getAll()).thenReturn(appointments);

        assertEquals(appointments, controller.getAll());
    }

    @Test
    @DisplayName("findByCustomer() delegates to service.findByCustomer()")
    void findByCustomer_delegatesToService(){
        List<Appointment> appointments = List.of(mock(Appointment.class));
        when(service.findByCustomer("c1")).thenReturn(appointments);

        assertEquals(appointments, controller.findByCustomer("c1"));
    }

    @Test
    @DisplayName("findByStylist() delegates to service.findByStylist()")
    void findByStylist_delegatesToService(){
        List<Appointment> appointments = List.of(mock(Appointment.class));
        when(service.findByStylist("s1")).thenReturn(appointments);

        assertEquals(appointments, controller.findByStylist("s1"));
    }

    @Test
    @DisplayName("confirm() delegates to service.confirmAppointment()")
    void confirm_delegatesToService(){
        Appointment appointment = mock(Appointment.class);
        when(service.confirmAppointment("a1")).thenReturn(appointment);

        assertEquals(appointment, controller.confirm("a1"));
    }

    @Test
    @DisplayName("cancel() delegates to service.cancelAppointment()")
    void cancel_delegatesToService(){
        Appointment appointment = mock(Appointment.class);
        when(service.cancelAppointment("a1")).thenReturn(appointment);

        assertEquals(appointment, controller.cancel("a1"));
    }

    @Test
    @DisplayName("complete() delegates to service.completeAppointment()")
    void complete_delegatesToService(){
        Appointment appointment = mock(Appointment.class);
        when(service.completeAppointment("a1")).thenReturn(appointment);

        assertEquals(appointment, controller.complete("a1"));
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService(){
        controller.delete("a1");

        verify(service).delete("a1");
    }
}
