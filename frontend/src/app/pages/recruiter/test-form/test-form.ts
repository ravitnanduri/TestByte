import { Component, EventEmitter, Input, OnInit, Output, computed, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  Assessment,
  AssessmentLanguage,
  CreateAssessmentRequest,
  Question,
  QuestionOption,
  QuestionType,
  TestPageContent,
} from '../../../core/models/assessment.model';
import { MonacoEditor } from '../../../shared/monaco-editor/monaco-editor';
import { PromptEditor } from '../../../shared/prompt-editor/prompt-editor';
import { toMonacoLanguage } from '../../../shared/monaco-editor/language-map';

type OptionGroup = FormGroup<{
  text: import('@angular/forms').FormControl<string>;
  correct: import('@angular/forms').FormControl<boolean>;
}>;

type QuestionGroup = FormGroup<{
  type: import('@angular/forms').FormControl<QuestionType>;
  prompt: import('@angular/forms').FormControl<string>;
  language: import('@angular/forms').FormControl<AssessmentLanguage>;
  starterCode: import('@angular/forms').FormControl<string>;
  editorFontSize: import('@angular/forms').FormControl<number>;
  editorFontColor: import('@angular/forms').FormControl<string>;
  options: FormArray<OptionGroup>;
}>;

type PageGroup = FormGroup<{
  durationMinutes: import('@angular/forms').FormControl<number>;
  questions: FormArray<QuestionGroup>;
}>;

/**
 * Shared page/question/option authoring form used by both test-new and test-edit -- they differ only in
 * whether they preload an existing test and whether they create or update on save.
 */
@Component({
  selector: 'app-test-form',
  imports: [ReactiveFormsModule, MonacoEditor, PromptEditor],
  templateUrl: './test-form.html',
})
export class TestForm implements OnInit {
  @Input() initialTest: Assessment | null = null;
  @Input() saving = false;
  @Input() submitLabel = 'Save';
  @Input() error: string | null = null;

  @Output() save = new EventEmitter<CreateAssessmentRequest>();

  private fb = new FormBuilder();

