import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { AssignmentService } from '../../../core/services/assignment.service';
import { Assessment } from '../../../core/models/assessment.model';
import { AssignmentSummary } from '../../../core/models/assignment.model';

@Component({
  selector: 'app-schedule',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './schedule.html',
})
export class Schedule implements OnInit {
  private fb = inject(FormBuilder);
  private assessmentService = inject(AssessmentService);
  private assignmentService = inject(AssignmentService);

  tests = signal<Assessment[]>([]);
  loading = signal(true);
  saving = signal(false);
  loadError = signal<string | null>(null);
  error = signal<string | null>(null);
  created = signal<AssignmentSummary | null>(null);
  copied = signal(false);

  form = this.fb.nonNullable.group({
    testId: [null as number | null, Validators.required],
    candidateName: ['', Validators.required],
    roleAppliedFor: ['', Validators.required],
  });

  ngOnInit(): void {
    this.assessmentService.list().subscribe({
      next: (tests) => {
        this.tests.set(tests);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Failed to load available tests.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();
    this.assignmentService
      .create({
        testId: value.testId!,
        candidateName: value.candidateName,
        roleAppliedFor: value.roleAppliedFor,
      })
      .subscribe({
        next: (assignment) => {
          this.saving.set(false);
          this.created.set(assignment);
        },
        error: (err) => {
          this.saving.set(false);
          this.error.set(err.error?.message ?? 'Failed to schedule test.');
        },
      });
  }

  copyLink(): void {
    const link = this.created()?.link;
    if (!link) return;
    navigator.clipboard.writeText(link).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  scheduleAnother(): void {
    this.created.set(null);
    this.form.reset({ testId: null, candidateName: '', roleAppliedFor: '' });
  }
}
