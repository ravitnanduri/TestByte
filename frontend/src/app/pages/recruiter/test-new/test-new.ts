import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { CreateAssessmentRequest } from '../../../core/models/assessment.model';
import { TestForm } from '../test-form/test-form';

@Component({
  selector: 'app-test-new',
  imports: [RouterLink, TestForm],
  templateUrl: './test-new.html',
})
export class TestNew {
  private assessmentService = inject(AssessmentService);
  private router = inject(Router);

  saving = signal(false);
  error = signal<string | null>(null);

  onSave(request: CreateAssessmentRequest): void {
    this.saving.set(true);
    this.error.set(null);
    this.assessmentService.create(request).subscribe({
      next: () => this.router.navigate(['/recruiter/schedule']),
      error: (err) => {
        this.saving.set(false);
        this.error.set(err.error?.message ?? 'Failed to create test.');
      },
    });
  }
}
