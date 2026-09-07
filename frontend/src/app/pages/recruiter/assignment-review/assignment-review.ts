import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AssignmentService } from '../../../core/services/assignment.service';
import {
  AssignmentReview as AssignmentReviewModel,
  ProctoringEvent,
} from '../../../core/models/assignment.model';
import { MonacoEditor } from '../../../shared/monaco-editor/monaco-editor';
import { toMonacoLanguage } from '../../../shared/monaco-editor/language-map';

@Component({
  selector: 'app-assignment-review',
  imports: [RouterLink, DatePipe, MonacoEditor],
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

  monacoLanguage = computed(() => {
    const a = this.assignment();
    return a ? toMonacoLanguage(a.language) : 'plaintext';
  });

  proctoringEvents = computed<ProctoringEvent[]>(() => {
    const raw = this.assignment()?.proctoringEvents;
    if (!raw) return [];
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  });

  ngOnInit(): void {
    this.assignmentId = Number(this.route.snapshot.paramMap.get('id'));
    this.assignmentService.get(this.assignmentId).subscribe({
      next: (assignment) => {
        this.assignment.set(assignment);
        this.commentDraft.set(assignment.reviewComment ?? '');
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load this assignment.');
        this.loading.set(false);
      },
    });
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
        return 'Pasted into the editor';
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
