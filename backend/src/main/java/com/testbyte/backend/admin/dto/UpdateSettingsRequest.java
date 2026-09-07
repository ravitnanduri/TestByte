package com.testbyte.backend.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateSettingsRequest(@NotBlank @Email String approverNotificationEmail) {
}
