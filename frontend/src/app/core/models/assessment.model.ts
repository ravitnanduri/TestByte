export type AssessmentLanguage = 'JAVA' | 'PYTHON' | 'SQL' | 'JAVASCRIPT' | 'TYPESCRIPT';
export type QuestionType = 'CODE' | 'TEXT' | 'MULTIPLE_CHOICE';

export interface QuestionOption {
  id?: number;
  text: string;
  correct: boolean;
}

export interface Question {
  id?: number;
  type: QuestionType;
  prompt: string;
  language?: AssessmentLanguage | null;
  starterCode?: string | null;
  editorFontSize?: number | null;
  editorFontColor?: string | null;
  options?: QuestionOption[];
}

export interface TestPageContent {
  id?: number;
  durationMinutes: number;
  questions: Question[];
}

export interface Assessment {
  id: number;
  title: string;
  pages: TestPageContent[];
  createdAt: string;
  totalDurationMinutes: number;
  questionCount: number;
}

export interface CreateAssessmentRequest {
  title: string;
  pages: TestPageContent[];
}
