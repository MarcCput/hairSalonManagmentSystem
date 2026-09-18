package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Role;
import za.ac.cput.service.IRoleService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleController Tests")
class RoleControllerTest {

    @Mock
    private IRoleService service;

    @InjectMocks
    private RoleController controller;

    @Test
    @DisplayName("create() delegates to service.register()")
    void create_delegatesToService() {
        Role role = mock(Role.class);
        when(service.register("ADMIN", "Full access")).thenReturn(role);

        RoleController.RoleRequest request = new RoleController.RoleRequest("ADMIN",
                "Full access");

        assertEquals(role, controller.create(request));
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService() {
        Role role = mock(Role.class);
        when(service.read("r1")).thenReturn(role);

        assertEquals(role, controller.read("r1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<Role> roles = List.of(mock(Role.class));
        when(service.getAll()).thenReturn(roles);

        assertEquals(roles, controller.getAll());
    }

    @Test
    @DisplayName("findByName() delegates to service.findByName()")
    void findByName_delegatesToService() {
        Role role = mock(Role.class);
        when(service.findByName("ADMIN")).thenReturn(role);

        assertEquals(role, controller.findByName("ADMIN"));
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("r1");

        verify(service).delete("r1");
    }
}

