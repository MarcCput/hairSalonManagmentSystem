package za.ac.cput.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Role;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.RoleFactory;
import za.ac.cput.repository.RoleRepository;
import za.ac.cput.service.impl.RoleServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleServiceImpl Tests")
class RoleServiceImplTest {

    @Mock
    private RoleRepository repository;

    @InjectMocks
    private RoleServiceImpl service;

    @Test
    @DisplayName("register() builds and saves a valid role")
    void register_savesValidRole() {
        when(repository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role result = service.register("admin", "Full system access");

        assertNotNull(result);
        assertEquals("ADMIN", result.getName()); // factory uppercases the name
    }

    @Test
    @DisplayName("register() throws InvalidOperationException for a blank name")
    void register_throwsOnBlankName() {
        assertThrows(InvalidOperationException.class, () -> service.register("", "desc"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("findByName() returns the role when found")
    void findByName_returnsRole_whenFound() {
        Role role = RoleFactory.buildRole("ADMIN", "Full system access");
        when(repository.findByName("ADMIN")).thenReturn(Optional.of(role));

        assertEquals(role, service.findByName("ADMIN"));
    }

    @Test
    @DisplayName("findByName() throws ResourceNotFoundException when the name doesn't exist")
    void findByName_throws_whenNotFound() {
        when(repository.findByName("NOBODY")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByName("NOBODY"));
    }

    @Test
    @DisplayName("read() returns the role when found")
    void read_returnsRole_whenFound() {
        Role role = RoleFactory.buildRole("ADMIN", "Full system access");
        when(repository.findById(role.getRoleId())).thenReturn(Optional.of(role));

        assertEquals(role, service.read(role.getRoleId()));
    }

    @Test
    @DisplayName("delete() delegates to the repository")
    void delete_callsRepository() {
        service.delete("some-id");

        verify(repository).deleteById("some-id");
    }

    @Test
    @DisplayName("getAll() returns every role")
    void getAll_returnsAllRoles() {
        when(repository.findAll()).thenReturn(List.of(RoleFactory.buildRole("ADMIN", "desc")));

        assertEquals(1, service.getAll().size());
    }
}

