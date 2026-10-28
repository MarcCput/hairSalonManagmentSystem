package za.ac.cput.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

/**
 * Chapter 9 - ApplicationContextSecurity.xml, expressed as Java configuration.
 *
 * The chapter's file has four sections; this class covers B, C and D:
 *   Section B  <global-method-security>  -> @EnableMethodSecurity below
 *   Section C  <http> + <intercept-url>  -> securityFilterChain()
 *   Section D  <authentication-manager>  -> AppUserDetailsService + the PasswordEncoder here
 * (Section A was XML namespace plumbing and has no Java equivalent.)
 */
@Configuration
// Section B: enables @PreAuthorize / @Secured on service and controller methods (9.7.6).
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Chapter 9.7.2 adds a <password-encoder hash="md5"/>.
     *
     * Deliberate deviation: MD5 (and SHA) are plain digests designed to be FAST, which is
     * exactly wrong for passwords - commodity hardware tries billions per second, and the
     * chapter's example is unsalted, so identical passwords produce identical hashes.
     * BCrypt is purpose-built: salted per password and tunably slow. The concept the chapter
     * teaches - never store plain text, encode on the way in - is unchanged.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Exposed so the login endpoint can verify credentials the same way the filter chain does. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF is disabled because the API is called by a same-origin SPA with no cookie-
            // based form posts. See the write-up: this is the one trade-off worth revisiting.
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session -> {
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
                // Chapter 9.7.5 - session fixation protection. migrateSession is the chapter's
                // default: a brand new session id is issued at login, so a session id an
                // attacker planted beforehand is useless.
                session.sessionFixation(fixation -> fixation.migrateSession());
                // Chapter 9.7.4 - concurrent session control, max-sessions="1".
                session.maximumSessions(1);
            })

            // Section C: <intercept-url>. Order matters - the first match wins, so the
            // specific public routes are listed before the catch-all.
            // Section C: <intercept-url>. ORDER MATTERS - the first matching rule wins.
            // Every /api rule must therefore come before the SPA catch-all at the bottom,
            // otherwise a pattern like /*/{path} swallows /api/roles and opens it to anyone.
            .authorizeHttpRequests(auth -> auth

                // --- unprotected API (the chapter's IS_AUTHENTICATED_ANONYMOUSLY) ---
                // Sign-up and log-in must be reachable by someone who is not yet anyone.
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                // The marketing site reads these to render the price list and the team.
                .requestMatchers(HttpMethod.GET,
                        "/api/services/**", "/api/stylists/**", "/api/feedback").permitAll()

                // --- protected API ---
                // THE REQUIREMENT: you must be a signed-up, signed-in customer to book.
                .requestMatchers(HttpMethod.POST, "/api/appointments/book").hasRole(RoleSeeder.CUSTOMER)

                // Account and role administration is admin-only - including creating
                // stylist logins (AdminController). Must precede the /api/** rule below.
                .requestMatchers("/api/admin/**", "/api/users/**", "/api/roles/**").hasRole(RoleSeeder.ADMIN)

                // Changing the catalogue is staff-only; reading it was permitted above.
                .requestMatchers(HttpMethod.POST,   "/api/services/**", "/api/stylists/**").hasRole(RoleSeeder.ADMIN)
                .requestMatchers(HttpMethod.PUT,    "/api/services/**", "/api/stylists/**").hasRole(RoleSeeder.ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/services/**", "/api/stylists/**").hasRole(RoleSeeder.ADMIN)

                // Anything else under /api needs a login. Stated explicitly so that a new
                // endpoint is closed by default rather than falling through to the SPA rule.
                .requestMatchers("/api/**").authenticated()

                // --- the built Vue app (everything below is NOT under /api) ---
                .requestMatchers(HttpMethod.GET,
                        "/", "/index.html", "/favicon.ico", "/assets/**", "/images/**").permitAll()
                // Dot-less paths forwarded to index.html by SpaFallbackController. The Vue
                // router decides what to show; the API rules above are what actually guard data.
                .requestMatchers(HttpMethod.GET,
                        "/{path:[^.]*}", "/*/{path:[^.]*}", "/*/*/{path:[^.]*}").permitAll()

                .anyRequest().authenticated()
            )

            // The chapter redirects browsers to login.jsp / accessDenied.jsp. A SPA fetches
            // JSON, so a redirect to HTML would be useless to it - return the status code and
            // let the Vue router decide where to send the person.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))   // 401 - not logged in
                .accessDeniedHandler((req, res, e) -> res.sendError(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to perform this action."))                 // 403 - wrong role
            )

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, a) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )

            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());

        return http.build();
    }
}
