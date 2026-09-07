package com.testbyte.backend.assessment;

import com.testbyte.backend.assessment.dto.AssessmentResponse;
import com.testbyte.backend.assessment.dto.CreateAssessmentRequest;
import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.exception.NotFoundException;
import com.testbyte.backend.repository.AssessmentRepository;
import com.testbyte.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    }

    @Test
    void updateOverwritesAllEditableFields() {
        Assessment existing = Assessment.builder()
                .id(1L)
                .title("Old title")
                .language(AssessmentLanguage.JAVA)
                .instructionsHtml("<p>old</p>")
                .starterCode("old code")
                .aiTrapPhrase("oldTrap")
                .durationMinutes(10)
                .active(true)
                .build();
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(existing));

        CreateAssessmentRequest request = new CreateAssessmentRequest(
                "New title", AssessmentLanguage.PYTHON, "<p>new</p>", "new code", "newTrap", 20);

        AssessmentResponse response = service.update(1L, request);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.language()).isEqualTo(AssessmentLanguage.PYTHON);
        assertThat(response.instructionsHtml()).isEqualTo("<p>new</p>");
        assertThat(response.starterCode()).isEqualTo("new code");
        assertThat(response.durationMinutes()).isEqualTo(20);
        assertThat(existing.getAiTrapPhrase()).isEqualTo("newTrap");
    }

    @Test
    void updateThrowsWhenTestNotFound() {
        when(assessmentRepository.findById(99L)).thenReturn(Optional.empty());

        CreateAssessmentRequest request = new CreateAssessmentRequest(
                "T", AssessmentLanguage.JAVA, "<p>i</p>", "code", null, 10);

        assertThatThrownBy(() -> service.update(99L, request)).isInstanceOf(NotFoundException.class);
    }
}
