import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { Assessment } from '../../../core/models/assessment.model';

@Component({
  selector: 'app-test-list',
  imports: [RouterLink],
  templateUrl: './test-list.html',
})
export class TestList implements OnInit {
  private assessmentService = inject(AssessmentService);

  tests = signal<Assessment[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.assessmentService.list().subscribe({
      next: (tests) => {
        this.tests.set(tests);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load tests.');
        this.loading.set(false);
      },
    });
  }
}
