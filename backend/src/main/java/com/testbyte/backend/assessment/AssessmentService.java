package com.testbyte.backend.assessment;

import com.testbyte.backend.assessment.dto.AssessmentResponse;
import com.testbyte.backend.assessment.dto.CreateAssessmentRequest;
import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.exception.NotFoundException;
import com.testbyte.backend.repository.AssessmentRepository;
import com.testbyte.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final UserRepository userRepository;

    public AssessmentService(AssessmentRepository assessmentRepository, UserRepository userRepository) {
        this.assessmentRepository = assessmentRepository;
        this.userRepository = userRepository;
    }

    public List<AssessmentResponse> listActive() {
        return assessmentRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(AssessmentResponse::from)
                .toList();
    }

    @Transactional
    public AssessmentResponse create(CreateAssessmentRequest request, Long createdByUserId) {
        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Assessment assessment = Assessment.builder()
                .title(request.title())
                .language(request.language())
                .instructionsHtml(request.instructionsHtml())
                .starterCode(request.starterCode())
                .aiTrapPhrase(request.aiTrapPhrase())
                .durationMinutes(request.durationMinutes())
                .createdBy(createdBy)
                .active(true)
                .build();

        assessmentRepository.save(assessment);
        return AssessmentResponse.from(assessment);
    }

    public Assessment getById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Test not found"));
    }
}
