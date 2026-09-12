package com.testbyte.backend.assignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAssignmentRequest(
        @NotEmpty @Valid List<AnswerRequest> answers,
        String proctoringEvents
) {
}
