package com.testbyte.backend.email;

import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.User;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendBaseUrl;

    public EmailService(JavaMailSender mailSender,
                         @Value("${app.mail.from}") String fromAddress,
                         @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendRecruiterPendingApprovalEmail(String approverEmail, User pendingRecruiter) {
        String link = frontendBaseUrl + "/admin/pending-approvals";
        String body = """
                <p>A new recruiter has signed up and is waiting for approval.</p>
                <p><strong>Name:</strong> %s<br/><strong>Email:</strong> %s</p>
                <p><a href="%s">Review pending recruiters</a></p>
                """.formatted(pendingRecruiter.getName(), pendingRecruiter.getEmail(), link);
        send(approverEmail, "TestByte: New recruiter pending approval", body);
    }

    public void sendApprovalDecisionEmail(User recruiter, boolean approved) {
        String link = frontendBaseUrl + "/login";
        String body = approved
                ? """
                <p>Hi %s,</p>
                <p>Your TestByte recruiter account has been approved. You can now log in.</p>
                <p><a href="%s">Log in to TestByte</a></p>
                """.formatted(recruiter.getName(), link)
                : """
                <p>Hi %s,</p>
                <p>Your TestByte recruiter signup request was not approved. Please contact the administrator for details.</p>
                """.formatted(recruiter.getName());
        send(recruiter.getEmail(), approved ? "TestByte: Account approved" : "TestByte: Account not approved", body);
    }

    public void sendAdminInviteEmail(String email, UUID inviteToken) {
        String link = frontendBaseUrl + "/admin/accept-invite?token=" + inviteToken;
        String body = """
                <p>You have been invited to join TestByte as an administrator.</p>
                <p><a href="%s">Accept invite and set your password</a></p>
                <p>This link expires soon and can only be used once.</p>
                """.formatted(link);
        send(email, "TestByte: Admin invitation", body);
    }

    public void sendSubmissionReviewEmail(User recruiter, AssessmentAssignment assignment) {
        String link = frontendBaseUrl + "/recruiter/assignments/" + assignment.getId() + "/review";
        String body = """
                <p>Hi %s,</p>
                <p><strong>%s</strong> (%s) has submitted their coding assessment.</p>
                <p><a href="%s">Review the submission</a></p>
                """.formatted(recruiter.getName(), assignment.getCandidateName(), assignment.getRoleAppliedFor(), link);
        send(recruiter.getEmail(), "TestByte: Submission received - " + assignment.getCandidateName(), body);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }
}
