package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.domain.AssignmentStatus;

import java.time.Instant;

public record PublicAssignmentResponse(
        String candidateName,
        String roleAppliedFor,
        String testTitle,
        AssessmentLanguage language,
        String instructionsHtml,
        String starterCode,
        Integer hiddenLineNumber,
        Integer durationMinutes,
        AssignmentStatus status,
        Instant startedAt,
        Instant expiresAt
) {
    public static PublicAssignmentResponse from(AssessmentAssignment assignment) {
        return new PublicAssignmentResponse(
                assignment.getCandidateName(),
                assignment.getRoleAppliedFor(),
                assignment.getAssessment().getTitle(),
                assignment.getAssessment().getLanguage(),
                assignment.getAssessment().getInstructionsHtml(),
                assignment.getAssessment().getStarterCode(),
                assignment.getAssessment().findAiTrapLineNumber(),
                assignment.getAssessment().getDurationMinutes(),
                assignment.getStatus(),
                assignment.getStartedAt(),
                assignment.getExpiresAt()
        );
    }
}
