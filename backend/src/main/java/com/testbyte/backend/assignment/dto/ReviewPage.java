package com.testbyte.backend.assignment.dto;

import java.util.List;

public record ReviewPage(
        Integer durationMinutes,
        List<ReviewQuestionAnswer> questions
) {
}
