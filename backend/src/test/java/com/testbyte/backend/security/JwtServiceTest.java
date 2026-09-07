package com.testbyte.backend.security;

import com.testbyte.backend.domain.Role;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.domain.UserStatus;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-only-secret-key-that-is-at-least-32-bytes-long", 60);

    private User sampleUser() {
        return User.builder()
                .id(42L)
                .name("Ada Lovelace")
                .email("ada@example.com")
                .role(Role.RECRUITER)
                .status(UserStatus.APPROVED)
                .build();
    }

    @Test
    void roundTripsClaimsThroughGenerateAndParse() {
        String token = jwtService.generateToken(sampleUser());

        AuthPrincipal principal = jwtService.parseToken(token);

        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.email()).isEqualTo("ada@example.com");
        assertThat(principal.name()).isEqualTo("Ada Lovelace");
        assertThat(principal.role()).isEqualTo(Role.RECRUITER);
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService other = new JwtService("a-completely-different-secret-key-of-32-bytes+", 60);
        String token = other.generateToken(sampleUser());

        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(SignatureException.class);
    }
}
