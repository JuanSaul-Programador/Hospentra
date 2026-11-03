import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { I18nService } from '../../../core/services/i18n.service';
import { InAppNotificationService } from '../../../core/services/in-app-notification.service';
import { InAppNotification } from '../../../core/models/notification.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent implements OnInit {
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly notificationService = inject(InAppNotificationService);
  private readonly router = inject(Router);

  readonly isMobileMenuOpen = signal(false);
  readonly isUserMenuOpen = signal(false);

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.notificationService.loadUnreadCount();
    }
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  toggleUserMenu(): void {
    this.notificationService.closeDropdown();
    this.isUserMenuOpen.update(v => !v);
  }

  toggleNotifications(event: MouseEvent): void {
    event.stopPropagation();
    this.isUserMenuOpen.set(false);
    this.notificationService.toggleDropdown();
  }

  closeMenus(): void {
    this.isMobileMenuOpen.set(false);
    this.isUserMenuOpen.set(false);
    this.notificationService.closeDropdown();
  }

  markNotificationAsRead(notif: InAppNotification, event: MouseEvent): void {
    event.stopPropagation();
    if (!notif.read) {
      this.notificationService.markAsRead(notif.id).subscribe();
    }
    this.notificationService.closeDropdown();
    if (notif.link) {
      this.router.navigateByUrl(notif.link);
    }
  }

  markAllNotificationsAsRead(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationService.markAllAsRead().subscribe();
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'PRE_CHECK_IN':
        return 'verified_user';
      case 'ROOM_SERVICE':
        return 'room_service';
      case 'RESERVATION':
        return 'luggage';
      case 'CHECK_IN':
        return 'key';
      default:
        return 'notifications';
    }
  }

  dismissToast(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationService.dismissToast();
  }

  onToastClick(item: InAppNotification): void {
    this.notificationService.dismissToast();
    if (!item.read) {
      this.notificationService.markAsRead(item.id).subscribe();
    }
    if (item.link) {
      this.router.navigateByUrl(item.link);
    }
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
