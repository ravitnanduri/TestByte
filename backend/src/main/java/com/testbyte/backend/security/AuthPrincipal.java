package com.testbyte.backend.security;

import com.testbyte.backend.domain.Role;

public record AuthPrincipal(Long userId, String email, String name, Role role) {
}
