package com.testbyte.backend.assignment;

import com.testbyte.backend.assignment.dto.PublicAssignmentResponse;
import com.testbyte.backend.assignment.dto.SubmitAssignmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/assignments")
public class PublicAssignmentController {

    private final AssignmentService assignmentService;

    public PublicAssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/{token}")
    public PublicAssignmentResponse get(@PathVariable UUID token) {
        return assignmentService.getPublicByToken(token);
    }

    @PostMapping("/{token}/start")
    public PublicAssignmentResponse start(@PathVariable UUID token) {
        return assignmentService.start(token);
    }

    @PostMapping("/{token}/submit")
    public ResponseEntity<Void> submit(@PathVariable UUID token, @Valid @RequestBody SubmitAssignmentRequest request) {
        assignmentService.submit(token, request.code(), request.proctoringEvents());
        return ResponseEntity.noContent().build();
    }
}
