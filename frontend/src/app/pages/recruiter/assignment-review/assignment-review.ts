import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AssignmentService } from '../../../core/services/assignment.service';
import { AssessmentLanguage } from '../../../core/models/assessment.model';
import {
  AssignmentReview as AssignmentReviewModel,
  ProctoringEvent,
} from '../../../core/models/assignment.model';
import { MonacoEditor } from '../../../shared/monaco-editor/monaco-editor';
import { MarkdownPipe } from '../../../shared/markdown/markdown.pipe';
import { toMonacoLanguage } from '../../../shared/monaco-editor/language-map';

@Component({
  selector: 'app-assignment-review',
  imports: [RouterLink, DatePipe, MonacoEditor, MarkdownPipe],
  templateUrl: './assignment-review.html',
})
export class AssignmentReview implements OnInit {
  private route = inject(ActivatedRoute);
  private assignmentService = inject(AssignmentService);

  private assignmentId = 0;

  assignment = signal<AssignmentReviewModel | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  commentDraft = signal('');
  savingReview = signal(false);
  reviewError = signal<string | null>(null);

  proctoringEvents = signal<ProctoringEvent[]>([]);

  ngOnInit(): void {
    this.assignmentId = Number(this.route.snapshot.paramMap.get('id'));
    this.assignmentService.get(this.assignmentId).subscribe({
      next: (assignment) => {
        this.assignment.set(assignment);
        this.commentDraft.set(assignment.reviewComment ?? '');
        this.proctoringEvents.set(this.parseProctoringEvents(assignment.proctoringEvents));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load this assignment.');
        this.loading.set(false);
      },
    });
  }

  monacoLanguage(language: AssessmentLanguage | null): string {
    return language ? toMonacoLanguage(language) : 'plaintext';
  }

  private parseProctoringEvents(raw: string | null): ProctoringEvent[] {
    if (!raw) return [];
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  eventLabel(type: string): string {
    switch (type) {
      case 'tab_hidden':
        return 'Switched away from the tab';
      case 'tab_visible':
        return 'Returned to the tab';
      case 'window_blur':
        return 'Window lost focus';
      case 'window_focus':
        return 'Window regained focus';
      case 'paste_attempt':
        return 'Pasted content from outside the page';
      case 'paste_internal':
        return 'Pasted text they copied from the page';
      case 'copy_attempt':
        return 'Copied text on the page';
      case 'cut_attempt':
        return 'Cut text on the page';
      default:
        return type;
    }
  }

  saveReview(): void {
    if (!this.commentDraft().trim()) return;
    this.savingReview.set(true);
    this.reviewError.set(null);

    this.assignmentService.saveReview(this.assignmentId, this.commentDraft()).subscribe({
      next: (updated) => {
        this.assignment.set(updated);
        this.savingReview.set(false);
      },
      error: (err) => {
        this.reviewError.set(err.error?.message ?? 'Failed to save review.');
        this.savingReview.set(false);
      },
    });
  }
}
