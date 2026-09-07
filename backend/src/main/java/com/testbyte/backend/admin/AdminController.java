package com.testbyte.backend.admin;

import com.testbyte.backend.admin.dto.InviteAdminRequest;
import com.testbyte.backend.admin.dto.PendingRecruiterResponse;
import com.testbyte.backend.admin.dto.SettingsResponse;
import com.testbyte.backend.admin.dto.UpdateSettingsRequest;
import com.testbyte.backend.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/pending-recruiters")
    public List<PendingRecruiterResponse> pendingRecruiters() {
        return adminService.listPendingRecruiters();
    }

    @PostMapping("/pending-recruiters/{id}/approve")
    public void approve(@PathVariable Long id) {
        adminService.approveRecruiter(id);
    }

    @PostMapping("/pending-recruiters/{id}/reject")
    public void reject(@PathVariable Long id) {
        adminService.rejectRecruiter(id);
    }

    @GetMapping("/settings")
    public SettingsResponse getSettings() {
        return adminService.getSettings();
    }

    @PutMapping("/settings")
    public SettingsResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        adminService.updateSettings(request.approverNotificationEmail());
        return adminService.getSettings();
    }

    @PostMapping("/invite-admin")
    public void inviteAdmin(@Valid @RequestBody InviteAdminRequest request,
                             @AuthenticationPrincipal AuthPrincipal principal) {
        adminService.inviteAdmin(request.email(), principal.userId());
    }
}
