import { AssessmentLanguage } from './assessment.model';

export type AssignmentStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'EXPIRED';

export interface CreateAssignmentRequest {
  testId: number;
  candidateName: string;
  roleAppliedFor: string;
}

export interface AssignmentSummary {
  id: number;
  candidateName: string;
  roleAppliedFor: string;
  testTitle: string;
  status: AssignmentStatus;
  link: string;
  createdAt: string;
  expiresAt: string;
  recruiterName: string;
  recruiterEmail: string;
}

export interface AssignmentReview {
  id: number;
  candidateName: string;
  roleAppliedFor: string;
  testTitle: string;
  language: AssessmentLanguage;
  instructionsHtml: string;
  starterCode: string;
  submittedCode: string | null;
  possibleAiFlag: boolean;
  status: AssignmentStatus;
  createdAt: string;
  startedAt: string | null;
  submittedAt: string | null;
}

export interface PublicAssignment {
  candidateName: string;
  roleAppliedFor: string;
  testTitle: string;
  language: AssessmentLanguage;
  instructionsHtml: string;
  starterCode: string;
  durationMinutes: number;
  status: AssignmentStatus;
  startedAt: string | null;
  expiresAt: string;
}
