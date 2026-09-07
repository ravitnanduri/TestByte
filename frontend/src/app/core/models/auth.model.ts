export type Role = 'ADMIN' | 'RECRUITER';
export type UserStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AuthResponse {
  token: string;
  name: string;
  email: string;
  role: Role;
}

export interface SignupResponse {
  status: UserStatus;
  message: string;
}

export interface CurrentUser {
  name: string;
  email: string;
  role: Role;
}
