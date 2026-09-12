package com.testbyte.backend.assignment;

import com.testbyte.backend.assessment.AssessmentService;
import com.testbyte.backend.assignment.dto.AnswerRequest;
import com.testbyte.backend.assignment.dto.AssignmentSummaryResponse;
import com.testbyte.backend.assignment.dto.PublicAssignmentResponse;
import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.domain.AssignmentStatus;
import com.testbyte.backend.domain.QuestionType;
import com.testbyte.backend.domain.Role;
import com.testbyte.backend.domain.TestPage;
import com.testbyte.backend.domain.TestQuestion;
import com.testbyte.backend.domain.TestQuestionOption;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.email.EmailService;
import com.testbyte.backend.exception.GoneException;
import com.testbyte.backend.repository.AssessmentAssignmentRepository;
import com.testbyte.backend.repository.TestQuestionOptionRepository;
import com.testbyte.backend.repository.TestQuestionRepository;
import com.testbyte.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssessmentAssignmentRepository assignmentRepository;
    @Mock
    private AssessmentService assessmentService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TestQuestionRepository testQuestionRepository;
    @Mock
    private TestQuestionOptionRepository testQuestionOptionRepository;
    @Mock
    private EmailService emailService;

    private AssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AssignmentService(assignmentRepository, assessmentService, userRepository,
                testQuestionRepository, testQuestionOptionRepository, emailService,
                "http://localhost:4200", 7);
    }

    /** One test, one page, one CODE question (id 10) -- the common single-question shape. */
    private Assessment sampleAssessment() {
        Assessment assessment = Assessment.builder().id(1L).title("Order Total Calculation").active(true).build();
        TestPage page = TestPage.builder().id(1L).test(assessment).pageOrder(1).durationMinutes(10)
                .questions(new ArrayList<>()).build();
        TestQuestion question = TestQuestion.builder().id(10L).page(page).questionOrder(1)
                .questionType(QuestionType.CODE).prompt("instructions").language(AssessmentLanguage.JAVA)
                .starterCode("public double calculateTotal() {}").options(new ArrayList<>()).build();
        page.getQuestions().add(question);
        assessment.getPages().add(page);
        return assessment;
    }

    private AssessmentAssignment assignment(Assessment assessment, AssignmentStatus status, Instant expiresAt) {
        return AssessmentAssignment.builder()
                .id(1L)
                .assessment(assessment)
                .recruiter(User.builder().id(1L).name("Recruiter").email("r@example.com").build())
                .candidateName("Jane Candidate")
                .roleAppliedFor("Backend Engineer")
                .token(UUID.randomUUID())
                .status(status)
                .expiresAt(expiresAt)
                .answers(new ArrayList<>())
                .build();
    }

    @Test
    void submitMarksAssignmentSubmittedAndEmailsRecruiter() {
        Assessment assessment = sampleAssessment();
        TestQuestion question = assessment.getPages().get(0).getQuestions().get(0);
        AssessmentAssignment a = assignment(assessment, AssignmentStatus.IN_PROGRESS, Instant.now().plus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findByToken(a.getToken())).thenReturn(Optional.of(a));
        when(testQuestionRepository.findById(10L)).thenReturn(Optional.of(question));

        service.submit(a.getToken(), List.of(new AnswerRequest(10L, "public double computeTotal() { return 0; }", null)), "[]");

        assertThat(a.getStatus()).isEqualTo(AssignmentStatus.SUBMITTED);
        assertThat(a.getAnswers()).hasSize(1);
        assertThat(a.getAnswers().get(0).getAnswerText()).isEqualTo("public double computeTotal() { return 0; }");
        verify(emailService).sendSubmissionReviewEmail(any(), any(), any(), any(), any());
    }

    @Test
    void submitStoresProctoringEvents() {
        Assessment assessment = sampleAssessment();
        TestQuestion question = assessment.getPages().get(0).getQuestions().get(0);
        AssessmentAssignment a = assignment(assessment, AssignmentStatus.IN_PROGRESS, Instant.now().plus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findByToken(a.getToken())).thenReturn(Optional.of(a));
        when(testQuestionRepository.findById(10L)).thenReturn(Optional.of(question));

        String events = "[{\"type\":\"tab_switch\",\"timestamp\":\"2026-01-01T00:00:00Z\"}]";
        service.submit(a.getToken(), List.of(new AnswerRequest(10L, "code", null)), events);

        assertThat(a.getProctoringEventsJson()).isEqualTo(events);
    }

    @Test
    void submitStoresAnswersForEachQuestionType() {
        Assessment assessment = Assessment.builder().id(1L).title("Multi-question test").active(true).build();
        TestPage page = TestPage.builder().id(1L).test(assessment).pageOrder(1).durationMinutes(10)
                .questions(new ArrayList<>()).build();
        TestQuestion mcq = TestQuestion.builder().id(20L).page(page).questionOrder(1)
                .questionType(QuestionType.MULTIPLE_CHOICE).prompt("Pick one").options(new ArrayList<>()).build();
        TestQuestionOption correctOption = TestQuestionOption.builder().id(200L).question(mcq).optionOrder(1)
                .optionText("Right answer").correct(true).build();
        mcq.getOptions().add(correctOption);
        TestQuestion text = TestQuestion.builder().id(21L).page(page).questionOrder(2)
                .questionType(QuestionType.TEXT).prompt("Explain yourself").options(new ArrayList<>()).build();
        page.getQuestions().add(mcq);
        page.getQuestions().add(text);
        assessment.getPages().add(page);

        AssessmentAssignment a = assignment(assessment, AssignmentStatus.IN_PROGRESS, Instant.now().plus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findByToken(a.getToken())).thenReturn(Optional.of(a));
        when(testQuestionRepository.findById(20L)).thenReturn(Optional.of(mcq));
        when(testQuestionRepository.findById(21L)).thenReturn(Optional.of(text));
        when(testQuestionOptionRepository.findById(200L)).thenReturn(Optional.of(correctOption));

        service.submit(a.getToken(), List.of(
                new AnswerRequest(20L, null, 200L),
                new AnswerRequest(21L, "My explanation", null)
        ), "[]");

        assertThat(a.getAnswers()).hasSize(2);
        var mcqAnswer = a.getAnswers().stream().filter(ans -> ans.getQuestion().getId().equals(20L)).findFirst().orElseThrow();
        assertThat(mcqAnswer.getSelectedOption().getId()).isEqualTo(200L);
        assertThat(mcqAnswer.getSelectedOptionTextSnapshot()).isEqualTo("Right answer");
        assertThat(mcqAnswer.getSelectedOptionWasCorrectSnapshot()).isTrue();
        var textAnswer = a.getAnswers().stream().filter(ans -> ans.getQuestion().getId().equals(21L)).findFirst().orElseThrow();
        assertThat(textAnswer.getAnswerText()).isEqualTo("My explanation");
    }

    @Test
    void expiredLinkCannotBeStarted() {
        Assessment assessment = sampleAssessment();
        AssessmentAssignment a = assignment(assessment, AssignmentStatus.PENDING, Instant.now().minus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findByToken(a.getToken())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.start(a.getToken())).isInstanceOf(GoneException.class);
        assertThat(a.getStatus()).isEqualTo(AssignmentStatus.EXPIRED);
    }

    @Test
    void gettingPublicAssignmentLazilyMarksExpired() {
        Assessment assessment = sampleAssessment();
        AssessmentAssignment a = assignment(assessment, AssignmentStatus.PENDING, Instant.now().minus(1, ChronoUnit.HOURS));
        when(assignmentRepository.findByToken(a.getToken())).thenReturn(Optional.of(a));

        PublicAssignmentResponse response = service.getPublicByToken(a.getToken());

        assertThat(response.status()).isEqualTo(AssignmentStatus.EXPIRED);
    }

    @Test
    void recruiterOnlySeesTheirOwnAssignments() {
        User recruiter = User.builder().id(1L).name("Recruiter").email("r@example.com").build();
        AssessmentAssignment a = assignment(sampleAssessment(), AssignmentStatus.PENDING, Instant.now().plus(1, ChronoUnit.DAYS));
        when(userRepository.findById(1L)).thenReturn(Optional.of(recruiter));
        when(assignmentRepository.findByRecruiterOrderByCreatedAtDesc(recruiter)).thenReturn(List.of(a));

        List<AssignmentSummaryResponse> result = service.listAssignments(1L, Role.RECRUITER);

        assertThat(result).hasSize(1);
        verify(assignmentRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void adminSeesAllAssignmentsAcrossRecruiters() {
        AssessmentAssignment a1 = assignment(sampleAssessment(), AssignmentStatus.PENDING, Instant.now().plus(1, ChronoUnit.DAYS));
        AssessmentAssignment a2 = assignment(sampleAssessment(), AssignmentStatus.PENDING, Instant.now().plus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(a1, a2));

        List<AssignmentSummaryResponse> result = service.listAssignments(99L, Role.ADMIN);

        assertThat(result).hasSize(2);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void saveReviewByAssignmentOwnerSucceeds() {
        AssessmentAssignment a = assignment(sampleAssessment(), AssignmentStatus.SUBMITTED, Instant.now().plus(1, ChronoUnit.DAYS));
        User owner = a.getRecruiter();
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(a));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        var response = service.saveReview(1L, 1L, Role.RECRUITER, "Looks good");

        assertThat(response.reviewComment()).isEqualTo("Looks good");
        assertThat(a.getReviewedAt()).isNotNull();
        assertThat(a.getReviewedBy()).isEqualTo(owner);
    }

    @Test
    void saveReviewByUnrelatedRecruiterIsForbidden() {
        AssessmentAssignment a = assignment(sampleAssessment(), AssignmentStatus.SUBMITTED, Instant.now().plus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.saveReview(1L, 42L, Role.RECRUITER, "note"))
                .isInstanceOf(com.testbyte.backend.exception.ForbiddenException.class);
    }
}
