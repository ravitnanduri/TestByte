import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AssignmentService } from '../../../core/services/assignment.service';
import { AnswerRequest, ProctoringEvent, PublicAssignment, PublicPage } from '../../../core/models/assignment.model';
import { AssessmentLanguage } from '../../../core/models/assessment.model';
import { MonacoEditor } from '../../../shared/monaco-editor/monaco-editor';
import { MarkdownPipe } from '../../../shared/markdown/markdown.pipe';
import { toMonacoLanguage } from '../../../shared/monaco-editor/language-map';

@Component({
  selector: 'app-test-page',
  imports: [MonacoEditor, MarkdownPipe],
  templateUrl: './test-page.html',
})
export class TestPage implements OnInit, OnDestroy {
  private token = '';
  private timerHandle?: ReturnType<typeof setInterval>;
  private deadline: number | null = null;
  private proctoringEvents: ProctoringEvent[] = [];
  private answers = new Map<number, AnswerRequest>();

  assignment = signal<PublicAssignment | null>(null);
  currentPageIndex = signal(0);
  loading = signal(true);
  submitting = signal(false);
  error = signal<string | null>(null);
  submitted = signal(false);
  remainingSeconds = signal<number | null>(null);

  currentPage = computed<PublicPage | null>(() => {
    const a = this.assignment();
    return a ? (a.pages[this.currentPageIndex()] ?? null) : null;
  });

  isLastPage = computed(() => {
    const a = this.assignment();
    return !a || this.currentPageIndex() >= a.pages.length - 1;
  });

  remainingLabel = computed(() => {
    const seconds = this.remainingSeconds();
    if (seconds === null) return '';
    const m = Math.floor(seconds / 60)
      .toString()
      .padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  });

  constructor(
    private route: ActivatedRoute,
    private assignmentService: AssignmentService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    this.load();
    this.registerProctoringListeners();
  }

  ngOnDestroy(): void {
    if (this.timerHandle) clearInterval(this.timerHandle);
    this.unregisterProctoringListeners();
  }

  monacoLanguage(language: AssessmentLanguage | null): string {
    return language ? toMonacoLanguage(language) : 'plaintext';
  }

