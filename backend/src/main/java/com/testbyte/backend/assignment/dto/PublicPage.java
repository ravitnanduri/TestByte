package com.testbyte.backend.assignment.dto;

import com.testbyte.backend.domain.TestPage;

import java.util.List;

public record PublicPage(
        Long id,
        Integer durationMinutes,
        List<PublicQuestion> questions
) {
    public static PublicPage from(TestPage page) {
        return new PublicPage(
                page.getId(),
                page.getDurationMinutes(),
                page.getQuestions().stream().map(PublicQuestion::from).toList()
        );
    }
}
