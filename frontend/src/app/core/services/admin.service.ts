import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PendingRecruiter, SettingsResponse } from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

  constructor(private http: HttpClient) {}

  pendingRecruiters(): Observable<PendingRecruiter[]> {
    return this.http.get<PendingRecruiter[]>(`${this.baseUrl}/pending-recruiters`);
  }

  approve(id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/pending-recruiters/${id}/approve`, {});
  }

  reject(id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/pending-recruiters/${id}/reject`, {});
  }

  getSettings(): Observable<SettingsResponse> {
    return this.http.get<SettingsResponse>(`${this.baseUrl}/settings`);
  }

  updateSettings(approverNotificationEmail: string): Observable<SettingsResponse> {
    return this.http.put<SettingsResponse>(`${this.baseUrl}/settings`, { approverNotificationEmail });
  }

  inviteAdmin(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/invite-admin`, { email });
  }
}
