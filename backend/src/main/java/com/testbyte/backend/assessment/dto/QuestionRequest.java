package com.testbyte.backend.assessment.dto;

import com.testbyte.backend.domain.AssessmentLanguage;
import com.testbyte.backend.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * language/starterCode/editorFontSize/editorFontColor only apply to CODE questions; options only apply
 * to MULTIPLE_CHOICE questions. Type-conditional requirements (language required for CODE, >= 2 options
 * with exactly 1 correct for MULTIPLE_CHOICE) are enforced in AssessmentService, not here.
 */
public record QuestionRequest(
        @NotNull QuestionType type,
        @NotBlank String prompt,
        AssessmentLanguage language,
        String starterCode,
        Integer editorFontSize,
        String editorFontColor,
        @Valid List<QuestionOptionRequest> options
) {
}
