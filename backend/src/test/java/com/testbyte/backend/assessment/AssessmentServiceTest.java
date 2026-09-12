package com.testbyte.backend.assessment;

import com.testbyte.backend.assessment.dto.AssessmentResponse;
import com.testbyte.backend.assessment.dto.CreateAssessmentRequest;
import com.testbyte.backend.assessment.dto.QuestionOptionRequest;
import com.testbyte.backend.assessment.dto.QuestionRequest;
import com.testbyte.backend.assessment.dto.TestPageRequest;
import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.domain.QuestionType;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.exception.BadRequestException;
import com.testbyte.backend.exception.NotFoundException;
import com.testbyte.backend.repository.AssessmentRepository;
import com.testbyte.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private UserRepository userRepository;

    private AssessmentService service;

    @BeforeEach
    void setUp() {
        service = new AssessmentService(assessmentRepository, userRepository);
        lenient().when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).name("Recruiter").email("r@example.com").build()));
    }

    private QuestionRequest codeQuestion(String prompt) {
        return new QuestionRequest(QuestionType.CODE, prompt, AssessmentLanguage.JAVA, "starter", 14, "#000000", null);
    }

    private QuestionRequest textQuestion(String prompt) {
        return new QuestionRequest(QuestionType.TEXT, prompt, null, null, null, null, null);
    }

    private QuestionRequest mcqQuestion(String prompt, List<QuestionOptionRequest> options) {
        return new QuestionRequest(QuestionType.MULTIPLE_CHOICE, prompt, null, null, null, null, options);
    }

    @Test
    void createBuildsPagesQuestionsAndOptions() {
        CreateAssessmentRequest request = new CreateAssessmentRequest("Multi-question test", List.of(
                new TestPageRequest(5, List.of(
                        mcqQuestion("Pick one", List.of(
                                new QuestionOptionRequest("A", true),
                                new QuestionOptionRequest("B", false)
                        )),
                        textQuestion("Explain yourself"))),
                new TestPageRequest(10, List.of(codeQuestion("Write some code")))
        ));

        AssessmentResponse response = service.create(request, 1L);

        assertThat(response.title()).isEqualTo("Multi-question test");
        assertThat(response.pages()).hasSize(2);
        assertThat(response.pages().get(0).questions()).hasSize(2);
        assertThat(response.pages().get(0).questions().get(0).options()).hasSize(2);
        assertThat(response.pages().get(1).questions().get(0).language()).isEqualTo(AssessmentLanguage.JAVA);
        assertThat(response.totalDurationMinutes()).isEqualTo(15);
        assertThat(response.questionCount()).isEqualTo(3);
    }

    @Test
    void codeQuestionWithoutLanguageIsRejected() {
        QuestionRequest badCodeQuestion = new QuestionRequest(QuestionType.CODE, "prompt", null, "starter", null, null, null);
        CreateAssessmentRequest request = new CreateAssessmentRequest("Bad test",
                List.of(new TestPageRequest(5, List.of(badCodeQuestion))));

        assertThatThrownBy(() -> service.create(request, 1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void multipleChoiceQuestionRequiresExactlyOneCorrectOption() {
        CreateAssessmentRequest zeroCorrect = new CreateAssessmentRequest("Bad MCQ", List.of(
                new TestPageRequest(5, List.of(mcqQuestion("Pick one", List.of(
                        new QuestionOptionRequest("A", false),
                        new QuestionOptionRequest("B", false)))))));
        assertThatThrownBy(() -> service.create(zeroCorrect, 1L)).isInstanceOf(BadRequestException.class);

        CreateAssessmentRequest twoCorrect = new CreateAssessmentRequest("Bad MCQ", List.of(
                new TestPageRequest(5, List.of(mcqQuestion("Pick one", List.of(
                        new QuestionOptionRequest("A", true),
                        new QuestionOptionRequest("B", true)))))));
        assertThatThrownBy(() -> service.create(twoCorrect, 1L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateReplacesAllPagesAndQuestions() {
        Assessment existing = Assessment.builder().id(1L).title("Old title").active(true).build();
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        CreateAssessmentRequest request = new CreateAssessmentRequest("New title",
                List.of(new TestPageRequest(20, List.of(codeQuestion("New prompt")))));

        AssessmentResponse response = service.update(1L, request);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.pages()).hasSize(1);
        assertThat(response.pages().get(0).durationMinutes()).isEqualTo(20);
        assertThat(response.pages().get(0).questions().get(0).prompt()).isEqualTo("New prompt");
    }

    @Test
    void updateThrowsWhenTestNotFound() {
        when(assessmentRepository.findById(99L)).thenReturn(Optional.empty());

        CreateAssessmentRequest request = new CreateAssessmentRequest("T",
                List.of(new TestPageRequest(10, List.of(codeQuestion("i")))));

        assertThatThrownBy(() -> service.update(99L, request)).isInstanceOf(NotFoundException.class);
    }
}
