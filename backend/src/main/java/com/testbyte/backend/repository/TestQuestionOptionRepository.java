package com.testbyte.backend.repository;

import com.testbyte.backend.domain.TestQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestQuestionOptionRepository extends JpaRepository<TestQuestionOption, Long> {
}
