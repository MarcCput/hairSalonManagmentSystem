package za.ac.cput.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import za.ac.cput.domain.User;
import za.ac.cput.repository.UserRepository;

import java.util.List;

/**
 * Chapter 9 Section D - the `<authentication-manager>` / `<jdbc-user-service>` block.
 *
 * The chapter wires authentication to two SQL statements:
 *     users-by-username-query       SELECT username, passwd, enabled FROM Users WHERE username=?
 *     authorities-by-username-query SELECT u.username, r.rolename FROM Users u, Roles r ...
 *
 * Spring Security 6 asks for the same two facts through this one interface instead, so the
 * queries become a repository call. Using the repository keeps one definition of "a user"
 * rather than hand-written SQL that silently rots when the entity changes.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.findByUsername(username)
                // Deliberately vague: saying "no such user" tells an attacker which usernames
                // exist. The login endpoint turns every failure into one generic message.
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            // Pre-existing rows from before security was added have no password and must not
            // be loginable - otherwise they would authenticate against an empty hash.
            throw new UsernameNotFoundException("Bad credentials");
        }

        String roleName = user.getRole() != null ? user.getRole().getName() : RoleSeeder.CUSTOMER;

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                // Roles are stored bare ("CUSTOMER"); Spring compares authorities with the
                // "ROLE_" prefix when you write hasRole("CUSTOMER"), so it is added here once.
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + roleName)))
                // Chapter 9's `enabled` column: a disabled account exists but cannot log in.
                .disabled(!user.isActive())
                .build();
    }
}
