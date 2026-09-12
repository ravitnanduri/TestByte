package com.testbyte.backend.assignment.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerRequest(
        @NotNull Long questionId,
        String answerText,
        Long selectedOptionId
) {
}
