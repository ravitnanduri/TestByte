package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.domain.QuestionType;
import com.testbyte.backend.domain.TestQuestion;

import java.util.List;

public record PublicQuestion(
        Long id,
        QuestionType type,
        String prompt,
        AssessmentLanguage language,
        String starterCode,
        Integer editorFontSize,
        String editorFontColor,
        List<PublicQuestionOption> options
) {
    public static PublicQuestion from(TestQuestion question) {
        return new PublicQuestion(
                question.getId(),
                question.getQuestionType(),
                question.getPrompt(),
                question.getLanguage(),
                question.getStarterCode(),
                question.getEditorFontSize(),
                question.getEditorFontColor(),
                question.getOptions().stream().map(PublicQuestionOption::from).toList()
        );
    }
}
