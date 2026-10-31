import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { kitchenGuard } from './core/guards/kitchen.guard';

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
    path: 'rooms/:id',
    loadComponent: () => import('./features/rooms/room-detail/room-detail.component').then(m => m.RoomDetailComponent)
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
    path: 'kitchen/orders',
    canActivate: [kitchenGuard],
    loadComponent: () => import('./features/kitchen/orders/kitchen-orders.component').then(m => m.KitchenOrdersComponent)
  },
  {
    path: 'kitchen',
    redirectTo: 'kitchen/orders'
  },
  {
    path: 'account/settings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/account/account-settings/account-settings.component').then(m => m.AccountSettingsComponent)
  },
  {
    path: 'profile',
    redirectTo: 'account/settings'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
