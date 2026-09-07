import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  @ViewChild('emailInput') emailInput!: ElementRef<HTMLInputElement>;
  @ViewChild('passwordInput') passwordInput!: ElementRef<HTMLInputElement>;

  loading = signal(false);
  error = signal<string | null>(null);

  submit(): void {
    // Read straight from the DOM rather than a reactive-forms value: some
    // browsers/password managers fill inputs without firing the events
    // reactive forms rely on to stay in sync, leaving the form model stale.
    const email = this.emailInput.nativeElement.value.trim();
    const password = this.passwordInput.nativeElement.value;

    if (!email || !password) {
      this.error.set('Please enter both your email and password.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.auth.login(email, password).subscribe({
      next: () => {
        const role = this.auth.role();
        this.router.navigate([role === 'ADMIN' ? '/admin/pending-approvals' : '/recruiter/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Login failed. Please try again.');
      },
    });
  }
}
