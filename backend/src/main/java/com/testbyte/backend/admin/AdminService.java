package com.testbyte.backend.admin;

import com.testbyte.backend.admin.dto.PendingRecruiterResponse;
import com.testbyte.backend.admin.dto.SettingsResponse;
import com.testbyte.backend.domain.AdminInvite;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.domain.UserStatus;
import com.testbyte.backend.email.EmailService;
import com.testbyte.backend.exception.ConflictException;
import com.testbyte.backend.exception.NotFoundException;
import com.testbyte.backend.repository.AdminInviteRepository;
import com.testbyte.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AdminInviteRepository adminInviteRepository;
    private final AppSettingsService appSettingsService;
    private final EmailService emailService;
    private final long adminInviteExpiryHours;

    public AdminService(UserRepository userRepository,
                         AdminInviteRepository adminInviteRepository,
                         AppSettingsService appSettingsService,
                         EmailService emailService,
                         @Value("${app.admin-invite-expiry-hours}") long adminInviteExpiryHours) {
        this.userRepository = userRepository;
        this.adminInviteRepository = adminInviteRepository;
        this.appSettingsService = appSettingsService;
        this.emailService = emailService;
        this.adminInviteExpiryHours = adminInviteExpiryHours;
    }

    public List<PendingRecruiterResponse> listPendingRecruiters() {
        return userRepository.findByStatus(UserStatus.PENDING).stream()
                .map(PendingRecruiterResponse::from)
                .toList();
    }

    @Transactional
    public void approveRecruiter(Long userId) {
        User user = findPendingUser(userId);
        user.setStatus(UserStatus.APPROVED);
        userRepository.save(user);
        emailService.sendApprovalDecisionEmail(user.getName(), user.getEmail(), true);
    }

    @Transactional
    public void rejectRecruiter(Long userId) {
        User user = findPendingUser(userId);
        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);
        emailService.sendApprovalDecisionEmail(user.getName(), user.getEmail(), false);
    }

    private User findPendingUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getStatus() != UserStatus.PENDING) {
            throw new ConflictException("This user is not pending approval");
        }
        return user;
    }

    public SettingsResponse getSettings() {
        return new SettingsResponse(appSettingsService.getApproverNotificationEmail());
    }

    public void updateSettings(String approverNotificationEmail) {
        appSettingsService.setApproverNotificationEmail(approverNotificationEmail);
    }

    @Transactional
    public void inviteAdmin(String email, Long invitedByUserId) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User invitedBy = userRepository.findById(invitedByUserId)
                .orElseThrow(() -> new NotFoundException("Inviting user not found"));
        AdminInvite invite = AdminInvite.builder()
                .email(email.toLowerCase())
                .invitedBy(invitedBy)
                .expiresAt(Instant.now().plus(adminInviteExpiryHours, ChronoUnit.HOURS))
                .build();
        adminInviteRepository.save(invite);
        emailService.sendAdminInviteEmail(invite.getEmail(), invite.getToken());
    }
}
