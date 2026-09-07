package com.testbyte.backend.repository;

import com.testbyte.backend.domain.User;
import com.testbyte.backend.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByStatus(UserStatus status);
    boolean existsByRole(com.testbyte.backend.domain.Role role);
}
