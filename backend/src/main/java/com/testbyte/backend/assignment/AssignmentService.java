package com.testbyte.backend.assignment;

import com.testbyte.backend.assessment.AssessmentService;
import com.testbyte.backend.assignment.dto.AssignmentReviewResponse;
import com.testbyte.backend.assignment.dto.AssignmentSummaryResponse;
import com.testbyte.backend.assignment.dto.CreateAssignmentRequest;
import com.testbyte.backend.assignment.dto.PublicAssignmentResponse;
import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.AssignmentStatus;
import com.testbyte.backend.domain.Role;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.email.EmailService;
import com.testbyte.backend.exception.ConflictException;
import com.testbyte.backend.exception.ForbiddenException;
import com.testbyte.backend.exception.GoneException;
import com.testbyte.backend.exception.NotFoundException;
import com.testbyte.backend.repository.AssessmentAssignmentRepository;
import com.testbyte.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AssignmentService {

    private final AssessmentAssignmentRepository assignmentRepository;
    private final AssessmentService assessmentService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String frontendBaseUrl;
    private final long assignmentLinkExpiryDays;

    public AssignmentService(AssessmentAssignmentRepository assignmentRepository,
                              AssessmentService assessmentService,
                              UserRepository userRepository,
                              EmailService emailService,
                              @Value("${app.frontend-base-url}") String frontendBaseUrl,
                              @Value("${app.assignment-link-expiry-days}") long assignmentLinkExpiryDays) {
        this.assignmentRepository = assignmentRepository;
        this.assessmentService = assessmentService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.assignmentLinkExpiryDays = assignmentLinkExpiryDays;
    }

    @Transactional
    public AssignmentSummaryResponse create(CreateAssignmentRequest request, Long recruiterId) {
        Assessment assessment = assessmentService.getById(request.testId());
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new NotFoundException("Recruiter not found"));

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .assessment(assessment)
                .recruiter(recruiter)
                .candidateName(request.candidateName())
                .roleAppliedFor(request.roleAppliedFor())
                .status(AssignmentStatus.PENDING)
                .expiresAt(Instant.now().plus(assignmentLinkExpiryDays, ChronoUnit.DAYS))
                .build();

        assignmentRepository.save(assignment);
        return AssignmentSummaryResponse.from(assignment, frontendBaseUrl);
    }

    public List<AssignmentSummaryResponse> listForRecruiter(Long recruiterId) {
        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new NotFoundException("Recruiter not found"));
        return assignmentRepository.findByRecruiterOrderByCreatedAtDesc(recruiter).stream()
                .peek(this::markExpiredIfNeeded)
                .map(a -> AssignmentSummaryResponse.from(a, frontendBaseUrl))
                .toList();
    }

    public AssignmentReviewResponse getForReview(Long assignmentId, Long currentUserId, Role currentUserRole) {
        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        boolean isOwner = assignment.getRecruiter().getId().equals(currentUserId);
        if (!isOwner && currentUserRole != Role.ADMIN) {
            throw new ForbiddenException("You do not have access to this assignment");
        }

        markExpiredIfNeeded(assignment);
        return AssignmentReviewResponse.from(assignment);
    }

    public PublicAssignmentResponse getPublicByToken(UUID token) {
        AssessmentAssignment assignment = findByToken(token);
        markExpiredIfNeeded(assignment);
        return PublicAssignmentResponse.from(assignment);
    }

    @Transactional
    public PublicAssignmentResponse start(UUID token) {
        AssessmentAssignment assignment = findByToken(token);
        markExpiredIfNeeded(assignment);

        if (assignment.getStatus() == AssignmentStatus.EXPIRED) {
            throw new GoneException("This test link has expired");
        }
        if (assignment.getStatus() == AssignmentStatus.SUBMITTED) {
            throw new ConflictException("This test has already been submitted");
        }
        if (assignment.getStatus() == AssignmentStatus.PENDING) {
            assignment.setStatus(AssignmentStatus.IN_PROGRESS);
            assignment.setStartedAt(Instant.now());
            assignmentRepository.save(assignment);
        }

        return PublicAssignmentResponse.from(assignment);
    }

    @Transactional
    public void submit(UUID token, String code) {
        AssessmentAssignment assignment = findByToken(token);
        markExpiredIfNeeded(assignment);

        if (assignment.getStatus() == AssignmentStatus.EXPIRED) {
            throw new GoneException("This test link has expired");
        }
        if (assignment.getStatus() == AssignmentStatus.SUBMITTED) {
            throw new ConflictException("This test has already been submitted");
        }

        String trapPhrase = assignment.getAssessment().getAiTrapPhrase();
        boolean possibleAiFlag = trapPhrase != null && !trapPhrase.isBlank() && code.contains(trapPhrase);

        assignment.setSubmittedCode(code);
        assignment.setStatus(AssignmentStatus.SUBMITTED);
        assignment.setSubmittedAt(Instant.now());
        assignment.setPossibleAiFlag(possibleAiFlag);
        assignmentRepository.save(assignment);

        emailService.sendSubmissionReviewEmail(assignment.getRecruiter(), assignment);
    }

    private AssessmentAssignment findByToken(UUID token) {
        return assignmentRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Test link not found"));
    }

    /**
     * Lazily flips PENDING/IN_PROGRESS assignments to EXPIRED once their deadline passes,
     * avoiding the need for a separate scheduled job at this scale.
     */
    private void markExpiredIfNeeded(AssessmentAssignment assignment) {
        if ((assignment.getStatus() == AssignmentStatus.PENDING || assignment.getStatus() == AssignmentStatus.IN_PROGRESS)
                && Instant.now().isAfter(assignment.getExpiresAt())) {
            assignment.setStatus(AssignmentStatus.EXPIRED);
            assignmentRepository.save(assignment);
        }
    }
}
