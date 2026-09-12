package com.testbyte.backend.assessment.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionOptionRequest(
        @NotBlank String text,
        boolean correct
) {
}
