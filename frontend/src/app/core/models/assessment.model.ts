export type AssessmentLanguage = 'JAVA' | 'PYTHON' | 'SQL' | 'JAVASCRIPT' | 'TYPESCRIPT';

export interface Assessment {
  id: number;
  title: string;
  language: AssessmentLanguage;
  instructionsHtml: string;
  starterCode: string;
  durationMinutes: number;
  createdAt: string;
}

export interface CreateAssessmentRequest {
  title: string;
  language: AssessmentLanguage;
  instructionsHtml: string;
  starterCode: string;
  aiTrapPhrase?: string;
  durationMinutes: number;
}
