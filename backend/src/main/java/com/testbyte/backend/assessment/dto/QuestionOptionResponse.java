package com.testbyte.backend.assessment.dto;

import com.testbyte.backend.domain.TestQuestionOption;

public record QuestionOptionResponse(
        Long id,
        String text,
        boolean correct
) {
    public static QuestionOptionResponse from(TestQuestionOption option) {
        return new QuestionOptionResponse(option.getId(), option.getOptionText(), option.isCorrect());
    }
}
