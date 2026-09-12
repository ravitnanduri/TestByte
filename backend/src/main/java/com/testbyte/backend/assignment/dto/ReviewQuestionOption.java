package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.TestQuestionOption;

public record ReviewQuestionOption(Long id, String text, boolean correct) {
    public static ReviewQuestionOption from(TestQuestionOption option) {
        return new ReviewQuestionOption(option.getId(), option.getOptionText(), option.isCorrect());
    }
}
