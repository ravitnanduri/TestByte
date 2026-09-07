package com.testbyte.backend.auth;

import com.testbyte.backend.admin.AppSettingsService;
import com.testbyte.backend.auth.dto.LoginRequest;
import com.testbyte.backend.auth.dto.SignupRequest;
import com.testbyte.backend.auth.dto.SignupResponse;
import com.testbyte.backend.domain.Role;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.domain.UserStatus;
import com.testbyte.backend.email.EmailService;
import com.testbyte.backend.exception.BadRequestException;
import com.testbyte.backend.exception.ConflictException;
import com.testbyte.backend.exception.ForbiddenException;
import com.testbyte.backend.repository.AdminInviteRepository;
import com.testbyte.backend.repository.UserRepository;
import com.testbyte.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AdminInviteRepository adminInviteRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private AppSettingsService appSettingsService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService("test-only-secret-key-that-is-at-least-32-bytes-long", 60);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, adminInviteRepository, passwordEncoder, jwtService,
                emailService, appSettingsService);
    }

    @Test
    void firstEverSignupBecomesApprovedAdmin() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);

        SignupResponse response = authService.signup(new SignupRequest("Ravi", "ravi@example.com", "password123"));

        assertThat(response.status()).isEqualTo(UserStatus.APPROVED);
        verify(userRepository).save(argThat(u -> u.getRole() == Role.ADMIN && u.getStatus() == UserStatus.APPROVED));
        verifyNoInteractions(emailService);
    }

    @Test
    void laterSignupsArePendingRecruitersAndNotifyApprover() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);
        when(appSettingsService.getApproverNotificationEmail()).thenReturn("approver@example.com");

        SignupResponse response = authService.signup(new SignupRequest("Grace", "grace@example.com", "password123"));

        assertThat(response.status()).isEqualTo(UserStatus.PENDING);
        verify(userRepository).save(argThat(u -> u.getRole() == Role.RECRUITER && u.getStatus() == UserStatus.PENDING));
        verify(emailService).sendRecruiterPendingApprovalEmail(
                eq("approver@example.com"), eq("Grace"), eq("grace@example.com"));
    }

    @Test
    void signupRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("Dup", "dup@example.com", "password123")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginRejectsPendingUser() {
        User pending = User.builder().id(1L).name("Pending").email("p@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.RECRUITER).status(UserStatus.PENDING).build();
        when(userRepository.findByEmailIgnoreCase("p@example.com")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> authService.login(new LoginRequest("p@example.com", "password123")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        User approved = User.builder().id(1L).name("Approved").email("a@example.com")
                .passwordHash(passwordEncoder.encode("correct-password"))
                .role(Role.RECRUITER).status(UserStatus.APPROVED).build();
        when(userRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@example.com", "wrong-password")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void loginSucceedsForApprovedUserWithCorrectPassword() {
        User approved = User.builder().id(1L).name("Approved").email("a@example.com")
                .passwordHash(passwordEncoder.encode("correct-password"))
                .role(Role.RECRUITER).status(UserStatus.APPROVED).build();
        when(userRepository.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(approved));

        var response = authService.login(new LoginRequest("a@example.com", "correct-password"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.role()).isEqualTo(Role.RECRUITER);
    }
}
