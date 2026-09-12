package com.testbyte.backend.assignment;

import com.testbyte.backend.assessment.AssessmentService;
import com.testbyte.backend.assignment.dto.AnswerRequest;
import com.testbyte.backend.assignment.dto.AssignmentReviewResponse;
import com.testbyte.backend.assignment.dto.AssignmentSummaryResponse;
import com.testbyte.backend.assignment.dto.CreateAssignmentRequest;
import com.testbyte.backend.assignment.dto.PublicAssignmentResponse;
import com.testbyte.backend.assignment.dto.ReviewPage;
import com.testbyte.backend.assignment.dto.ReviewQuestionAnswer;
import com.testbyte.backend.assignment.dto.ReviewQuestionOption;
import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.AssignmentAnswer;
import com.testbyte.backend.domain.AssignmentStatus;
import com.testbyte.backend.domain.Role;
import com.testbyte.backend.domain.TestQuestion;
import com.testbyte.backend.domain.TestQuestionOption;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.email.EmailService;
import com.testbyte.backend.exception.BadRequestException;
import com.testbyte.backend.exception.ConflictException;
import com.testbyte.backend.exception.ForbiddenException;
import com.testbyte.backend.exception.GoneException;
import com.testbyte.backend.exception.NotFoundException;
import com.testbyte.backend.repository.AssessmentAssignmentRepository;
import com.testbyte.backend.repository.TestQuestionOptionRepository;
import com.testbyte.backend.repository.TestQuestionRepository;
import com.testbyte.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private final AssessmentAssignmentRepository assignmentRepository;
    private final AssessmentService assessmentService;
    private final UserRepository userRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final TestQuestionOptionRepository testQuestionOptionRepository;
    private final EmailService emailService;
    private final String frontendBaseUrl;
    private final long assignmentLinkExpiryDays;

    public AssignmentService(AssessmentAssignmentRepository assignmentRepository,
                              AssessmentService assessmentService,
                              UserRepository userRepository,
                              TestQuestionRepository testQuestionRepository,
                              TestQuestionOptionRepository testQuestionOptionRepository,
                              EmailService emailService,
                              @Value("${app.frontend-base-url}") String frontendBaseUrl,
                              @Value("${app.assignment-link-expiry-days}") long assignmentLinkExpiryDays) {
        this.assignmentRepository = assignmentRepository;
        this.assessmentService = assessmentService;
        this.userRepository = userRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.testQuestionOptionRepository = testQuestionOptionRepository;
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

    // Not readOnly: markExpiredIfNeeded below opportunistically persists PENDING/IN_PROGRESS -> EXPIRED
    // transitions, and a readOnly transaction's manual flush mode would silently drop that write.
    @Transactional
    public List<AssignmentSummaryResponse> listAssignments(Long currentUserId, Role currentUserRole) {
        List<AssessmentAssignment> assignments;
        if (currentUserRole == Role.ADMIN) {
            assignments = assignmentRepository.findAllByOrderByCreatedAtDesc();
        } else {
            User recruiter = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new NotFoundException("Recruiter not found"));
            assignments = assignmentRepository.findByRecruiterOrderByCreatedAtDesc(recruiter);
        }
        return assignments.stream()
                .peek(this::markExpiredIfNeeded)
                .map(a -> AssignmentSummaryResponse.from(a, frontendBaseUrl))
                .toList();
    }

    @Transactional
    public AssignmentReviewResponse getForReview(Long assignmentId, Long currentUserId, Role currentUserRole) {
        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        boolean isOwner = assignment.getRecruiter().getId().equals(currentUserId);
        if (!isOwner && currentUserRole != Role.ADMIN) {
            throw new ForbiddenException("You do not have access to this assignment");
        }

        markExpiredIfNeeded(assignment);
        return buildReviewResponse(assignment);
    }

    @Transactional
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
    public void submit(UUID token, List<AnswerRequest> answerRequests, String proctoringEvents) {
        AssessmentAssignment assignment = findByToken(token);
        markExpiredIfNeeded(assignment);

        if (assignment.getStatus() == AssignmentStatus.EXPIRED) {
            throw new GoneException("This test link has expired");
        }
        if (assignment.getStatus() == AssignmentStatus.SUBMITTED) {
            throw new ConflictException("This test has already been submitted");
        }

        Long testId = assignment.getAssessment().getId();
        for (AnswerRequest answerRequest : answerRequests) {
            TestQuestion question = testQuestionRepository.findById(answerRequest.questionId())
                    .orElseThrow(() -> new BadRequestException("Unknown question in submission"));
            if (!question.getPage().getTest().getId().equals(testId)) {
                throw new BadRequestException("Question does not belong to this test");
            }

            TestQuestionOption selectedOption = null;
            if (answerRequest.selectedOptionId() != null) {
                selectedOption = testQuestionOptionRepository.findById(answerRequest.selectedOptionId())
                        .orElseThrow(() -> new BadRequestException("Unknown option in submission"));
                if (!selectedOption.getQuestion().getId().equals(question.getId())) {
                    throw new BadRequestException("Selected option does not belong to the given question");
                }
            }

            assignment.getAnswers().add(AssignmentAnswer.builder()
                    .assignment(assignment)
                    .question(question)
                    .answerText(answerRequest.answerText())
                    .selectedOption(selectedOption)
                    .promptSnapshot(question.getPrompt())
                    .questionTypeSnapshot(question.getQuestionType())
                    .languageSnapshot(question.getLanguage())
                    .selectedOptionTextSnapshot(selectedOption != null ? selectedOption.getOptionText() : null)
                    .selectedOptionWasCorrectSnapshot(selectedOption != null ? selectedOption.isCorrect() : null)
                    .build());
        }

        assignment.setStatus(AssignmentStatus.SUBMITTED);
        assignment.setSubmittedAt(Instant.now());
        assignment.setProctoringEventsJson(proctoringEvents);
        assignmentRepository.save(assignment);

        User recruiter = assignment.getRecruiter();
        emailService.sendSubmissionReviewEmail(recruiter.getName(), recruiter.getEmail(), assignment.getId(),
                assignment.getCandidateName(), assignment.getRoleAppliedFor());
    }

    @Transactional
    public AssignmentReviewResponse saveReview(Long assignmentId, Long currentUserId, Role currentUserRole,
                                                String comment) {
        AssessmentAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        boolean isOwner = assignment.getRecruiter().getId().equals(currentUserId);
        if (!isOwner && currentUserRole != Role.ADMIN) {
            throw new ForbiddenException("You do not have access to this assignment");
        }

        User reviewer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        assignment.setReviewComment(comment);
        assignment.setReviewedAt(Instant.now());
        assignment.setReviewedBy(reviewer);
        assignmentRepository.save(assignment);

        return buildReviewResponse(assignment);
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

    /**
     * Merges the test's live page/question structure with the assignment's answers. A question the
     * recruiter has since deleted has no live TestQuestion any more (its answer's question FK went null
     * on delete) -- that answer is reported separately via orphanedAnswers, built entirely from the
     * snapshot columns captured at submit time, rather than dropped or nested under a page that no
     * longer exists.
     */
    private AssignmentReviewResponse buildReviewResponse(AssessmentAssignment assignment) {
        Map<Long, AssignmentAnswer> answersByQuestionId = assignment.getAnswers().stream()
                .filter(a -> a.getQuestion() != null)
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a, b) -> a));

        List<ReviewPage> pages = assignment.getAssessment().getPages().stream()
                .map(page -> new ReviewPage(
                        page.getDurationMinutes(),
                        page.getQuestions().stream()
                                .map(q -> toReviewAnswer(q, answersByQuestionId.get(q.getId())))
                                .toList()))
                .toList();

        List<ReviewQuestionAnswer> orphanedAnswers = assignment.getAnswers().stream()
                .filter(a -> a.getQuestion() == null)
                .map(this::toOrphanedReviewAnswer)
                .toList();

        return new AssignmentReviewResponse(
                assignment.getId(),
                assignment.getCandidateName(),
                assignment.getRoleAppliedFor(),
                assignment.getAssessment().getTitle(),
                pages,
                orphanedAnswers,
                assignment.getStatus(),
                assignment.getCreatedAt(),
                assignment.getStartedAt(),
                assignment.getSubmittedAt(),
                assignment.getProctoringEventsJson(),
                assignment.getReviewComment(),
                assignment.getReviewedAt(),
                assignment.getReviewedBy() != null ? assignment.getReviewedBy().getName() : null
        );
    }

    private ReviewQuestionAnswer toReviewAnswer(TestQuestion question, AssignmentAnswer answer) {
        List<ReviewQuestionOption> options = question.getOptions().stream().map(ReviewQuestionOption::from).toList();
        return new ReviewQuestionAnswer(
                question.getId(),
                question.getQuestionType(),
                question.getPrompt(),
                question.getLanguage(),
                question.getStarterCode(),
                question.getEditorFontSize(),
                question.getEditorFontColor(),
                options,
                answer != null ? answer.getAnswerText() : null,
                answer != null && answer.getSelectedOption() != null ? answer.getSelectedOption().getId() : null,
                answer != null ? answer.getSelectedOptionTextSnapshot() : null,
                answer != null ? answer.getSelectedOptionWasCorrectSnapshot() : null
        );
    }

    private ReviewQuestionAnswer toOrphanedReviewAnswer(AssignmentAnswer answer) {
        return new ReviewQuestionAnswer(
                null,
                answer.getQuestionTypeSnapshot(),
                answer.getPromptSnapshot(),
                answer.getLanguageSnapshot(),
                null,
                null,
                null,
                List.of(),
                answer.getAnswerText(),
                null,
                answer.getSelectedOptionTextSnapshot(),
                answer.getSelectedOptionWasCorrectSnapshot()
        );
    }
}
