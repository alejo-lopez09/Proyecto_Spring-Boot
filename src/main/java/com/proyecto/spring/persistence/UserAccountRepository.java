package com.proyecto.spring.persistence;

import com.proyecto.spring.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
