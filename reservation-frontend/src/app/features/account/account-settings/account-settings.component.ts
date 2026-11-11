import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AccountService } from '../../../core/services/account.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserProfileResponse } from '../../../core/models/account.model';

export type SettingsTab = 'PROFILE' | 'SECURITY' | 'PRIVACY';

@Component({
  selector: 'app-account-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './account-settings.component.html',
  styleUrl: './account-settings.component.scss'
})
export class AccountSettingsComponent implements OnInit {
  private readonly accountService = inject(AccountService);
  readonly authService = inject(AuthService);

  readonly activeTab = signal<SettingsTab>('PROFILE');
  readonly profile = signal<UserProfileResponse | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly isSavingProfile = signal<boolean>(false);
  readonly isSendingOtp = signal<boolean>(false);
  readonly isChangingPassword = signal<boolean>(false);
  readonly isDeletingAccount = signal<boolean>(false);
  readonly isCancellingDeletion = signal<boolean>(false);

  readonly toastMessage = signal<string | null>(null);
  readonly toastType = signal<'SUCCESS' | 'ERROR'>('SUCCESS');

  readonly editFirstName = signal<string>('');
  readonly editLastName = signal<string>('');
  readonly editPhone = signal<string>('');

  readonly otpCode = signal<string>('');
  readonly newPassword = signal<string>('');
  readonly confirmPassword = signal<string>('');
  readonly showNewPassword = signal<boolean>(false);
  readonly isOtpSent = signal<boolean>(false);
  readonly otpCountdown = signal<number>(0);
  private countdownTimer: any = null;

  readonly isDeleteModalOpen = signal<boolean>(false);
  readonly deletePassword = signal<string>('');
  readonly deleteReason = signal<string>('');
  readonly showDeletePassword = signal<boolean>(false);
  readonly deleteModalError = signal<string | null>(null);

