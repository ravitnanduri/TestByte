import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-accept-invite',
  imports: [ReactiveFormsModule],
  templateUrl: './accept-invite.html',
})
export class AcceptInvite implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  token = signal<string | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.token.set(this.route.snapshot.queryParamMap.get('token'));
    if (!this.token()) {
      this.error.set('This invite link is missing a token.');
    }
  }

  submit(): void {
    if (this.form.invalid || !this.token()) return;
    this.loading.set(true);
    this.error.set(null);

    const { name, password } = this.form.getRawValue();
    this.auth.acceptAdminInvite(this.token()!, name, password).subscribe({
      next: () => this.router.navigate(['/admin/pending-approvals']),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Failed to accept invite.');
      },
    });
  }
}
