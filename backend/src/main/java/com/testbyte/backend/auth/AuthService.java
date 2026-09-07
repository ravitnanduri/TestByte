package com.testbyte.backend.auth;

import com.testbyte.backend.admin.AppSettingsService;
import com.testbyte.backend.auth.dto.AcceptAdminInviteRequest;
import com.testbyte.backend.auth.dto.AuthResponse;
import com.testbyte.backend.auth.dto.LoginRequest;
import com.testbyte.backend.auth.dto.SignupRequest;
import com.testbyte.backend.auth.dto.SignupResponse;
import com.testbyte.backend.domain.AdminInvite;
import com.testbyte.backend.domain.Role;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.domain.UserStatus;
import com.testbyte.backend.email.EmailService;
import com.testbyte.backend.exception.BadRequestException;
import com.testbyte.backend.exception.ConflictException;
import com.testbyte.backend.exception.ForbiddenException;
import com.testbyte.backend.exception.GoneException;
import com.testbyte.backend.exception.NotFoundException;
import com.testbyte.backend.repository.AdminInviteRepository;
import com.testbyte.backend.repository.UserRepository;
import com.testbyte.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AdminInviteRepository adminInviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AppSettingsService appSettingsService;

    public AuthService(UserRepository userRepository,
                        AdminInviteRepository adminInviteRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        EmailService emailService,
                        AppSettingsService appSettingsService) {
        this.userRepository = userRepository;
        this.adminInviteRepository = adminInviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.appSettingsService = appSettingsService;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        boolean isFirstUser = userRepository.count() == 0;

        User user = User.builder()
                .name(request.name())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(isFirstUser ? Role.ADMIN : Role.RECRUITER)
                .status(isFirstUser ? UserStatus.APPROVED : UserStatus.PENDING)
                .build();

        userRepository.save(user);

        if (isFirstUser) {
            return new SignupResponse(UserStatus.APPROVED,
                    "Account created as the first administrator. You can log in now.");
        }

        emailService.sendRecruiterPendingApprovalEmail(appSettingsService.getApproverNotificationEmail(), user);
        return new SignupResponse(UserStatus.PENDING,
                "Account created. An administrator must approve it before you can log in.");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new ForbiddenException("Your account is still pending administrator approval");
        }
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new ForbiddenException("Your account signup was not approved");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole());
    }

    @Transactional
    public AuthResponse acceptAdminInvite(AcceptAdminInviteRequest request) {
        AdminInvite invite = adminInviteRepository.findByToken(request.token())
                .orElseThrow(() -> new NotFoundException("Invite not found"));

        if (invite.isUsed()) {
            throw new GoneException("This invite has already been used");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new GoneException("This invite has expired");
        }
        if (userRepository.existsByEmailIgnoreCase(invite.getEmail())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.name())
                .email(invite.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ADMIN)
                .status(UserStatus.APPROVED)
                .build();
        userRepository.save(user);

        invite.setUsed(true);
        adminInviteRepository.save(invite);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole());
    }
}
