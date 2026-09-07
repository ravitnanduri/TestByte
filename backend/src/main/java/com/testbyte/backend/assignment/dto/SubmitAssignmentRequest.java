package com.testbyte.backend.assignment.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitAssignmentRequest(@NotNull String code, String proctoringEvents) {
}