  readonly remainingGraceDays = computed(() => {
    const prof = this.profile();
    if (!prof || !prof.scheduledDeletionDate) return 0;
    const scheduled = new Date(prof.scheduledDeletionDate).getTime();
    const now = new Date().getTime();
    const diff = scheduled - now;
    return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)));
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.accountService.getProfile().subscribe({
      next: (data) => {
        this.profile.set(data);
        this.editFirstName.set(data.firstName || '');
        this.editLastName.set(data.lastName || '');
        this.editPhone.set(data.phone || '');
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.showToast('No fue posible cargar la información de perfil.', 'ERROR');
      }
    });
  }

  switchTab(tab: SettingsTab): void {
    this.activeTab.set(tab);
  }

  saveProfile(): void {
    const firstName = this.editFirstName().trim();
    const lastName = this.editLastName().trim();
    const phone = this.editPhone().trim();

    if (!firstName || !lastName) {
      this.showToast('Nombre y Apellidos son campos obligatorios.', 'ERROR');
      return;
    }

    this.isSavingProfile.set(true);
    this.accountService.updateProfile({ firstName, lastName, phone: phone || null }).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.isSavingProfile.set(false);
        this.showToast('Perfil actualizado correctamente.', 'SUCCESS');
      },
      error: (err) => {
        this.isSavingProfile.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al actualizar el perfil.';
        this.showToast(msg, 'ERROR');
      }
    });
  }

  requestOtp(): void {
    if (this.otpCountdown() > 0) return;

    this.isSendingOtp.set(true);
    this.accountService.requestPasswordChangeOtp().subscribe({
      next: () => {
        this.isSendingOtp.set(false);
        this.isOtpSent.set(true);
        this.startCooldown(60);
        this.showToast('Código de seguridad enviado a su correo registrado.', 'SUCCESS');
      },
      error: (err) => {
        this.isSendingOtp.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al enviar el código de seguridad.';
        this.showToast(msg, 'ERROR');
      }
    });
  }

  private startCooldown(seconds: number): void {
    this.otpCountdown.set(seconds);
    if (this.countdownTimer) clearInterval(this.countdownTimer);
    this.countdownTimer = setInterval(() => {
      this.otpCountdown.update(v => {
        if (v <= 1) {
          clearInterval(this.countdownTimer);
          return 0;
        }
        return v - 1;
      });
    }, 1000);
  }

  submitChangePassword(): void {
    const code = this.otpCode().trim();
    const newPass = this.newPassword().trim();
    const confirmPass = this.confirmPassword().trim();

    if (!code || code.length < 6) {
      this.showToast('Ingrese el código de 6 dígitos enviado a su correo.', 'ERROR');
      return;
    }

    const strongPasswordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
    if (!newPass || !strongPasswordRegex.test(newPass)) {
      this.showToast('La nueva contraseña debe tener al menos 8 caracteres, incluyendo mayúscula, minúscula y número.', 'ERROR');
      return;
    }

    if (newPass !== confirmPass) {
      this.showToast('Las contraseñas ingresadas no coinciden.', 'ERROR');
      return;
    }

    this.isChangingPassword.set(true);
    this.accountService.changePassword({ code, newPassword: newPass }).subscribe({
      next: () => {
        this.isChangingPassword.set(false);
        this.otpCode.set('');
        this.newPassword.set('');
        this.confirmPassword.set('');
        this.isOtpSent.set(false);
        this.showToast('Contraseña actualizada con éxito.', 'SUCCESS');
      },
      error: (err) => {
        this.isChangingPassword.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al actualizar la contraseña.';
        this.showToast(msg, 'ERROR');
      }
    });
  }

  openDeleteModal(): void {
    this.deletePassword.set('');
    this.deleteReason.set('');
    this.deleteModalError.set(null);
    this.showDeletePassword.set(false);
    this.isDeleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    this.isDeleteModalOpen.set(false);
    this.deleteModalError.set(null);
  }

  confirmAccountDeletion(): void {
    const password = this.deletePassword().trim();
    const reason = this.deleteReason().trim();

    if (!password) {
      this.deleteModalError.set('Ingrese su contraseña actual para confirmar la solicitud.');
      return;
    }

    this.isDeletingAccount.set(true);
    this.deleteModalError.set(null);

    this.accountService.requestAccountDeletion({ password, reason: reason || undefined }).subscribe({
      next: (status) => {
        this.isDeletingAccount.set(false);
        this.closeDeleteModal();
        this.profile.update(p => p ? {
          ...p,
          pendingDeletion: status.pendingDeletion,
          deletionRequestedAt: status.deletionRequestedAt,
          scheduledDeletionDate: status.scheduledDeletionDate
        } : null);
        this.showToast('Solicitud registrada. Su cuenta entrará en el periodo de gracia de 30 días.', 'SUCCESS');
      },
      error: (err) => {
        this.isDeletingAccount.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al procesar la solicitud de eliminación.';
        this.deleteModalError.set(msg);
      }
    });
  }

  cancelDeletion(): void {
    this.isCancellingDeletion.set(true);
    this.accountService.cancelAccountDeletion().subscribe({
      next: (status) => {
        this.isCancellingDeletion.set(false);
        this.profile.update(p => p ? {
          ...p,
          pendingDeletion: false,
          deletionRequestedAt: null,
          scheduledDeletionDate: null
        } : null);
        this.showToast('Eliminación cancelada. Su cuenta se encuentra plenamente activa.', 'SUCCESS');
      },
      error: (err) => {
        this.isCancellingDeletion.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al cancelar la eliminación.';
        this.showToast(msg, 'ERROR');
      }
    });
  }

  showToast(message: string, type: 'SUCCESS' | 'ERROR' = 'SUCCESS'): void {
    this.toastMessage.set(message);
    this.toastType.set(type);
    setTimeout(() => {
      this.toastMessage.set(null);
    }, 4500);
  }

  togglePasswordVisibility(): void {
    this.showNewPassword.update(v => !v);
  }

  toggleDeletePasswordVisibility(): void {
    this.showDeletePassword.update(v => !v);
  }

  getRoleDisplayName(role?: string): string {
    if (!role) return 'Huésped Registrado';
    const cleanRole = role.replace('ROLE_', '').toUpperCase();
    switch (cleanRole) {
      case 'ADMIN':
        return 'Administrador General';
      case 'RECEPTIONIST':
        return 'Front Desk / Recepción';
      case 'CUSTOMER':
        return 'Huésped Registrado';
      default:
        return cleanRole;
    }
  }
}
