import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AssignmentService } from '../../../core/services/assignment.service';
import { PublicAssignment, ProctoringEvent } from '../../../core/models/assignment.model';
import { MonacoEditor } from '../../../shared/monaco-editor/monaco-editor';
import { toMonacoLanguage } from '../../../shared/monaco-editor/language-map';

@Component({
  selector: 'app-test-page',
  imports: [MonacoEditor],
  templateUrl: './test-page.html',
})
export class TestPage implements OnInit, OnDestroy {
  private token = '';
  private timerHandle?: ReturnType<typeof setInterval>;
  private deadline: number | null = null;
  private proctoringEvents: ProctoringEvent[] = [];

  assignment = signal<PublicAssignment | null>(null);
  code = signal('');
  loading = signal(true);
  submitting = signal(false);
  error = signal<string | null>(null);
  submitted = signal(false);
  remainingSeconds = signal<number | null>(null);

  monacoLanguage = computed(() => {
    const a = this.assignment();
    return a ? toMonacoLanguage(a.language) : 'plaintext';
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

  private load(): void {
    this.assignmentService.getPublic(this.token).subscribe({
      next: (assignment) => {
        this.assignment.set(assignment);
        this.code.set(assignment.starterCode);
        this.loading.set(false);
        if (assignment.status === 'IN_PROGRESS') {
          this.startTimer(assignment);
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
        this.startTimer(assignment);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Failed to start the test.');
      },
    });
  }

  private startTimer(assignment: PublicAssignment): void {
    const startedAt = assignment.startedAt ? new Date(assignment.startedAt).getTime() : Date.now();
    this.deadline = startedAt + assignment.durationMinutes * 60 * 1000;
    this.tick();
    this.timerHandle = setInterval(() => this.tick(), 1000);
  }

  private tick(): void {
    if (this.deadline === null) return;
    const secondsLeft = Math.max(0, Math.round((this.deadline - Date.now()) / 1000));
    this.remainingSeconds.set(secondsLeft);
    if (secondsLeft <= 0) {
      if (this.timerHandle) clearInterval(this.timerHandle);
      this.submit();
    }
  }

  submit(): void {
    if (this.submitting() || this.submitted()) return;
    this.submitting.set(true);
    const eventsJson = JSON.stringify(this.proctoringEvents);
    this.assignmentService.submit(this.token, this.code(), eventsJson).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitted.set(true);
        if (this.timerHandle) clearInterval(this.timerHandle);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err.error?.message ?? 'Failed to submit your answer.');
      },
    });
  }

  private recordEvent = (type: string) => {
    this.proctoringEvents.push({ type, timestamp: new Date().toISOString() });
  };

  private onVisibilityChange = () => {
    this.recordEvent(document.hidden ? 'tab_hidden' : 'tab_visible');
  };
  private onWindowBlur = () => this.recordEvent('window_blur');
  private onWindowFocus = () => this.recordEvent('window_focus');
  private onPaste = () => this.recordEvent('paste_attempt');

  private registerProctoringListeners(): void {
    document.addEventListener('visibilitychange', this.onVisibilityChange);
    window.addEventListener('blur', this.onWindowBlur);
    window.addEventListener('focus', this.onWindowFocus);
    // Capture phase so this fires regardless of what handles (or stops) the event afterward.
    document.addEventListener('paste', this.onPaste, true);
  }

  private unregisterProctoringListeners(): void {
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    window.removeEventListener('blur', this.onWindowBlur);
    window.removeEventListener('focus', this.onWindowFocus);
    document.removeEventListener('paste', this.onPaste, true);
  }
}
