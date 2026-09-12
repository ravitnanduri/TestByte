package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.AssignmentStatus;

import java.time.Instant;
import java.util.List;

public record PublicAssignmentResponse(
        String candidateName,
        String roleAppliedFor,
        String testTitle,
        List<PublicPage> pages,
        AssignmentStatus status,
        Instant startedAt,
        Instant expiresAt
) {
    public static PublicAssignmentResponse from(AssessmentAssignment assignment) {
        return new PublicAssignmentResponse(
                assignment.getCandidateName(),
                assignment.getRoleAppliedFor(),
                assignment.getAssessment().getTitle(),
                assignment.getAssessment().getPages().stream().map(PublicPage::from).toList(),
                assignment.getStatus(),
                assignment.getStartedAt(),
                assignment.getExpiresAt()
        );
    }
}
