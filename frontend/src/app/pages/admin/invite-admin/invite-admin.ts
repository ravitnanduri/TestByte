import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-invite-admin',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './invite-admin.html',
})
export class InviteAdmin {
  private fb = inject(FormBuilder);
  private adminService = inject(AdminService);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  sending = signal(false);
  sent = signal(false);
  error = signal<string | null>(null);

  submit(): void {
    if (this.form.invalid) return;
    this.sending.set(true);
    this.error.set(null);

    this.adminService.inviteAdmin(this.form.getRawValue().email).subscribe({
      next: () => {
        this.sending.set(false);
        this.sent.set(true);
        this.form.reset();
      },
      error: (err) => {
        this.sending.set(false);
        this.error.set(err.error?.message ?? 'Failed to send invite.');
      },
    });
  }
}