  private load(): void {
    this.assignmentService.getPublic(this.token).subscribe({
      next: (assignment) => {
        this.assignment.set(assignment);
        this.loading.set(false);
        if (assignment.status === 'IN_PROGRESS' && assignment.startedAt) {
          this.resumeAtCurrentPage(assignment);
        }
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'This test link could not be found.');
        this.loading.set(false);
      },
    });
  }

  begin(): void {
    this.assignmentService.start(this.token).subscribe({
      next: (assignment) => {
        this.assignment.set(assignment);
        this.currentPageIndex.set(0);
        this.seedDefaultsForCurrentPage();
        this.startTimerForCurrentPage(assignment);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Failed to start the test.');
      },
    });
  }

  /** On reload mid-test, figure out which page the elapsed time falls into and jump straight there. */
  private resumeAtCurrentPage(assignment: PublicAssignment): void {
    const startedAt = new Date(assignment.startedAt!).getTime();
    const elapsedSeconds = (Date.now() - startedAt) / 1000;

    let cumulativeSeconds = 0;
    let pageIndex = assignment.pages.length - 1;
    for (let i = 0; i < assignment.pages.length; i++) {
      cumulativeSeconds += assignment.pages[i].durationMinutes * 60;
      if (elapsedSeconds < cumulativeSeconds) {
        pageIndex = i;
        break;
      }
    }
    this.currentPageIndex.set(pageIndex);
    this.seedDefaultsForCurrentPage();
    this.startTimerForCurrentPage(assignment);
  }

  private cumulativeSecondsThroughCurrentPage(assignment: PublicAssignment): number {
    let total = 0;
    for (let i = 0; i <= this.currentPageIndex(); i++) {
      total += assignment.pages[i].durationMinutes * 60;
    }
    return total;
  }

  private startTimerForCurrentPage(assignment: PublicAssignment): void {
    if (this.timerHandle) clearInterval(this.timerHandle);
    const startedAt = assignment.startedAt ? new Date(assignment.startedAt).getTime() : Date.now();
    this.deadline = startedAt + this.cumulativeSecondsThroughCurrentPage(assignment) * 1000;
    this.tick();
    this.timerHandle = setInterval(() => this.tick(), 1000);
  }

  private tick(): void {
    if (this.deadline === null) return;
    const secondsLeft = Math.max(0, Math.round((this.deadline - Date.now()) / 1000));
    this.remainingSeconds.set(secondsLeft);
    if (secondsLeft <= 0) {
      this.advance();
    }
  }

  private seedDefaultsForCurrentPage(): void {
    const page = this.currentPage();
    if (!page) return;
    for (const q of page.questions) {
      if (q.type === 'CODE' && !this.answers.has(q.id)) {
        this.answers.set(q.id, { questionId: q.id, answerText: q.starterCode ?? '' });
      }
    }
  }

  answerText(questionId: number): string {
    return this.answers.get(questionId)?.answerText ?? '';
  }

  selectedOptionId(questionId: number): number | null {
    return this.answers.get(questionId)?.selectedOptionId ?? null;
  }

  setAnswerText(questionId: number, text: string): void {
    const existing = this.answers.get(questionId) ?? { questionId };
    this.answers.set(questionId, { ...existing, answerText: text });
  }

  setSelectedOption(questionId: number, optionId: number): void {
    const existing = this.answers.get(questionId) ?? { questionId };
    this.answers.set(questionId, { ...existing, selectedOptionId: optionId });
  }

  /** Moving on early (before the page timer expires) or being forced on by the timer -- same path. */
  goToNextPageOrSubmit(): void {
    if (this.isLastPage()) {
      this.submit();
    } else {
      this.advance();
    }
  }

  private advance(): void {
    const a = this.assignment();
    if (!a) return;
    if (this.currentPageIndex() >= a.pages.length - 1) {
      this.submit();
      return;
    }
    this.currentPageIndex.set(this.currentPageIndex() + 1);
    this.seedDefaultsForCurrentPage();
    this.startTimerForCurrentPage(a);
  }

  submit(): void {
    if (this.submitting() || this.submitted()) return;
    this.submitting.set(true);
    if (this.timerHandle) clearInterval(this.timerHandle);

    const answers = Array.from(this.answers.values());
    const eventsJson = JSON.stringify(this.proctoringEvents);
    this.assignmentService.submit(this.token, answers, eventsJson).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitted.set(true);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err.error?.message ?? 'Failed to submit your answers.');
      },
    });
  }

  /**
   * Text copied or cut from anywhere on this page during the test, so a later paste of exactly that text
   * can be recognized as "pasted their own copy" rather than flagged as pasted-in-from-outside. Compared
   * with normalized whitespace/line-endings since a round trip through the OS clipboard can alter those.
   */
  private copiedSnippets = new Set<string>();

  private recordEvent = (type: string) => {
    this.proctoringEvents.push({ type, timestamp: new Date().toISOString() });
  };

  private normalize(text: string): string {
    return text.replace(/\r\n/g, '\n').trim();
  }

  private rememberCopied(text: string): void {
    const normalized = this.normalize(text);
    if (normalized) this.copiedSnippets.add(normalized);
  }

  private isInternalPaste(text: string): boolean {
    return this.copiedSnippets.has(this.normalize(text));
  }

  private onVisibilityChange = () => {
    this.recordEvent(document.hidden ? 'tab_hidden' : 'tab_visible');
  };
  private onWindowBlur = () => this.recordEvent('window_blur');
  private onWindowFocus = () => this.recordEvent('window_focus');

  /**
   * Monaco's own onDidPaste/copy/cut hooks (wired via the (pasteDetected)/(copyDetected)/(cutDetected)
   * outputs on each <app-monaco-editor> below) are the reliable signal for clipboard actions inside the
   * code editor -- see the comment on MonacoEditor.pasteDetected. These document-level listeners are only
   * a fallback for targets outside Monaco (the plain TEXT question textarea, the instructions panel), so
   * they skip anything that landed inside a Monaco host to avoid double-logging the same action twice.
   */
  private onPaste = (event: ClipboardEvent) => {
    const target = event.target as HTMLElement | null;
    if (target?.closest('.monaco-host')) return;
    const pasted = event.clipboardData?.getData('text/plain') ?? '';
    if (this.isInternalPaste(pasted)) return;
    this.recordEvent('paste_attempt');
  };

  private onCopy = (event: Event) => {
    const target = event.target as HTMLElement | null;
    if (target?.closest('.monaco-host')) return;
    this.rememberCopied(this.extractSelectedText(target));
    this.recordEvent('copy_attempt');
  };

  private onCut = (event: Event) => {
    const target = event.target as HTMLElement | null;
    if (target?.closest('.monaco-host')) return;
    this.rememberCopied(this.extractSelectedText(target));
    this.recordEvent('cut_attempt');
  };

  /** window.getSelection() doesn't see text selected inside an <input>/<textarea>'s own selection model. */
  private extractSelectedText(target: HTMLElement | null): string {
    if (target instanceof HTMLTextAreaElement || target instanceof HTMLInputElement) {
      return target.value.substring(target.selectionStart ?? 0, target.selectionEnd ?? 0);
    }
    return window.getSelection()?.toString() ?? '';
  }

  onEditorPaste(pastedText: string): void {
    if (this.isInternalPaste(pastedText)) return;
    this.recordEvent('paste_attempt');
  }

  onEditorCopy(copiedText: string): void {
    this.rememberCopied(copiedText);
    this.recordEvent('copy_attempt');
  }

  onEditorCut(cutText: string): void {
    this.rememberCopied(cutText);
    this.recordEvent('cut_attempt');
  }

  private registerProctoringListeners(): void {
    document.addEventListener('visibilitychange', this.onVisibilityChange);
    window.addEventListener('blur', this.onWindowBlur);
    window.addEventListener('focus', this.onWindowFocus);
    // Capture phase so these fire regardless of what handles (or stops) the event afterward.
    document.addEventListener('paste', this.onPaste, true);
    document.addEventListener('copy', this.onCopy, true);
    document.addEventListener('cut', this.onCut, true);
  }

  private unregisterProctoringListeners(): void {
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    window.removeEventListener('blur', this.onWindowBlur);
    window.removeEventListener('focus', this.onWindowFocus);
    document.removeEventListener('paste', this.onPaste, true);
    document.removeEventListener('copy', this.onCopy, true);
    document.removeEventListener('cut', this.onCut, true);
  }
}
