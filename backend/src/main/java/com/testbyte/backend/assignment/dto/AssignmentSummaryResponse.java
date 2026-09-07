package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.AssignmentStatus;

import java.time.Instant;

public record AssignmentSummaryResponse(
        Long id,
        String candidateName,
        String roleAppliedFor,
        String testTitle,
        AssignmentStatus status,
        String link,
        Instant createdAt,
        Instant expiresAt,
        String recruiterName,
        String recruiterEmail,
        boolean reviewed
) {
    public static AssignmentSummaryResponse from(AssessmentAssignment assignment, String frontendBaseUrl) {
        return new AssignmentSummaryResponse(
                assignment.getId(),
                assignment.getCandidateName(),
                assignment.getRoleAppliedFor(),
                assignment.getAssessment().getTitle(),
                assignment.getStatus(),
                frontendBaseUrl + "/test/" + assignment.getToken(),
                assignment.getCreatedAt(),
                assignment.getExpiresAt(),
                assignment.getRecruiter().getName(),
                assignment.getRecruiter().getEmail(),
                assignment.getReviewedAt() != null
        );
    }
}
