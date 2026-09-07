import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { AssessmentLanguage } from '../../../core/models/assessment.model';
import { MonacoEditor } from '../../../shared/monaco-editor/monaco-editor';
import { toMonacoLanguage } from '../../../shared/monaco-editor/language-map';

@Component({
  selector: 'app-test-new',
  imports: [ReactiveFormsModule, RouterLink, MonacoEditor],
  templateUrl: './test-new.html',
})
export class TestNew {
  private fb = inject(FormBuilder);
  private assessmentService = inject(AssessmentService);
  private router = inject(Router);

  languages: AssessmentLanguage[] = ['JAVA', 'PYTHON', 'SQL', 'JAVASCRIPT', 'TYPESCRIPT'];

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    language: this.fb.nonNullable.control<AssessmentLanguage>('JAVA', Validators.required),
    instructionsHtml: ['', Validators.required],
    starterCode: ['', Validators.required],
    aiTrapPhrase: [''],
    durationMinutes: [10, [Validators.required, Validators.min(1)]],
  });

  private selectedLanguage = toSignal(this.form.controls.language.valueChanges, {
    initialValue: this.form.controls.language.value,
  });
  monacoLanguage = computed(() => toMonacoLanguage(this.selectedLanguage()));

  saving = signal(false);
  error = signal<string | null>(null);

  onStarterCodeChange(code: string): void {
    this.form.controls.starterCode.setValue(code);
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set(null);

    const value = this.form.getRawValue();
    this.assessmentService
      .create({
        title: value.title,
        language: value.language,
        instructionsHtml: value.instructionsHtml,
        starterCode: value.starterCode,
        aiTrapPhrase: value.aiTrapPhrase || undefined,
        durationMinutes: value.durationMinutes,
      })
      .subscribe({
        next: () => this.router.navigate(['/recruiter/schedule']),
        error: (err) => {
          this.saving.set(false);
          this.error.set(err.error?.message ?? 'Failed to create test.');
        },
      });
  }
}
