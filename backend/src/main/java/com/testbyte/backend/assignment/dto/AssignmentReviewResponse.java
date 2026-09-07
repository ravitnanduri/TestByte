package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.domain.AssignmentStatus;

import java.time.Instant;

public record AssignmentReviewResponse(
        Long id,
        String candidateName,
        String roleAppliedFor,
        String testTitle,
        AssessmentLanguage language,
        String instructionsHtml,
        String starterCode,
        String submittedCode,
        boolean possibleAiFlag,
        AssignmentStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant submittedAt
) {
    public static AssignmentReviewResponse from(AssessmentAssignment assignment) {
        return new AssignmentReviewResponse(
                assignment.getId(),
                assignment.getCandidateName(),
                assignment.getRoleAppliedFor(),
                assignment.getAssessment().getTitle(),
                assignment.getAssessment().getLanguage(),
                assignment.getAssessment().getInstructionsHtml(),
                assignment.getAssessment().getStarterCode(),
                assignment.getSubmittedCode(),
                assignment.isPossibleAiFlag(),
                assignment.getStatus(),
                assignment.getCreatedAt(),
                assignment.getStartedAt(),
                assignment.getSubmittedAt()
        );
    }
}
