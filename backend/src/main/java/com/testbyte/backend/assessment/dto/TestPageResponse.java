package com.testbyte.backend.assessment.dto;

import com.testbyte.backend.domain.TestPage;

import java.util.List;

public record TestPageResponse(
        Long id,
        Integer durationMinutes,
        List<QuestionResponse> questions
) {
    public static TestPageResponse from(TestPage page) {
        return new TestPageResponse(
                page.getId(),
                page.getDurationMinutes(),
                page.getQuestions().stream().map(QuestionResponse::from).toList()
        );
    }
}
