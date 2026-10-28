package za.ac.cput.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.domain.Appointment;
import za.ac.cput.service.AuthService;
import za.ac.cput.service.IAppointmentService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final IAppointmentService service;
    private final AuthService authService;

    public AppointmentController(IAppointmentService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    public record BookAppointmentRequest(String customerId, String stylistId, String salonServiceId,
                                         LocalDate date, LocalTime startTime, String notes) {}

    /**
     * Chapter 9.7.6 method security, and 9.7.7 obtaining the current user.
     *
     * The @PreAuthorize is a second lock on the same door the SecurityConfig URL rule guards.
     * That is deliberate: if someone later re-maps this method to a different path, the URL
     * rule stops matching but this annotation still holds.
     *
     * The customerId is taken from the SESSION, not from the request body. Trusting the body
     * would let a signed-in customer post someone else's id and book - or cancel - on their
     * behalf. The body's customerId is now ignored.
     */
    @PostMapping("/book")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Appointment book(@RequestBody BookAppointmentRequest request, Authentication authentication) {
        String customerId = authService.customerFor(authentication.getName()).getCustomerId();
        return service.bookAppointment(customerId, request.stylistId(), request.salonServiceId(),
                request.date(), request.startTime(), request.notes());
    }

    @GetMapping("/{id}")
    public Appointment read(@PathVariable String id) {
        return service.read(id);
    }

    @GetMapping
    public List<Appointment> getAll() {
        return service.getAll();
    }

    @GetMapping("/customer/{customerId}")
    public List<Appointment> findByCustomer(@PathVariable String customerId) {
        return service.findByCustomer(customerId);
    }
    @GetMapping("/stylist/{stylistId}")
    public List<Appointment> findByStylist(@PathVariable String stylistId) {
        return service.findByStylist(stylistId);
    }

    @PostMapping("/{id}/confirm")
    public Appointment confirm(@PathVariable String id) {
        return service.confirmAppointment(id);
    }

    @PostMapping("/{id}/cancel")
    public Appointment cancel(@PathVariable String id) {
        return service.cancelAppointment(id);
    }

    @PostMapping("/{id}/complete")
    public Appointment complete(@PathVariable String id) {
        return service.completeAppointment(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}

