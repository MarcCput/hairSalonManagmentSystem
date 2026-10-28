package za.ac.cput.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.service.AuthService;

/**
 * Creates the FIRST admin account - and only the first.
 *
 * Why not hard-code an admin? A username and password in source code sit in git history
 * forever, are identical on every machine that runs the code, can only be changed by editing
 * code, and are readable by everyone with access to the repository. That is a published
 * master key.
 *
 * Instead the credentials come from the environment, which is outside the repository:
 *
 *     SALON_ADMIN_USERNAME   e.g. judith
 *     SALON_ADMIN_PASSWORD   at least 12 characters
 *     SALON_ADMIN_EMAIL      optional, defaults to admin@judithhairsalon.co.za
 *
 * (Spring's relaxed binding maps SALON_ADMIN_USERNAME onto salon.admin.username.)
 *
 * The password is BCrypt-hashed on the way in like every other one. Once an admin exists this
 * does nothing, so changing the variables later does NOT reset anyone's password, and the
 * variables can be removed after the first start.
 */
@Component
public class AdminBootstrap implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthService authService;
    private final String username;
    private final String password;
    private final String email;

    public AdminBootstrap(AuthService authService,
                          @Value("${salon.admin.username:}") String username,
                          @Value("${salon.admin.password:}") String password,
                          @Value("${salon.admin.email:admin@judithhairsalon.co.za}") String email) {
        this.authService = authService;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    /** After RoleSeeder, which runs at HIGHEST_PRECEDENCE - the ADMIN role must exist first. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (authService.adminExists()) return;

        if (username.isBlank() || password.isBlank()) {
            // Not fatal: the public site and customer sign-up work fine without an admin.
            // Loud, because without one nobody can reach the admin dashboard or create stylists.
            log.warn("No admin account exists and SALON_ADMIN_USERNAME / SALON_ADMIN_PASSWORD are not set. "
                    + "The admin dashboard is unreachable until both are set and the app is restarted.");
            return;
        }

        try {
            authService.createAdmin(username, password, email);
            // Never log the password - logs are often copied, shipped and kept far longer than anyone expects.
            log.info("Created the first admin account '{}'. SALON_ADMIN_PASSWORD can now be removed.", username);
        } catch (InvalidOperationException | IllegalArgumentException e) {
            // e.g. password too short, or a bad email. Start anyway; say exactly what to fix.
            log.error("Could not create the admin account: {}", e.getMessage());
        }
    }
}
