package com.testbyte.backend.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TestPageRequest(
        @NotNull @Min(1) Integer durationMinutes,
        @NotEmpty @Valid List<QuestionRequest> questions
) {
}
