package com.testbyte.backend.assignment;

import com.testbyte.backend.assignment.dto.AssignmentReviewResponse;
import com.testbyte.backend.assignment.dto.AssignmentSummaryResponse;
import com.testbyte.backend.assignment.dto.CreateAssignmentRequest;
import com.testbyte.backend.assignment.dto.ReviewAssignmentRequest;
import com.testbyte.backend.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public AssignmentSummaryResponse create(@Valid @RequestBody CreateAssignmentRequest request,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.create(request, principal.userId());
    }

    @GetMapping
    public List<AssignmentSummaryResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.listAssignments(principal.userId(), principal.role());
    }

    @GetMapping("/{id}")
    public AssignmentReviewResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.getForReview(id, principal.userId(), principal.role());
    }

    @PutMapping("/{id}/review")
    public AssignmentReviewResponse review(@PathVariable Long id, @Valid @RequestBody ReviewAssignmentRequest request,
                                            @AuthenticationPrincipal AuthPrincipal principal) {
        return assignmentService.saveReview(id, principal.userId(), principal.role(), request.comment());
    }
}
