package com.testbyte.backend.assessment;

import com.testbyte.backend.assessment.dto.AssessmentResponse;
import com.testbyte.backend.assessment.dto.CreateAssessmentRequest;
import com.testbyte.backend.assessment.dto.QuestionOptionRequest;
import com.testbyte.backend.assessment.dto.QuestionRequest;
import com.testbyte.backend.assessment.dto.TestPageRequest;
import com.testbyte.backend.domain.Assessment;
import com.testbyte.backend.domain.QuestionType;
import com.testbyte.backend.domain.TestPage;
import com.testbyte.backend.domain.TestQuestion;
import com.testbyte.backend.domain.TestQuestionOption;
import com.testbyte.backend.domain.User;
import com.testbyte.backend.exception.BadRequestException;
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

    @Transactional
    public List<AssessmentResponse> listActive() {
        return assessmentRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(AssessmentResponse::from)
                .toList();
    }

    @Transactional
    public AssessmentResponse create(CreateAssessmentRequest request, Long createdByUserId) {
        validate(request);
        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Assessment assessment = Assessment.builder()
                .title(request.title())
                .createdBy(createdBy)
                .active(true)
                .build();
        applyPages(assessment, request.pages());

        assessmentRepository.save(assessment);
        return AssessmentResponse.from(assessment);
    }

    public Assessment getById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Test not found"));
    }

    @Transactional
    public AssessmentResponse getResponseById(Long id) {
        return AssessmentResponse.from(getById(id));
    }

    @Transactional
    public AssessmentResponse update(Long id, CreateAssessmentRequest request) {
        validate(request);
        Assessment assessment = getById(id);
        assessment.setTitle(request.title());
        assessment.getPages().clear();
        applyPages(assessment, request.pages());
        assessmentRepository.save(assessment);
        return AssessmentResponse.from(assessment);
    }

    private void applyPages(Assessment assessment, List<TestPageRequest> pageRequests) {
        int pageOrder = 1;
        for (TestPageRequest pageRequest : pageRequests) {
            TestPage page = TestPage.builder()
                    .test(assessment)
                    .pageOrder(pageOrder++)
                    .durationMinutes(pageRequest.durationMinutes())
                    .build();

            int questionOrder = 1;
            for (QuestionRequest questionRequest : pageRequest.questions()) {
                page.getQuestions().add(buildQuestion(page, questionOrder++, questionRequest));
            }
            assessment.getPages().add(page);
        }
    }

    private TestQuestion buildQuestion(TestPage page, int order, QuestionRequest request) {
        TestQuestion question = TestQuestion.builder()
                .page(page)
                .questionOrder(order)
                .questionType(request.type())
                .prompt(request.prompt())
                .language(request.type() == QuestionType.CODE ? request.language() : null)
                .starterCode(request.type() == QuestionType.CODE ? request.starterCode() : null)
                .editorFontSize(request.type() == QuestionType.CODE ? request.editorFontSize() : null)
                .editorFontColor(request.type() == QuestionType.CODE ? request.editorFontColor() : null)
                .build();

        if (request.type() == QuestionType.MULTIPLE_CHOICE) {
            int optionOrder = 1;
            for (QuestionOptionRequest optionRequest : request.options()) {
                question.getOptions().add(TestQuestionOption.builder()
                        .question(question)
                        .optionOrder(optionOrder++)
                        .optionText(optionRequest.text())
                        .correct(optionRequest.correct())
                        .build());
            }
        }
        return question;
    }

    private void validate(CreateAssessmentRequest request) {
        for (TestPageRequest page : request.pages()) {
            for (QuestionRequest question : page.questions()) {
                switch (question.type()) {
                    case CODE -> {
                        if (question.language() == null) {
                            throw new BadRequestException("Every code question needs a language selected.");
                        }
                    }
                    case MULTIPLE_CHOICE -> {
                        List<QuestionOptionRequest> options = question.options();
                        if (options == null || options.size() < 2) {
                            throw new BadRequestException("Multiple-choice questions need at least 2 options.");
                        }
                        long correctCount = options.stream().filter(QuestionOptionRequest::correct).count();
                        if (correctCount != 1) {
                            throw new BadRequestException("Multiple-choice questions need exactly one correct option.");
                        }
                    }
                    case TEXT -> {
                        // no extra requirements
                    }
                }
            }
        }
    }
}
