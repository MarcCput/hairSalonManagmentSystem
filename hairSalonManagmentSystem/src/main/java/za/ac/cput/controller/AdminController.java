package za.ac.cput.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.domain.User;
import za.ac.cput.service.AuthService;

import java.util.List;

/**
 * Staff account management. Admin only.
 *
 * Guarded twice, as the chapter describes both mechanisms:
 *   - URL level  (9.6 / <intercept-url>): SecurityConfig maps /api/admin/** to ROLE_ADMIN
 *   - method level (9.7.6 / @PreAuthorize): the class annotation below
 * If either were removed by mistake, the other still holds.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    /** Password only - name and email are taken from the Stylist row itself. */
    public record StylistAccountRequest(@NotBlank String username, @NotBlank String password) {}

    public record StylistAccountResponse(String stylistId, String username) {}

    @PostMapping("/stylists/{stylistId}/account")
    @ResponseStatus(HttpStatus.CREATED)
    public StylistAccountResponse createStylistAccount(@PathVariable String stylistId,
                                                       @Valid @RequestBody StylistAccountRequest request) {
        User user = authService.createStylistAccount(stylistId, request.username(), request.password());
        // The password is not echoed back. The admin chose it and must hand it over in person.
        return new StylistAccountResponse(stylistId, user.getUsername());
    }

    @GetMapping("/stylist-accounts")
    public List<AuthService.StylistLogin> stylistAccounts() {
        return authService.stylistLogins();
    }
}
