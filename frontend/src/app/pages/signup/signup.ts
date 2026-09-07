import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-signup',
  imports: [RouterLink],
  templateUrl: './signup.html',
})
export class Signup {
  private auth = inject(AuthService);

  @ViewChild('nameInput') nameInput!: ElementRef<HTMLInputElement>;
  @ViewChild('emailInput') emailInput!: ElementRef<HTMLInputElement>;
  @ViewChild('passwordInput') passwordInput!: ElementRef<HTMLInputElement>;

  loading = signal(false);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  submit(): void {
    // Read straight from the DOM rather than a reactive-forms value: some
    // browsers/password managers fill inputs without firing the events
    // reactive forms rely on to stay in sync, leaving the form model stale.
    const name = this.nameInput.nativeElement.value.trim();
    const email = this.emailInput.nativeElement.value.trim();
    const password = this.passwordInput.nativeElement.value;

    if (!name || !email || password.length < 8) {
      this.error.set('Please fill in your name, email, and an 8+ character password.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.auth.signup(name, email, password).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.successMessage.set(response.message);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Signup failed. Please try again.');
      },
    });
  }
}
