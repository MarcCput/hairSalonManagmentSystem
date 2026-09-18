package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Role;
import za.ac.cput.domain.User;
import za.ac.cput.service.IRoleService;
import za.ac.cput.service.IUserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * UserController has two collaborators (IUserService and IRoleService), so both get mocked -
 * exactly the same pattern as a service class with multiple repository dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private IUserService service;

    @Mock
    private IRoleService roleService;

    @InjectMocks
    private UserController controller;

    @Test
    @DisplayName("create() with a roleId looks up the role first, then registers the user with it")
    void create_withRoleId_looksUpRoleThenRegisters() {
        Role role = mock(Role.class);
        User user = mock(User.class);
        when(roleService.read("r1")).thenReturn(role);
        when(service.register("jdoe", "j@email.com", "Jane",
                "Doe", role)).thenReturn(user);

        UserController.UserRequest request = new UserController.UserRequest("jdoe",
                "j@email.com", "Jane", "Doe", "r1");

        User result = controller.create(request);

        assertEquals(user, result);
        verify(roleService).read("r1");
        verify(service).register("jdoe", "j@email.com", "Jane", "Doe",
                role);
    }

    @Test
    @DisplayName("create() without a roleId never touches roleService, registers with a null role")
    void create_withoutRoleId_skipsRoleLookup() {
        User user = mock(User.class);
        when(service.register("jdoe", "j@email.com", "Jane", "Doe",
                null)).thenReturn(user);

        UserController.UserRequest request = new UserController.UserRequest("jdoe",
                "j@email.com", "Jane", "Doe", null);

        User result = controller.create(request);

        assertEquals(user, result);
        verifyNoInteractions(roleService);
        verify(service).register("jdoe", "j@email.com", "Jane",
                "Doe", null);
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService() {
        User user = mock(User.class);
        when(service.read("u1")).thenReturn(user);

        assertEquals(user, controller.read("u1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<User> users = List.of(mock(User.class));
        when(service.getAll()).thenReturn(users);

        assertEquals(users, controller.getAll());
    }

    @Test
    @DisplayName("findByUsername() delegates to service.findByUsername()")
    void findByUsername_delegatesToService() {
        User user = mock(User.class);
        when(service.findByUsername("jdoe")).thenReturn(user);

        assertEquals(user, controller.findByUsername("jdoe"));
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("u1");

        verify(service).delete("u1");
    }
}

