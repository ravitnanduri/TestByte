import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./pages/login/login').then((m) => m.Login) },
  { path: 'signup', loadComponent: () => import('./pages/signup/signup').then((m) => m.Signup) },

  {
    path: 'admin/pending-approvals',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () =>
      import('./pages/admin/pending-approvals/pending-approvals').then((m) => m.PendingApprovals),
  },
  {
    path: 'admin/settings',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./pages/admin/settings/settings').then((m) => m.Settings),
  },
  {
    path: 'admin/invite-admin',
    canActivate: [roleGuard(['ADMIN'])],
    loadComponent: () => import('./pages/admin/invite-admin/invite-admin').then((m) => m.InviteAdmin),
  },
  {
    path: 'admin/accept-invite',
    loadComponent: () => import('./pages/admin/accept-invite/accept-invite').then((m) => m.AcceptInvite),
  },

  {
    path: 'recruiter/dashboard',
    canActivate: [roleGuard(['RECRUITER', 'ADMIN'])],
    loadComponent: () => import('./pages/recruiter/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'recruiter/tests',
    canActivate: [roleGuard(['RECRUITER', 'ADMIN'])],
    loadComponent: () => import('./pages/recruiter/test-list/test-list').then((m) => m.TestList),
  },
  {
    path: 'recruiter/tests/new',
    canActivate: [roleGuard(['RECRUITER', 'ADMIN'])],
    loadComponent: () => import('./pages/recruiter/test-new/test-new').then((m) => m.TestNew),
  },
  {
    path: 'recruiter/tests/:id/edit',
    canActivate: [roleGuard(['RECRUITER', 'ADMIN'])],
    loadComponent: () => import('./pages/recruiter/test-edit/test-edit').then((m) => m.TestEdit),
  },
  {
    path: 'recruiter/schedule',
    canActivate: [roleGuard(['RECRUITER', 'ADMIN'])],
    loadComponent: () => import('./pages/recruiter/schedule/schedule').then((m) => m.Schedule),
  },
  {
    path: 'recruiter/assignments/:id/review',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/recruiter/assignment-review/assignment-review').then((m) => m.AssignmentReview),
  },

  {
    path: 'test/:token',
    loadComponent: () => import('./pages/candidate/test-page/test-page').then((m) => m.TestPage),
  },

  { path: '**', redirectTo: 'login' },
];
