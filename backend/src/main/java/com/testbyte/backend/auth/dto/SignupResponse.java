package com.testbyte.backend.auth.dto;

import com.testbyte.backend.domain.UserStatus;

public record SignupResponse(UserStatus status, String message) {
}
