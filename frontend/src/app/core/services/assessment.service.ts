import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Assessment, CreateAssessmentRequest } from '../models/assessment.model';

@Injectable({ providedIn: 'root' })
export class AssessmentService {
  private readonly baseUrl = `${environment.apiBaseUrl}/tests`;

  constructor(private http: HttpClient) {}

  list(): Observable<Assessment[]> {
    return this.http.get<Assessment[]>(this.baseUrl);
  }

  create(request: CreateAssessmentRequest): Observable<Assessment> {
    return this.http.post<Assessment>(this.baseUrl, request);
  }
}
