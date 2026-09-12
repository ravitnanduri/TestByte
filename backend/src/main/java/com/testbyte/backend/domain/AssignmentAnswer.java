package com.testbyte.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A candidate's answer to one question of an assignment (DB table "assignment_answers").
 * question/selectedOption are ON DELETE SET NULL and may become null if a recruiter later edits the
 * test out from under a submitted assignment -- the *Snapshot fields (filled in at submit time) are
 * what the review page actually displays, so historical answers stay readable either way.
 */
@Entity
@Table(name = "assignment_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AssessmentAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private TestQuestion question;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private TestQuestionOption selectedOption;

    @Column(name = "prompt_snapshot", columnDefinition = "TEXT")
    private String promptSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type_snapshot", length = 20)
    private QuestionType questionTypeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_snapshot", length = 20)
    private AssessmentLanguage languageSnapshot;

    @Column(name = "selected_option_text_snapshot", columnDefinition = "TEXT")
    private String selectedOptionTextSnapshot;

    @Column(name = "selected_option_was_correct_snapshot")
    private Boolean selectedOptionWasCorrectSnapshot;
}
