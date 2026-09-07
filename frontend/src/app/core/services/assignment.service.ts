import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AssignmentReview,
  AssignmentSummary,
  CreateAssignmentRequest,
  PublicAssignment,
} from '../models/assignment.model';

@Injectable({ providedIn: 'root' })
export class AssignmentService {
  private readonly baseUrl = `${environment.apiBaseUrl}/assignments`;
  private readonly publicBaseUrl = `${environment.apiBaseUrl}/public/assignments`;

  constructor(private http: HttpClient) {}

  create(request: CreateAssignmentRequest): Observable<AssignmentSummary> {
    return this.http.post<AssignmentSummary>(this.baseUrl, request);
  }

  list(): Observable<AssignmentSummary[]> {
    return this.http.get<AssignmentSummary[]>(this.baseUrl);
  }

  get(id: number): Observable<AssignmentReview> {
    return this.http.get<AssignmentReview>(`${this.baseUrl}/${id}`);
  }

  saveReview(id: number, comment: string): Observable<AssignmentReview> {
    return this.http.put<AssignmentReview>(`${this.baseUrl}/${id}/review`, { comment });
  }

  getPublic(token: string): Observable<PublicAssignment> {
    return this.http.get<PublicAssignment>(`${this.publicBaseUrl}/${token}`);
  }

  start(token: string): Observable<PublicAssignment> {
    return this.http.post<PublicAssignment>(`${this.publicBaseUrl}/${token}/start`, {});
  }

  submit(token: string, code: string, proctoringEvents: string): Observable<void> {
    return this.http.post<void>(`${this.publicBaseUrl}/${token}/submit`, { code, proctoringEvents });
  }
}
