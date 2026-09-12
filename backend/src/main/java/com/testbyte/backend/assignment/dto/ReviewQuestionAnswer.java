package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.domain.QuestionType;

import java.util.List;

/**
 * One question + the candidate's answer to it, for the recruiter review page. prompt/type/language here
 * reflect the LIVE question when it still exists (id != null), or the snapshot captured at submit time
 * when the recruiter has since edited/removed that question (id == null).
 */
public record ReviewQuestionAnswer(
        Long questionId,
        QuestionType type,
        String prompt,
        AssessmentLanguage language,
        String starterCode,
        Integer editorFontSize,
        String editorFontColor,
        List<ReviewQuestionOption> options,
        String answerText,
        Long selectedOptionId,
        String selectedOptionText,
        Boolean selectedOptionCorrect
) {
}
