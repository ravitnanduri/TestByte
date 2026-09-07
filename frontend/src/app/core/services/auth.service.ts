import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, CurrentUser, Role, SignupResponse } from '../models/auth.model';

const STORAGE_KEY = 'testbyte.auth';

interface StoredAuth {
  token: string;
  user: CurrentUser;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  private readonly state = signal<StoredAuth | null>(this.readFromStorage());

  readonly currentUser = computed(() => this.state()?.user ?? null);
  readonly isLoggedIn = computed(() => this.state() !== null);
  readonly role = computed<Role | null>(() => this.state()?.user.role ?? null);

  constructor(private http: HttpClient) {}

  get token(): string | null {
    return this.state()?.token ?? null;
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, { email, password }).pipe(
      tap((response) => this.setSession(response))
    );
  }

  signup(name: string, email: string, password: string): Observable<SignupResponse> {
    return this.http.post<SignupResponse>(`${this.baseUrl}/signup`, { name, email, password });
  }

  acceptAdminInvite(token: string, name: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/accept-admin-invite`, { token, name, password }).pipe(
      tap((response) => this.setSession(response))
    );
  }

  logout(): void {
    this.state.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  private setSession(response: AuthResponse): void {
    const stored: StoredAuth = {
      token: response.token,
      user: { name: response.name, email: response.email, role: response.role },
    };
    this.state.set(stored);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  }

  private readFromStorage(): StoredAuth | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as StoredAuth) : null;
    } catch {
      return null;
    }
  }
}
