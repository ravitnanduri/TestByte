package com.testbyte.backend.assignment.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewAssignmentRequest(@NotBlank String comment) {
}
