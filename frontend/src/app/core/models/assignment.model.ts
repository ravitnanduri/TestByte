import { AssessmentLanguage, QuestionType } from './assessment.model';

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
  reviewed: boolean;
}

export interface ProctoringEvent {
  type: string;
  timestamp: string;
}

// Candidate-facing question shape -- deliberately has no "correct" flag on options.
export interface PublicQuestionOption {
  id: number;
  text: string;
}

export interface PublicQuestion {
  id: number;
  type: QuestionType;
  prompt: string;
  language: AssessmentLanguage | null;
  starterCode: string | null;
  editorFontSize: number | null;
  editorFontColor: string | null;
  options: PublicQuestionOption[];
}

export interface PublicPage {
  id: number;
  durationMinutes: number;
  questions: PublicQuestion[];
}

export interface PublicAssignment {
  candidateName: string;
  roleAppliedFor: string;
  testTitle: string;
  pages: PublicPage[];
  status: AssignmentStatus;
  startedAt: string | null;
  expiresAt: string;
}

export interface AnswerRequest {
  questionId: number;
  answerText?: string | null;
  selectedOptionId?: number | null;
}

// Recruiter review-facing question shape -- carries "correct" plus the candidate's answer.
export interface ReviewQuestionOption {
  id: number;
  text: string;
  correct: boolean;
}

export interface ReviewQuestionAnswer {
  questionId: number | null;
  type: QuestionType;
  prompt: string;
  language: AssessmentLanguage | null;
  starterCode: string | null;
  editorFontSize: number | null;
  editorFontColor: string | null;
  options: ReviewQuestionOption[];
  answerText: string | null;
  selectedOptionId: number | null;
  selectedOptionText: string | null;
  selectedOptionCorrect: boolean | null;
}

export interface ReviewPage {
  durationMinutes: number;
  questions: ReviewQuestionAnswer[];
}

export interface AssignmentReview {
  id: number;
  candidateName: string;
  roleAppliedFor: string;
  testTitle: string;
  pages: ReviewPage[];
  orphanedAnswers: ReviewQuestionAnswer[];
  status: AssignmentStatus;
  createdAt: string;
  startedAt: string | null;
  submittedAt: string | null;
  proctoringEvents: string | null;
  reviewComment: string | null;
  reviewedAt: string | null;
  reviewedByName: string | null;
}
