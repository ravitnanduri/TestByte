package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.TestQuestionOption;

// Deliberately omits "correct" -- this is what the candidate sees, revealing the answer would defeat
// the point of the question.
public record PublicQuestionOption(Long id, String text) {
    public static PublicQuestionOption from(TestQuestionOption option) {
        return new PublicQuestionOption(option.getId(), option.getOptionText());
    }
}
