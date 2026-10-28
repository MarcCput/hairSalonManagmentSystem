package za.ac.cput.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.service.AuthService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBootstrap")
class AdminBootstrapTest {

    @Mock AuthService authService;

    private AdminBootstrap bootstrap(String user, String pass) {
        return new AdminBootstrap(authService, user, pass, "admin@judithhairsalon.co.za");
    }

    @Test
    @DisplayName("creates the first admin from the environment variables")
    void createsAdminWhenNoneExists() throws Exception {
        when(authService.adminExists()).thenReturn(false);
        bootstrap("judith", "a-long-admin-password").run(null);
        verify(authService).createAdmin("judith", "a-long-admin-password", "admin@judithhairsalon.co.za");
    }

    @Test
    @DisplayName("does nothing once an admin exists - changing the variables cannot reset a password")
    void neverOverwritesAnExistingAdmin() throws Exception {
        when(authService.adminExists()).thenReturn(true);
        bootstrap("judith", "a-different-password").run(null);
        verify(authService, never()).createAdmin(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("creates nothing when the variables are not set - there is no built-in default admin")
    void noDefaultAdminWithoutVariables() throws Exception {
        when(authService.adminExists()).thenReturn(false);
        bootstrap("", "").run(null);
        verify(authService, never()).createAdmin(anyString(), anyString(), anyString());
    }
}
