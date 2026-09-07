package com.testbyte.backend.email;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * All send methods are @Async: email is best-effort (failures are logged, never thrown back to the
 * caller) and must never block the request thread that triggered it - e.g. an unreachable SMTP server
 * would otherwise hang the approve/reject/submit request itself.
 *
 * Methods take plain strings rather than JPA entities on purpose: entities can carry lazy associations
 * that are only safe to resolve on the original request thread/transaction, and @Async runs on a
 * different thread where that session is gone.
 */
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

    @Async
    public void sendRecruiterPendingApprovalEmail(String approverEmail, String recruiterName, String recruiterEmail) {
        String link = frontendBaseUrl + "/admin/pending-approvals";
        String body = """
                <p>A new recruiter has signed up and is waiting for approval.</p>
                <p><strong>Name:</strong> %s<br/><strong>Email:</strong> %s</p>
                <p><a href="%s">Review pending recruiters</a></p>
                """.formatted(recruiterName, recruiterEmail, link);
        send(approverEmail, "TestByte: New recruiter pending approval", body);
    }

    @Async
    public void sendApprovalDecisionEmail(String recruiterName, String recruiterEmail, boolean approved) {
        String link = frontendBaseUrl + "/login";
        String body = approved
                ? """
                <p>Hi %s,</p>
                <p>Your TestByte recruiter account has been approved. You can now log in.</p>
                <p><a href="%s">Log in to TestByte</a></p>
                """.formatted(recruiterName, link)
                : """
                <p>Hi %s,</p>
                <p>Your TestByte recruiter signup request was not approved. Please contact the administrator for details.</p>
                """.formatted(recruiterName);
        send(recruiterEmail, approved ? "TestByte: Account approved" : "TestByte: Account not approved", body);
    }

    @Async
    public void sendAdminInviteEmail(String email, UUID inviteToken) {
        String link = frontendBaseUrl + "/admin/accept-invite?token=" + inviteToken;
        String body = """
                <p>You have been invited to join TestByte as an administrator.</p>
                <p><a href="%s">Accept invite and set your password</a></p>
                <p>This link expires soon and can only be used once.</p>
                """.formatted(link);
        send(email, "TestByte: Admin invitation", body);
    }

    @Async
    public void sendSubmissionReviewEmail(String recruiterName, String recruiterEmail, Long assignmentId,
                                           String candidateName, String roleAppliedFor) {
        String link = frontendBaseUrl + "/recruiter/assignments/" + assignmentId + "/review";
        String body = """
                <p>Hi %s,</p>
                <p><strong>%s</strong> (%s) has submitted their coding assessment.</p>
                <p><a href="%s">Review the submission</a></p>
                """.formatted(recruiterName, candidateName, roleAppliedFor, link);
        send(recruiterEmail, "TestByte: Submission received - " + candidateName, body);
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
