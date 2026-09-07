import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token;

  const authorizedReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authorizedReq).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      if (error.status === 401 && auth.isLoggedIn()) {
        auth.logout();
        router.navigate(['/login']);
      }

      // status 0 means the request never reached the server (offline, CORS, server down) -
      // in that case error.error is a raw browser/fetch Error, not our API's JSON error body.
      if (error.status === 0) {
        return throwError(
          () =>
            new HttpErrorResponse({
              headers: error.headers,
              status: error.status,
              statusText: error.statusText,
              url: error.url ?? undefined,
              error: { message: 'Unable to reach the server. Please check your connection and try again.' },
            })
        );
      }

      return throwError(() => error);
    })
  );
};
