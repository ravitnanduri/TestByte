package com.testbyte.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AcceptAdminInviteRequest(
        @NotNull UUID token,
        @NotBlank String name,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
) {
}
