package com.testbyte.backend.repository;

import com.testbyte.backend.domain.AdminInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminInviteRepository extends JpaRepository<AdminInvite, Long> {
    Optional<AdminInvite> findByToken(UUID token);
}
