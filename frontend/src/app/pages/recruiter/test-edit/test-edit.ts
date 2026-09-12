import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { Assessment, CreateAssessmentRequest } from '../../../core/models/assessment.model';
import { TestForm } from '../test-form/test-form';

@Component({
  selector: 'app-test-edit',
  imports: [RouterLink, TestForm],
  templateUrl: './test-edit.html',
})
export class TestEdit implements OnInit {
  private assessmentService = inject(AssessmentService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private testId = 0;

  test = signal<Assessment | null>(null);
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.testId = Number(this.route.snapshot.paramMap.get('id'));
    this.assessmentService.get(this.testId).subscribe({
      next: (test) => {
        this.test.set(test);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Failed to load this test.');
        this.loading.set(false);
      },
    });
  }

  onSave(request: CreateAssessmentRequest): void {
    this.saving.set(true);
    this.error.set(null);
    this.assessmentService.update(this.testId, request).subscribe({
      next: () => this.router.navigate(['/recruiter/tests']),
      error: (err) => {
        this.saving.set(false);
        this.error.set(err.error?.message ?? 'Failed to save changes.');
      },
    });
  }
}
