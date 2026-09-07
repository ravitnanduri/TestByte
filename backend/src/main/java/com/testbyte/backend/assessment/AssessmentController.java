package com.testbyte.backend.assessment;

import com.testbyte.backend.assessment.dto.AssessmentResponse;
import com.testbyte.backend.assessment.dto.CreateAssessmentRequest;
import com.testbyte.backend.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public List<AssessmentResponse> list() {
        return assessmentService.listActive();
    }

    @GetMapping("/{id}")
    public AssessmentResponse get(@PathVariable Long id) {
        return assessmentService.getResponseById(id);
    }

    @PostMapping
    public AssessmentResponse create(@Valid @RequestBody CreateAssessmentRequest request,
                                      @AuthenticationPrincipal AuthPrincipal principal) {
        return assessmentService.create(request, principal.userId());
    }

    @PutMapping("/{id}")
    public AssessmentResponse update(@PathVariable Long id, @Valid @RequestBody CreateAssessmentRequest request) {
        return assessmentService.update(id, request);
    }
}
