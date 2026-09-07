import { AssessmentLanguage } from '../../core/models/assessment.model';

const MAP: Record<AssessmentLanguage, string> = {
  JAVA: 'java',
  PYTHON: 'python',
  SQL: 'sql',
  JAVASCRIPT: 'javascript',
  TYPESCRIPT: 'typescript',
};

export function toMonacoLanguage(language: AssessmentLanguage): string {
  return MAP[language] ?? 'plaintext';
}
