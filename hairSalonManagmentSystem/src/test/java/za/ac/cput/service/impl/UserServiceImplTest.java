package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Role;
import za.ac.cput.domain.User;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.RoleFactory;
import za.ac.cput.repository.UserRepository;
import za.ac.cput.service.impl.UserServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserServiceImpl service;

    private Role role;

    @BeforeEach
    void setUp() {
        role = RoleFactory.buildRole("CUSTOMER", "Regular customer account");
    }

    @Test
    @DisplayName("register() builds and saves a valid user")
    void register_savesValidUser() {
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.register("janedoe", "jane@example.com", "Jane", "Doe", role);

        assertNotNull(result);
        assertEquals("janedoe", result.getUsername());
        assertTrue(result.isActive());
    }

    @Test
    @DisplayName("register() throws InvalidOperationException for an invalid email")
    void register_throwsOnInvalidDetails() {
        assertThrows(InvalidOperationException.class,
                () -> service.register("janedoe", "not-an-email", "Jane", "Doe", role));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("findByUsername() returns the user when found")
    void findByUsername_returnsUser_whenFound() {
        User user = buildUser();
        when(repository.findByUsername("janedoe")).thenReturn(Optional.of(user));

        assertEquals(user, service.findByUsername("janedoe"));
    }

    @Test
    @DisplayName("findByUsername() throws ResourceNotFoundException when the username doesn't exist")
    void findByUsername_throws_whenNotFound() {
        when(repository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByUsername("nobody"));
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
        service.delete("some-id");

        verify(repository).deleteById("some-id");
    }

    @Test
    @DisplayName("getAll() returns every user")
    void getAll_returnsAllUsers() {
        when(repository.findAll()).thenReturn(List.of(buildUser()));

        assertEquals(1, service.getAll().size());
    }

    private User buildUser() {
        return za.ac.cput.factory.UserFactory.buildUser("janedoe", "jane@example.com", "Jane", "Doe", role);
    }
}

