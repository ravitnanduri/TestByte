import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { AssessmentLanguage } from '../../../core/models/assessment.model';
import { MonacoEditor } from '../../../shared/monaco-editor/monaco-editor';
import { toMonacoLanguage } from '../../../shared/monaco-editor/language-map';

@Component({
  selector: 'app-test-edit',
  imports: [ReactiveFormsModule, RouterLink, MonacoEditor],
  templateUrl: './test-edit.html',
})
export class TestEdit implements OnInit {
  private fb = inject(FormBuilder);
  private assessmentService = inject(AssessmentService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private testId = 0;

  languages: AssessmentLanguage[] = ['JAVA', 'PYTHON', 'SQL', 'JAVASCRIPT', 'TYPESCRIPT'];

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    language: this.fb.nonNullable.control<AssessmentLanguage>('JAVA', Validators.required),
    instructionsHtml: ['', Validators.required],
    starterCode: ['', Validators.required],
    durationMinutes: [10, [Validators.required, Validators.min(1)]],
  });

  private selectedLanguage = toSignal(this.form.controls.language.valueChanges, {
    initialValue: this.form.controls.language.value,
  });
  monacoLanguage = computed(() => toMonacoLanguage(this.selectedLanguage()));

  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.testId = Number(this.route.snapshot.paramMap.get('id'));
    this.assessmentService.get(this.testId).subscribe({
      next: (test) => {
        this.form.patchValue({
          title: test.title,
          language: test.language,
          instructionsHtml: test.instructionsHtml,
          starterCode: test.starterCode,
          durationMinutes: test.durationMinutes,
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Failed to load this test.');
        this.loading.set(false);
      },
    });
  }

  onStarterCodeChange(code: string): void {
    this.form.controls.starterCode.setValue(code);
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();
    this.assessmentService
      .update(this.testId, {
        title: value.title,
        language: value.language,
        instructionsHtml: value.instructionsHtml,
        starterCode: value.starterCode,
        durationMinutes: value.durationMinutes,
      })
      .subscribe({
        next: () => this.router.navigate(['/recruiter/tests']),
        error: (err) => {
          this.saving.set(false);
          this.error.set(err.error?.message ?? 'Failed to save changes.');
        },
      });
  }
}
