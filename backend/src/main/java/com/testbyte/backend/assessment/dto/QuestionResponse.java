package com.testbyte.backend.assessment.dto;

import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.domain.QuestionType;
import com.testbyte.backend.domain.TestQuestion;

import java.util.List;

public record QuestionResponse(
        Long id,
        QuestionType type,
        String prompt,
        AssessmentLanguage language,
        String starterCode,
        Integer editorFontSize,
        String editorFontColor,
        List<QuestionOptionResponse> options
) {
    public static QuestionResponse from(TestQuestion question) {
        return new QuestionResponse(
                question.getId(),
                question.getQuestionType(),
                question.getPrompt(),
                question.getLanguage(),
                question.getStarterCode(),
                question.getEditorFontSize(),
                question.getEditorFontColor(),
                question.getOptions().stream().map(QuestionOptionResponse::from).toList()
        );
    }
}
