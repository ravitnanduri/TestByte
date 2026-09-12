package com.testbyte.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * One timed page within a test (DB table "test_pages"). A test is taken page by page, each page
 * carrying its own duration; a page holds one or more questions.
 */
@Entity
@Table(name = "test_pages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Assessment test;

    @Column(name = "page_order", nullable = false)
    private Integer pageOrder;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    @Builder.Default
    private List<TestQuestion> questions = new ArrayList<>();
}
