package com.testbyte.backend.auth;

import com.testbyte.backend.auth.dto.AcceptAdminInviteRequest;
import com.testbyte.backend.auth.dto.AuthResponse;
import com.testbyte.backend.auth.dto.LoginRequest;
import com.testbyte.backend.auth.dto.SignupRequest;
import com.testbyte.backend.auth.dto.SignupResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/accept-admin-invite")
    public AuthResponse acceptAdminInvite(@Valid @RequestBody AcceptAdminInviteRequest request) {
        return authService.acceptAdminInvite(request);
    }
}
