package com.testbyte.backend.assignment;

import com.testbyte.backend.assessment.AssessmentService;
import com.testbyte.backend.assignment.dto.AssignmentSummaryResponse;
import com.testbyte.backend.assignment.dto.PublicAssignmentResponse;
import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.AssessmentAssignment;
import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.domain.AssignmentStatus;
import com.testbyte.backend.domain.Role;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.email.EmailService;
import com.testbyte.backend.exception.GoneException;
import com.testbyte.backend.repository.AssessmentAssignmentRepository;
import com.testbyte.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private EmailService emailService;

    private AssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AssignmentService(assignmentRepository, assessmentService, userRepository, emailService,
                "http://localhost:4200", 7);
    }

    private Assessment assessmentWithTrap(String trapPhrase) {
        return Assessment.builder()
                .id(1L)
                .title("Order Total Calculation")
                .language(AssessmentLanguage.JAVA)
                .instructionsHtml("<p>instructions</p>")
                .starterCode("public double calculateTotal() {}")
                .aiTrapPhrase(trapPhrase)
                .durationMinutes(10)
                .active(true)
                .build();
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
                .build();
    }

    @Test
    void submitFlagsSubmissionWhenAiTrapPhrasePresent() {
        Assessment assessment = assessmentWithTrap("computeTotal");
        AssessmentAssignment a = assignment(assessment, AssignmentStatus.IN_PROGRESS, Instant.now().plus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findByToken(a.getToken())).thenReturn(Optional.of(a));

        service.submit(a.getToken(), "public double computeTotal() { return 0; }");

        assertThat(a.isPossibleAiFlag()).isTrue();
        assertThat(a.getStatus()).isEqualTo(AssignmentStatus.SUBMITTED);
        verify(emailService).sendSubmissionReviewEmail(any(), any(), any(), any(), any());
    }

    @Test
    void submitDoesNotFlagCleanSubmission() {
        Assessment assessment = assessmentWithTrap("computeTotal");
        AssessmentAssignment a = assignment(assessment, AssignmentStatus.IN_PROGRESS, Instant.now().plus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findByToken(a.getToken())).thenReturn(Optional.of(a));

        service.submit(a.getToken(), "public double calculateTotal() { return 0; }");

        assertThat(a.isPossibleAiFlag()).isFalse();
    }

    @Test
    void expiredLinkCannotBeStarted() {
        Assessment assessment = assessmentWithTrap("computeTotal");
        AssessmentAssignment a = assignment(assessment, AssignmentStatus.PENDING, Instant.now().minus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findByToken(a.getToken())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.start(a.getToken())).isInstanceOf(GoneException.class);
        assertThat(a.getStatus()).isEqualTo(AssignmentStatus.EXPIRED);
    }

    @Test
    void gettingPublicAssignmentLazilyMarksExpired() {
        Assessment assessment = assessmentWithTrap(null);
        AssessmentAssignment a = assignment(assessment, AssignmentStatus.PENDING, Instant.now().minus(1, ChronoUnit.HOURS));
        when(assignmentRepository.findByToken(a.getToken())).thenReturn(Optional.of(a));

        PublicAssignmentResponse response = service.getPublicByToken(a.getToken());

        assertThat(response.status()).isEqualTo(AssignmentStatus.EXPIRED);
    }

    @Test
    void recruiterOnlySeesTheirOwnAssignments() {
        User recruiter = User.builder().id(1L).name("Recruiter").email("r@example.com").build();
        AssessmentAssignment a = assignment(assessmentWithTrap(null), AssignmentStatus.PENDING, Instant.now().plus(1, ChronoUnit.DAYS));
        when(userRepository.findById(1L)).thenReturn(Optional.of(recruiter));
        when(assignmentRepository.findByRecruiterOrderByCreatedAtDesc(recruiter)).thenReturn(List.of(a));

        List<AssignmentSummaryResponse> result = service.listAssignments(1L, Role.RECRUITER);

        assertThat(result).hasSize(1);
        verify(assignmentRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void adminSeesAllAssignmentsAcrossRecruiters() {
        AssessmentAssignment a1 = assignment(assessmentWithTrap(null), AssignmentStatus.PENDING, Instant.now().plus(1, ChronoUnit.DAYS));
        AssessmentAssignment a2 = assignment(assessmentWithTrap(null), AssignmentStatus.PENDING, Instant.now().plus(1, ChronoUnit.DAYS));
        when(assignmentRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(a1, a2));

        List<AssignmentSummaryResponse> result = service.listAssignments(99L, Role.ADMIN);

        assertThat(result).hasSize(2);
        verify(userRepository, never()).findById(any());
    }
}
