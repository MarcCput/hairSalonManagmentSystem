package za.ac.cput.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.cput.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);

    /** Has anyone been given this role yet? Used to bootstrap the first admin exactly once. */
    boolean existsByRole_Name(String roleName);

    /** One login per email address - customers and staff alike. */
    boolean existsByEmail_Value(String email);

    Optional<User> findByEmail_Value(String email);

    List<User> findByRole_Name(String roleName);
}
