export interface PendingRecruiter {
  id: number;
  name: string;
  email: string;
  createdAt: string;
}

export interface SettingsResponse {
  approverNotificationEmail: string;
}
