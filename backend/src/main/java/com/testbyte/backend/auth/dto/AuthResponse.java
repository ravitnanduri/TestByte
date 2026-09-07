package com.testbyte.backend.auth.dto;

import com.testbyte.backend.domain.Role;

public record AuthResponse(String token, String name, String email, Role role) {
}
