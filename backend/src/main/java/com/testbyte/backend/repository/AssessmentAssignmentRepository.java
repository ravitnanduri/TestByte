package com.testbyte.backend.repository;

import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAssignmentRepository extends JpaRepository<AssessmentAssignment, Long> {
    Optional<AssessmentAssignment> findByToken(UUID token);
    List<AssessmentAssignment> findByRecruiterOrderByCreatedAtDesc(User recruiter);
}
