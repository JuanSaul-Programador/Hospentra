import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    const role = authService.userRole();
    if (role === 'ROLE_ADMIN' || role === 'ROLE_RECEPTIONIST') {
      return true;
    }
  }

  return router.createUrlTree(['/admin/login']);
};
