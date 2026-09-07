package com.testbyte.backend.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAssignmentRequest(
        @NotNull Long testId,
        @NotBlank String candidateName,
        @NotBlank String roleAppliedFor
) {
}
