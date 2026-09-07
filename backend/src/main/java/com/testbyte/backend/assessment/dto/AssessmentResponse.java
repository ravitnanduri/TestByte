package com.testbyte.backend.assessment.dto;

import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.AssessmentLanguage;

import java.time.Instant;

public record AssessmentResponse(
        Long id,
        String title,
        AssessmentLanguage language,
        String instructionsHtml,
        String starterCode,
        Integer durationMinutes,
        Instant createdAt
) {
    public static AssessmentResponse from(Assessment assessment) {
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getTitle(),
                assessment.getLanguage(),
                assessment.getInstructionsHtml(),
                assessment.getStarterCode(),
                assessment.getDurationMinutes(),
                assessment.getCreatedAt()
        );
    }
}
