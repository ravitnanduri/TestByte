package com.testbyte.backend.assessment.dto;

import com.testbyte.backend.domain.AssessmentLanguage;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAssessmentRequest(
        @NotBlank String title,
        @NotNull AssessmentLanguage language,
        @NotBlank String instructionsHtml,
        @NotBlank String starterCode,
        String aiTrapPhrase,
        @NotNull @Min(1) Integer durationMinutes
) {
}
