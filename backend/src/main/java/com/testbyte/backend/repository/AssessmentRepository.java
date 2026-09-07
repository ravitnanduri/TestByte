package com.testbyte.backend.repository;

import com.testbyte.backend.domain.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByActiveTrueOrderByCreatedAtDesc();
}
