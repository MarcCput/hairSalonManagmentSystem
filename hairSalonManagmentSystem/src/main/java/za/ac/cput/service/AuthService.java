package za.ac.cput.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.config.RoleSeeder;
import za.ac.cput.domain.Customer;
import za.ac.cput.domain.Role;
import za.ac.cput.domain.Stylist;
import za.ac.cput.domain.User;
import za.ac.cput.domain.valueobject.Email;
import za.ac.cput.domain.valueobject.PhoneNumber;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.repository.CustomerRepository;
import za.ac.cput.repository.RoleRepository;
import za.ac.cput.repository.StylistRepository;
import za.ac.cput.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Account creation. Chapter 9.3 step 1 - "The Application Developer writes code to prompt the
 * user for their user name and password" - plus the role mapping from Figure 9.1.
 *
 * A login (User) is credentials plus ONE role. What that login represents in the salon lives in
 * a separate table, linked by shared email:
 *   CUSTOMER -> Customer row (created here at sign-up; every Appointment references it)
 *   STYLIST  -> Stylist row  (already exists; the admin attaches a login to it)
 *   ADMIN    -> nothing      (a salon operator, not a bookable person)
 *
 * Three ways in, and who can use each - this is the role separation the chapter asks for:
 *   registerCustomer      anyone, from the public sign-up page
 *   createStylistAccount  an ADMIN only (enforced in SecurityConfig and AdminController)
 *   createAdmin           nobody over HTTP - only AdminBootstrap, at startup, from env vars
 */
@Service
public class AuthService {

    /** Staff accounts unlock far more than a customer's, so the bar is higher. */
    public static final int CUSTOMER_MIN_PASSWORD = 8;
    public static final int STAFF_MIN_PASSWORD = 12;

    private final UserRepository users;
    private final CustomerRepository customers;
    private final StylistRepository stylists;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, CustomerRepository customers, StylistRepository stylists,
                       RoleRepository roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.customers = customers;
        this.stylists = stylists;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------------------------------------------------------------- customers

    @Transactional
    public User registerCustomer(String username, String rawPassword, String firstName,
                                 String lastName, String email, String phoneNumber) {

        // Email.of() validates and normalises (trim + lowercase), so two sign-ups differing
        // only by case cannot create two accounts for the same person.
        Email validated = Email.of(email);
        if (customers.findByEmail_Value(validated.getValue()).isPresent())
            throw new InvalidOperationException("An account already uses that email address.");

        customers.save(new Customer.Builder()
                .setCustomerId(UUID.randomUUID().toString())
                .setFirstName(firstName)
                .setLastName(lastName)
                .setEmail(validated)
                .setPhoneNumber(PhoneNumber.of(phoneNumber))
                .setRegisteredAt(LocalDateTime.now())
                .build());

        return saveLogin(username, rawPassword, CUSTOMER_MIN_PASSWORD, validated,
                firstName, lastName, RoleSeeder.CUSTOMER);
    }

    // ---------------------------------------------------------------- staff

    /**
     * Gives an existing stylist a login. The stylist's name and email come from their Stylist
     * row rather than the request, so an admin cannot accidentally attach a login to the wrong
     * person by mistyping an email.
     */
    @Transactional
    public User createStylistAccount(String stylistId, String username, String rawPassword) {
        Stylist stylist = stylists.findById(stylistId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Stylist", stylistId));
        if (stylist.getEmail() == null)
            throw new InvalidOperationException("This stylist has no email address to link a login to.");

        return saveLogin(username, rawPassword, STAFF_MIN_PASSWORD, stylist.getEmail(),
                stylist.getFirstName(), stylist.getLastName(), RoleSeeder.STYLIST);
    }

    /** Only ever called by AdminBootstrap. There is deliberately no HTTP route to this. */
    @Transactional
    public User createAdmin(String username, String rawPassword, String email) {
        return saveLogin(username, rawPassword, STAFF_MIN_PASSWORD, Email.of(email),
                "Salon", "Administrator", RoleSeeder.ADMIN);
    }

    public boolean adminExists() {
        return users.existsByRole_Name(RoleSeeder.ADMIN);
    }

    /** Which stylists already have a login - drives the admin page's "Add login" buttons. */
    public List<StylistLogin> stylistLogins() {
        return users.findByRole_Name(RoleSeeder.STYLIST).stream()
                .flatMap(user -> stylists.findByEmail_Value(user.getEmail().getValue())
                        .map(stylist -> new StylistLogin(stylist.getStylistId(), user.getUsername()))
                        .stream())
                .toList();
    }

    public record StylistLogin(String stylistId, String username) {}

    // ---------------------------------------------------------------- shared

    /**
     * The one place a login is written. Every account, whatever its role, goes through the same
     * checks and the same hashing - there is no second, weaker path.
     */
    private User saveLogin(String username, String rawPassword, int minPassword, Email email,
                           String firstName, String lastName, String roleName) {
        if (username == null || username.isBlank())
            throw new InvalidOperationException("Username is required.");
        if (rawPassword == null || rawPassword.length() < minPassword)
            throw new InvalidOperationException("Password must be at least " + minPassword + " characters.");
        if (users.findByUsername(username).isPresent())
            throw new InvalidOperationException("That username is already taken.");
        // One login per email. Without this, the email link to a Customer or Stylist row
        // would be ambiguous - two logins could claim the same person.
        if (users.existsByEmail_Value(email.getValue()))
            throw new InvalidOperationException("A login already exists for that email address.");

        Role role = roles.findByName(roleName)
                .orElseThrow(() -> new InvalidOperationException(
                        "The " + roleName + " role is missing - the application did not seed its roles."));

        return users.save(new User.Builder()
                .setUserId(UUID.randomUUID().toString())
                .setUsername(username)
                // Hashed HERE, on the way in. The raw password is never persisted and never
                // leaves this method.
                .setPassword(passwordEncoder.encode(rawPassword))
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setActive(true)              // chapter 9's `enabled` column
                .setCreatedAt(LocalDateTime.now())
                .setRole(role)
                .build());
    }

    // ---------------------------------------------------------------- lookups

    /** The User row (credentials + role) behind a signed-in username. */
    public User userFor(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new InvalidOperationException("Unknown user."));
    }

    /** The Customer behind a login, if it is a customer login. */
    public Optional<Customer> findCustomerFor(String username) {
        return users.findByUsername(username)
                .flatMap(user -> customers.findByEmail_Value(user.getEmail().getValue()));
    }

    /** The Stylist behind a login, if it is a stylist login. */
    public Optional<Stylist> findStylistFor(String username) {
        return users.findByUsername(username)
                .flatMap(user -> stylists.findByEmail_Value(user.getEmail().getValue()));
    }

    /** For callers that REQUIRE a customer, e.g. booking. */
    public Customer customerFor(String username) {
        return findCustomerFor(username)
                .orElseThrow(() -> new InvalidOperationException(
                        "This login has no customer record attached."));
    }
}
