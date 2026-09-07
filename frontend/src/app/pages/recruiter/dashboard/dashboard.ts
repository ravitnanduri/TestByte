import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AssignmentService } from '../../../core/services/assignment.service';
import { AssignmentSummary } from '../../../core/models/assignment.model';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  assignments = signal<AssignmentSummary[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  copiedId = signal<number | null>(null);

  constructor(private assignmentService: AssignmentService) {}

  ngOnInit(): void {
    this.assignmentService.list().subscribe({
      next: (assignments) => {
        this.assignments.set(assignments);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load your scheduled assessments.');
        this.loading.set(false);
      },
    });
  }

  copyLink(assignment: AssignmentSummary): void {
    navigator.clipboard.writeText(assignment.link).then(() => {
      this.copiedId.set(assignment.id);
      setTimeout(() => this.copiedId.set(null), 2000);
    });
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'badge badge-pending';
      case 'IN_PROGRESS':
        return 'badge badge-in-progress';
      case 'SUBMITTED':
        return 'badge badge-submitted';
      default:
        return 'badge badge-expired';
    }
  }
}
