import { Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { PendingRecruiter } from '../../../core/models/admin.model';

@Component({
  selector: 'app-pending-approvals',
  imports: [RouterLink, DatePipe],
  templateUrl: './pending-approvals.html',
})
export class PendingApprovals implements OnInit {
  recruiters = signal<PendingRecruiter[]>([]);
  loading = signal(true);
  actioningId = signal<number | null>(null);
  loadError = signal<string | null>(null);
  actionError = signal<string | null>(null);

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.adminService.pendingRecruiters().subscribe({
      next: (recruiters) => {
        this.recruiters.set(recruiters);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Failed to load pending recruiters.');
        this.loading.set(false);
      },
    });
  }

  approve(id: number): void {
    this.actioningId.set(id);
    this.actionError.set(null);
    this.adminService.approve(id).subscribe({
      next: () => {
        this.recruiters.update((list) => list.filter((r) => r.id !== id));
        this.actioningId.set(null);
      },
      error: () => {
        this.actionError.set('Failed to approve this recruiter. Please try again.');
        this.actioningId.set(null);
      },
    });
  }

  reject(id: number): void {
    this.actioningId.set(id);
    this.actionError.set(null);
    this.adminService.reject(id).subscribe({
      next: () => {
        this.recruiters.update((list) => list.filter((r) => r.id !== id));
        this.actioningId.set(null);
      },
      error: () => {
        this.actionError.set('Failed to reject this recruiter. Please try again.');
        this.actioningId.set(null);
      },
    });
  }
}
