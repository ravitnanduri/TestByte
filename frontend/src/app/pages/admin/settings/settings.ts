import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './settings.html',
})
export class Settings implements OnInit {
  private fb = inject(FormBuilder);
  private adminService = inject(AdminService);

  form = this.fb.nonNullable.group({
    approverNotificationEmail: ['', [Validators.required, Validators.email]],
  });

  loading = signal(true);
  saving = signal(false);
  saved = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.adminService.getSettings().subscribe({
      next: (settings) => {
        this.form.patchValue(settings);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load settings.');
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.saved.set(false);
    this.error.set(null);

    this.adminService.updateSettings(this.form.getRawValue().approverNotificationEmail).subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.set(true);
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err.error?.message ?? 'Failed to save settings.');
      },
    });
  }
}