  languages: AssessmentLanguage[] = ['JAVA', 'PYTHON', 'SQL', 'JAVASCRIPT', 'TYPESCRIPT'];
  questionTypes: QuestionType[] = ['CODE', 'TEXT', 'MULTIPLE_CHOICE'];

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    pages: this.fb.array<PageGroup>([]),
  });

  validationError = signal<string | null>(null);

  ngOnInit(): void {
    if (this.initialTest) {
      this.form.controls.title.setValue(this.initialTest.title);
      for (const page of this.initialTest.pages) {
        this.form.controls.pages.push(this.buildPageGroup(page));
      }
    } else {
      this.addPage();
    }
  }

  get pages(): FormArray<PageGroup> {
    return this.form.controls.pages;
  }

  questionsOf(page: PageGroup): FormArray<QuestionGroup> {
    return page.controls.questions;
  }

  optionsOf(question: QuestionGroup): FormArray<OptionGroup> {
    return question.controls.options;
  }

  monacoLanguage(question: QuestionGroup): string {
    return toMonacoLanguage(question.controls.language.value);
  }

  addPage(): void {
    this.pages.push(this.buildPageGroup());
  }

  removePage(pageIndex: number): void {
    this.pages.removeAt(pageIndex);
  }

  addQuestion(page: PageGroup): void {
    this.questionsOf(page).push(this.buildQuestionGroup());
  }

  removeQuestion(page: PageGroup, questionIndex: number): void {
    this.questionsOf(page).removeAt(questionIndex);
  }

  onQuestionTypeChange(question: QuestionGroup): void {
    const type = question.controls.type.value;
    if (type === 'MULTIPLE_CHOICE' && this.optionsOf(question).length < 2) {
      this.optionsOf(question).push(this.buildOptionGroup({ text: '', correct: true }));
      this.optionsOf(question).push(this.buildOptionGroup({ text: '', correct: false }));
    }
  }

  addOption(question: QuestionGroup): void {
    this.optionsOf(question).push(this.buildOptionGroup({ text: '', correct: false }));
  }

  removeOption(question: QuestionGroup, optionIndex: number): void {
    const options = this.optionsOf(question);
    const wasCorrect = options.at(optionIndex).controls.correct.value;
    options.removeAt(optionIndex);
    if (wasCorrect && options.length > 0) {
      options.at(0).controls.correct.setValue(true);
    }
  }

  selectCorrectOption(question: QuestionGroup, optionIndex: number): void {
    this.optionsOf(question).controls.forEach((opt, i) => opt.controls.correct.setValue(i === optionIndex));
  }

  onStarterCodeChange(question: QuestionGroup, code: string): void {
    question.controls.starterCode.setValue(code);
  }

  submit(): void {
    this.validationError.set(null);
    if (this.form.invalid) {
      this.validationError.set('Please fill in all required fields.');
      return;
    }

    for (const pageGroup of this.pages.controls) {
      for (const questionGroup of this.questionsOf(pageGroup).controls) {
        if (questionGroup.controls.type.value === 'MULTIPLE_CHOICE') {
          const options = this.optionsOf(questionGroup).controls;
          if (options.length < 2) {
            this.validationError.set('Multiple-choice questions need at least 2 options.');
            return;
          }
          if (options.filter((o) => o.controls.correct.value).length !== 1) {
            this.validationError.set('Multiple-choice questions need exactly one correct option.');
            return;
          }
        }
      }
    }

    const value = this.form.getRawValue();
    const pages: TestPageContent[] = value.pages.map((page) => ({
      durationMinutes: page.durationMinutes,
      questions: page.questions.map((q) => this.toQuestionPayload(q)),
    }));

    this.save.emit({ title: value.title, pages });
  }

  private toQuestionPayload(q: {
    type: QuestionType;
    prompt: string;
    language: AssessmentLanguage;
    starterCode: string;
    editorFontSize: number;
    editorFontColor: string;
    options: { text: string; correct: boolean }[];
  }): Question {
    if (q.type === 'CODE') {
      return {
        type: q.type,
        prompt: q.prompt,
        language: q.language,
        starterCode: q.starterCode,
        editorFontSize: q.editorFontSize,
        editorFontColor: q.editorFontColor,
      };
    }
    if (q.type === 'MULTIPLE_CHOICE') {
      return { type: q.type, prompt: q.prompt, options: q.options.map((o) => ({ text: o.text, correct: o.correct })) };
    }
    return { type: q.type, prompt: q.prompt };
  }

  private buildPageGroup(initial?: TestPageContent): PageGroup {
    const group: PageGroup = this.fb.nonNullable.group({
      durationMinutes: this.fb.nonNullable.control<number>(initial?.durationMinutes ?? 10, [
        Validators.required,
        Validators.min(1),
      ]),
      questions: this.fb.array<QuestionGroup>([]),
    });
    if (initial) {
      for (const question of initial.questions) {
        group.controls.questions.push(this.buildQuestionGroup(question));
      }
    } else {
      group.controls.questions.push(this.buildQuestionGroup());
    }
    return group;
  }

  private buildQuestionGroup(initial?: Question): QuestionGroup {
    const group: QuestionGroup = this.fb.nonNullable.group({
      type: this.fb.nonNullable.control<QuestionType>(initial?.type ?? 'CODE', Validators.required),
      prompt: this.fb.nonNullable.control(initial?.prompt ?? '', Validators.required),
      language: this.fb.nonNullable.control<AssessmentLanguage>(initial?.language ?? 'JAVA'),
      starterCode: this.fb.nonNullable.control(initial?.starterCode ?? ''),
      editorFontSize: this.fb.nonNullable.control<number>(initial?.editorFontSize ?? 14),
      editorFontColor: this.fb.nonNullable.control(initial?.editorFontColor ?? '#000000'),
      options: this.fb.array<OptionGroup>([]),
    });
    for (const option of initial?.options ?? []) {
      group.controls.options.push(this.buildOptionGroup(option));
    }
    return group;
  }

  private buildOptionGroup(initial?: QuestionOption): OptionGroup {
    return this.fb.nonNullable.group({
      text: this.fb.nonNullable.control(initial?.text ?? '', Validators.required),
      correct: this.fb.nonNullable.control<boolean>(initial?.correct ?? false),
    });
  }
}
