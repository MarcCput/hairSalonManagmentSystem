package za.ac.cput.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.config.RoleSeeder;
import za.ac.cput.domain.Role;
import za.ac.cput.domain.Stylist;
import za.ac.cput.domain.User;
import za.ac.cput.domain.valueobject.Email;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.repository.CustomerRepository;
import za.ac.cput.repository.RoleRepository;
import za.ac.cput.repository.StylistRepository;
import za.ac.cput.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - staff accounts")
class AuthServiceStaffTest {

    @Mock UserRepository users;
    @Mock CustomerRepository customers;
    @Mock StylistRepository stylists;
    @Mock RoleRepository roles;

    // A real encoder, not a mock: the point of one test is that what is stored is a real hash.
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private AuthService service;

    private final Role stylistRole = new Role.Builder().setRoleId("r-s").setName(RoleSeeder.STYLIST).build();

    @BeforeEach
    void setUp() {
        service = new AuthService(users, customers, stylists, roles, encoder);
    }

    /** A real Stylist rather than a mock - the domain getters are not interceptable. */
    private Stylist thandi() {
        return new Stylist.Builder()
                .setStylistId("s1")
                .setFirstName("Thandi")
                .setLastName("Mahlangu")
                .setEmail(Email.of("thandi@judithhairsalon.co.za"))
                .setSpeciality("Colour")
                .setActive(true)
                .build();
    }

    @Test
    @DisplayName("a stylist login gets the STYLIST role, the stylist's own email, and a BCrypt hash")
    void createStylistAccount_mapsRoleEmailAndHashesPassword() {
        when(stylists.findById("s1")).thenReturn(Optional.of(thandi()));
        when(users.findByUsername("thandi")).thenReturn(Optional.empty());
        when(users.existsByEmail_Value("thandi@judithhairsalon.co.za")).thenReturn(false);
        when(roles.findByName(RoleSeeder.STYLIST)).thenReturn(Optional.of(stylistRole));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createStylistAccount("s1", "thandi", "long-enough-pw");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        User user = saved.getValue();

        assertEquals(RoleSeeder.STYLIST, user.getRole().getName());
        assertEquals("thandi@judithhairsalon.co.za", user.getEmail().getValue(),
                "email must come from the Stylist row, not from the request");
        assertNotEquals("long-enough-pw", user.getPassword(), "password must never be stored as typed");
        assertTrue(encoder.matches("long-enough-pw", user.getPassword()), "stored value must be a valid hash of it");
    }

    @Test
    @DisplayName("staff passwords under 12 characters are refused")
    void createStylistAccount_rejectsShortPassword() {
        when(stylists.findById("s1")).thenReturn(Optional.of(thandi()));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> service.createStylistAccount("s1", "thandi", "only8chr"));
        assertTrue(ex.getMessage().contains("12"));
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("a stylist cannot be given a second login")
    void createStylistAccount_rejectsSecondLoginForSameEmail() {
        when(stylists.findById("s1")).thenReturn(Optional.of(thandi()));
        when(users.findByUsername("thandi2")).thenReturn(Optional.empty());
        when(users.existsByEmail_Value("thandi@judithhairsalon.co.za")).thenReturn(true);

        assertThrows(InvalidOperationException.class,
                () -> service.createStylistAccount("s1", "thandi2", "long-enough-pw"));
        verify(users, never()).save(any());
    }
}
