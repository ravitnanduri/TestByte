package com.testbyte.backend.assessment.dto;

import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.AssessmentLanguage;

import java.time.Instant;

/**
 * Recruiter/admin-facing view of a test - includes aiTrapPhrase, since anyone who can see this
 * already sees the full starterCode (which contains the trap phrase in plain text anyway) and
 * needs it to edit the test without silently wiping the trap. Never used for the candidate-facing
 * response (see PublicAssignmentResponse), which omits it.
 */
public record AssessmentResponse(
        Long id,
        String title,
        AssessmentLanguage language,
        String instructionsHtml,
        String starterCode,
        String aiTrapPhrase,
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
                assessment.getAiTrapPhrase(),
                assessment.getDurationMinutes(),
                assessment.getCreatedAt()
        );
    }
}
