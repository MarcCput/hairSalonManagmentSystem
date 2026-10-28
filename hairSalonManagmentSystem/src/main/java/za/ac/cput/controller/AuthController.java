package za.ac.cput.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.config.RoleSeeder;
import za.ac.cput.domain.Customer;
import za.ac.cput.domain.Stylist;
import za.ac.cput.domain.User;
import za.ac.cput.service.AuthService;

/**
 * Chapter 9.5.2 Form-Based Authentication.
 *
 * The chapter posts a form to `j_spring_security_check` and the container redirects to
 * login.jsp or the stored URL. A Vue SPA cannot use a redirect - it needs a JSON answer it
 * can act on - so these endpoints do the same three things explicitly:
 *   verify the credentials, establish a session, report who you now are.
 *
 * The session cookie that comes back is the authentication token for every later request,
 * exactly as with the chapter's form login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters.") String password,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            @NotBlank String phoneNumber) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    /** Who the caller is. Shape is deliberately minimal - no password, no role internals. */
    /**
     * customerId is set only for CUSTOMER logins, stylistId/speciality only for STYLIST logins,
     * neither for ADMIN. The frontend routes each role to its own dashboard from this.
     */
    public record SessionUser(String username, String firstName, String lastName,
                              String email, String role, String customerId,
                              String phoneNumber, java.time.LocalDate dateOfBirth,
                              String stylistId, String speciality) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionUser register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        authService.registerCustomer(request.username(), request.password(), request.firstName(),
                request.lastName(), request.email(), request.phoneNumber());
        // Sign the new customer straight in - having just typed the password, making them
        // type it again is friction with no security benefit.
        return authenticate(request.username(), request.password(), http);
    }

    @PostMapping("/login")
    public SessionUser login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authenticate(request.username(), request.password(), http);
    }

    /**
     * Chapter 9.7.7 "Obtaining information about the current user" - reads the principal out
     * of the SecurityContextHolder. The SPA calls this on page load to find out whether the
     * session cookie it already holds is still valid.
     */
    @GetMapping("/me")
    public ResponseEntity<SessionUser> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(describe(authentication.getName()));
    }

    private SessionUser authenticate(String username, String password, HttpServletRequest http) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            // One message for every failure. Distinguishing "no such user" from "wrong
            // password" would let someone enumerate valid usernames.
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Incorrect username or password.");
        }

        // Establish the session. Spring's fixation protection (SecurityConfig) issues a fresh
        // session id at this point, so a pre-planted id cannot be reused.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        http.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return describe(auth.getName());
    }

    /**
     * Previously this assumed every login was a customer and threw for anyone else - which
     * would have crashed the first admin or stylist login. Each role now reports what it has.
     */
    private SessionUser describe(String username) {
        User user = authService.userFor(username);
        String role = user.getRole() != null ? user.getRole().getName() : null;

        Customer customer = RoleSeeder.CUSTOMER.equals(role)
                ? authService.findCustomerFor(username).orElse(null) : null;
        Stylist stylist = RoleSeeder.STYLIST.equals(role)
                ? authService.findStylistFor(username).orElse(null) : null;

        String phone = customer != null && customer.getPhoneNumber() != null ? customer.getPhoneNumber().getValue()
                : stylist != null && stylist.getPhoneNumber() != null ? stylist.getPhoneNumber().getValue()
                : null;

        return new SessionUser(
                user.getUsername(), user.getFirstName(), user.getLastName(),
                user.getEmail().getValue(), role,
                customer != null ? customer.getCustomerId() : null,
                phone,
                customer != null ? customer.getDateOfBirth() : null,
                stylist != null ? stylist.getStylistId() : null,
                stylist != null ? stylist.getSpeciality() : null);
    }
}
