package za.ac.cput.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import za.ac.cput.domain.Role;
import za.ac.cput.repository.RoleRepository;

import java.util.UUID;

/**
 * Chapter 9.3 - "Create users and/or groups -> Define roles in application -> Map roles to
 * users" (Figure 9.1). This covers the middle step: the application's roles have to exist
 * before anyone can be mapped onto one.
 *
 * The roles table ships empty, so without this the very first sign-up would fail with no
 * CUSTOMER role to assign. Runs on every start but only inserts what is missing, so it is
 * safe against an existing database.
 *
 * Roles ARE hard-coded, deliberately: the code refers to them by name (hasRole("ADMIN")), so
 * they are part of the program, not data. Accounts are a different matter - see AdminBootstrap.
 */
@Component
public class RoleSeeder implements ApplicationRunner, Ordered {

    /** Role names, stored WITHOUT the "ROLE_" prefix - see AppUserDetailsService for why. */
    public static final String CUSTOMER = "CUSTOMER";
    public static final String STYLIST  = "STYLIST";
    public static final String ADMIN    = "ADMIN";

    private final RoleRepository roles;

    public RoleSeeder(RoleRepository roles) {
        this.roles = roles;
    }

    /** Runs first: AdminBootstrap needs the ADMIN role to exist before it can assign it. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void run(ApplicationArguments args) {
        create(CUSTOMER, "Books appointments and manages their own bookings");
        create(STYLIST,  "Views and works their own schedule");
        create(ADMIN,    "Full access to salon operations");
    }

    private void create(String name, String description) {
        if (roles.findByName(name).isPresent()) return;
        roles.save(new Role.Builder()
                .setRoleId(UUID.randomUUID().toString())
                .setName(name)
                .setDescription(description)
                .build());
    }
}
