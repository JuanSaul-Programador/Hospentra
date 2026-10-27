import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { I18nService } from '../../../core/services/i18n.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  readonly isMobileMenuOpen = signal(false);
  readonly isUserMenuOpen = signal(false);

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  toggleUserMenu(): void {
    this.isUserMenuOpen.update(v => !v);
  }

  closeMenus(): void {
    this.isMobileMenuOpen.set(false);
    this.isUserMenuOpen.set(false);
  }

  logout(): void {
    this.closeMenus();
    this.authService.logout();
    this.router.navigate(['/']);
  }

  getRoleLabel(role: string | null): string {
    switch (role) {
      case 'ROLE_ADMIN':
        return this.i18n.t('nav.admin');
      case 'ROLE_RECEPTIONIST':
        return this.i18n.t('nav.receptionist');
      case 'ROLE_CUSTOMER':
        return this.i18n.t('nav.customer');
      default:
        return '';
    }
  }
}
