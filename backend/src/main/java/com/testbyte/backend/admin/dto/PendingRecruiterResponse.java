package com.testbyte.backend.admin.dto;

import com.testbyte.backend.domain.User;

import java.time.Instant;

public record PendingRecruiterResponse(Long id, String name, String email, Instant createdAt) {
    public static PendingRecruiterResponse from(User user) {
        return new PendingRecruiterResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
