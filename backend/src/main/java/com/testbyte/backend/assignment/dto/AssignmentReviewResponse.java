package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.AssignmentStatus;

import java.time.Instant;
import java.util.List;

/**
 * Built by AssignmentService.getForReview (not a plain static "from", since it merges the live test
 * structure with the assignment's answers) rather than here.
 */
public record AssignmentReviewResponse(
        Long id,
        String candidateName,
        String roleAppliedFor,
        String testTitle,
        List<ReviewPage> pages,
        List<ReviewQuestionAnswer> orphanedAnswers,
        AssignmentStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant submittedAt,
        String proctoringEvents,
        String reviewComment,
        Instant reviewedAt,
        String reviewedByName
) {
}
