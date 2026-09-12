package com.testbyte.backend.assessment.dto;

import com.testbyte.backend.domain.Assessment;

import java.time.Instant;
import java.util.List;

public record AssessmentResponse(
        Long id,
        String title,
        List<TestPageResponse> pages,
        Instant createdAt,
        int totalDurationMinutes,
        int questionCount
) {
    public static AssessmentResponse from(Assessment assessment) {
        List<TestPageResponse> pages = assessment.getPages().stream().map(TestPageResponse::from).toList();
        int totalDuration = pages.stream().mapToInt(TestPageResponse::durationMinutes).sum();
        int questionCount = pages.stream().mapToInt(p -> p.questions().size()).sum();
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getTitle(),
                pages,
                assessment.getCreatedAt(),
                totalDuration,
                questionCount
        );
    }
}
