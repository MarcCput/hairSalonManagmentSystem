package za.ac.cput.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.cput.domain.Stylist;

import java.util.List;
import java.util.Optional;

public interface StylistRepository extends JpaRepository<Stylist, String> {
    List<Stylist> findByIsActiveTrue();

    /** A stylist's login is linked to their Stylist row by shared email, like customers. */
    Optional<Stylist> findByEmail_Value(String email);
}
