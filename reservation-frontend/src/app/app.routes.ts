import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'reservations/checkout',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reservations/checkout/checkout.component').then(m => m.CheckoutComponent)
  },
  {
    path: 'reservations/new',
    redirectTo: 'reservations/checkout'
  },
  {
    path: 'my-reservations',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reservations/my-reservations/my-reservations.component').then(m => m.MyReservationsComponent)
  },
  {
    path: 'reservations',
    redirectTo: 'my-reservations'
  },
  {
    path: 'admin/login',
    loadComponent: () => import('./features/admin/login/admin-login.component').then(m => m.AdminLoginComponent)
  },
  {
    path: 'admin/dashboard',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
  },
  {
    path: 'admin/reservations',
    redirectTo: 'admin/dashboard'
  },
  {
    path: 'admin',
    redirectTo: 'admin/dashboard'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
