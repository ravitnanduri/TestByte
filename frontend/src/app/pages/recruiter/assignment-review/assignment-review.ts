import { Component, OnInit, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AssignmentService } from '../../../core/services/assignment.service';
import { AssignmentReview as AssignmentReviewModel } from '../../../core/models/assignment.model';
import { MonacoEditor } from '../../../shared/monaco-editor/monaco-editor';
import { toMonacoLanguage } from '../../../shared/monaco-editor/language-map';

@Component({
  selector: 'app-assignment-review',
  imports: [RouterLink, DatePipe, MonacoEditor],
  templateUrl: './assignment-review.html',
})
export class AssignmentReview implements OnInit {
  assignment = signal<AssignmentReviewModel | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  monacoLanguage = computed(() => {
    const a = this.assignment();
    return a ? toMonacoLanguage(a.language) : 'plaintext';
  });

  constructor(
    private route: ActivatedRoute,
    private assignmentService: AssignmentService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.assignmentService.get(id).subscribe({
      next: (assignment) => {
        this.assignment.set(assignment);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load this assignment.');
        this.loading.set(false);
      },
    });
  }
}
