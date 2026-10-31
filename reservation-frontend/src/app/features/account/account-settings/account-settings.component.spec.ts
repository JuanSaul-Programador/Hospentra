import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AccountSettingsComponent } from './account-settings.component';
import { AccountService } from '../../../core/services/account.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserProfileResponse } from '../../../core/models/account.model';

describe('AccountSettingsComponent', () => {
  let component: AccountSettingsComponent;
  let fixture: ComponentFixture<AccountSettingsComponent>;
  let mockAccountService: any;
  let mockAuthService: any;

  const mockProfile: UserProfileResponse = {
    id: 1,
    username: 'client1',
    email: 'client1@hotel.com',
    role: 'ROLE_CUSTOMER',
    emailVerified: true,
    firstName: 'Elena',
    lastName: 'Rostova',
    phone: '+1 555-0199',
    deletionRequestedAt: null,
    scheduledDeletionDate: null,
    pendingDeletion: false
  };

  beforeEach(async () => {
    mockAccountService = {
      getProfile: vi.fn().mockReturnValue(of(mockProfile)),
      updateProfile: vi.fn().mockReturnValue(of({ ...mockProfile, firstName: 'Elena Mod', lastName: 'Rostova Mod' })),
      requestPasswordChangeOtp: vi.fn().mockReturnValue(of(void 0)),
      changePassword: vi.fn().mockReturnValue(of(void 0)),
      requestAccountDeletion: vi.fn().mockReturnValue(of({
        pendingDeletion: true,
        deletionRequestedAt: '2026-10-31T12:00:00',
        scheduledDeletionDate: '2026-11-30T12:00:00',
        message: 'Account scheduled for deletion'
      })),
      cancelAccountDeletion: vi.fn().mockReturnValue(of({
        pendingDeletion: false,
        deletionRequestedAt: null,
        scheduledDeletionDate: null,
        message: 'Cancelled'
      }))
    };

    mockAuthService = {
      currentUser: vi.fn().mockReturnValue({ username: 'client1', email: 'client1@hotel.com' }),
      userRole: vi.fn().mockReturnValue('ROLE_CUSTOMER'),
      logout: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [AccountSettingsComponent],
      providers: [
        provideRouter([]),
        { provide: AccountService, useValue: mockAccountService },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AccountSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load initial profile', () => {
    expect(component).toBeTruthy();
    expect(mockAccountService.getProfile).toHaveBeenCalled();
    expect(component.profile()?.email).toBe('client1@hotel.com');
    expect(component.editFirstName()).toBe('Elena');
    expect(component.editLastName()).toBe('Rostova');
    expect(component.isLoading()).toBe(false);
  });

  it('should switch tabs correctly', () => {
    expect(component.activeTab()).toBe('PROFILE');
    component.switchTab('SECURITY');
    expect(component.activeTab()).toBe('SECURITY');
    component.switchTab('PRIVACY');
    expect(component.activeTab()).toBe('PRIVACY');
  });

  it('should validate and save profile updates', () => {
    component.editFirstName.set('Elena Mod');
    component.editLastName.set('Rostova Mod');
    component.editPhone.set('+1 555-8888');

    component.saveProfile();

    expect(mockAccountService.updateProfile).toHaveBeenCalledWith({
      firstName: 'Elena Mod',
      lastName: 'Rostova Mod',
      phone: '+1 555-8888'
    });
    expect(component.profile()?.firstName).toBe('Elena Mod');
    expect(component.toastType()).toBe('SUCCESS');
  });

  it('should reject profile save if firstName or lastName is blank', () => {
    mockAccountService.updateProfile.mockClear();
    component.editFirstName.set('');
    component.editLastName.set('Rostova');

    component.saveProfile();

    expect(mockAccountService.updateProfile).not.toHaveBeenCalled();
    expect(component.toastType()).toBe('ERROR');
  });

  it('should request password change OTP and activate cooldown', () => {
    component.requestOtp();

    expect(mockAccountService.requestPasswordChangeOtp).toHaveBeenCalled();
    expect(component.isOtpSent()).toBe(true);
    expect(component.otpCountdown()).toBe(60);
    expect(component.toastType()).toBe('SUCCESS');
  });

  it('should validate inputs before submitting password change', () => {
    component.otpCode.set('123');
    component.newPassword.set('short');
    component.confirmPassword.set('different');

    component.submitChangePassword();
    expect(mockAccountService.changePassword).not.toHaveBeenCalled();
    expect(component.toastType()).toBe('ERROR');

    component.otpCode.set('123456');
    component.newPassword.set('ValidPassword123!');
    component.confirmPassword.set('NonMatchingPassword!');
    component.submitChangePassword();
    expect(mockAccountService.changePassword).not.toHaveBeenCalled();
    expect(component.toastType()).toBe('ERROR');

    component.confirmPassword.set('ValidPassword123!');
    component.submitChangePassword();
    expect(mockAccountService.changePassword).toHaveBeenCalledWith({
      code: '123456',
      newPassword: 'ValidPassword123!'
    });
    expect(component.toastType()).toBe('SUCCESS');
  });

  it('should open and close account deletion modal', () => {
    expect(component.isDeleteModalOpen()).toBe(false);
    component.openDeleteModal();
    expect(component.isDeleteModalOpen()).toBe(true);
    component.closeDeleteModal();
    expect(component.isDeleteModalOpen()).toBe(false);
  });

  it('should require password to confirm deletion request', () => {
    component.openDeleteModal();
    component.deletePassword.set('');

    component.confirmAccountDeletion();

    expect(mockAccountService.requestAccountDeletion).not.toHaveBeenCalled();
    expect(component.deleteModalError()).toBeTruthy();
  });

  it('should successfully submit deletion request and update state to pending deletion', () => {
    component.openDeleteModal();
    component.deletePassword.set('MySecretPass123!');
    component.deleteReason.set('Relocating to another country');

    component.confirmAccountDeletion();

    expect(mockAccountService.requestAccountDeletion).toHaveBeenCalledWith({
      password: 'MySecretPass123!',
      reason: 'Relocating to another country'
    });
    expect(component.isDeleteModalOpen()).toBe(false);
    expect(component.profile()?.pendingDeletion).toBe(true);
    expect(component.toastType()).toBe('SUCCESS');
  });

  it('should cancel account deletion and restore normal active status', () => {
    component.profile.set({
      ...mockProfile,
      pendingDeletion: true,
      scheduledDeletionDate: '2026-11-30T12:00:00'
    });

    component.cancelDeletion();

    expect(mockAccountService.cancelAccountDeletion).toHaveBeenCalled();
    expect(component.profile()?.pendingDeletion).toBe(false);
    expect(component.profile()?.scheduledDeletionDate).toBeNull();
    expect(component.toastType()).toBe('SUCCESS');
  });

  it('should calculate remainingGraceDays correctly', () => {
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 25);

    component.profile.set({
      ...mockProfile,
      pendingDeletion: true,
      scheduledDeletionDate: futureDate.toISOString()
    });

    expect(component.remainingGraceDays()).toBe(25);
  });

  it('should map roles to human-friendly display names', () => {
    expect(component.getRoleDisplayName('ROLE_CUSTOMER')).toBe('Huésped Registrado');
    expect(component.getRoleDisplayName('ROLE_ADMIN')).toBe('Administrador General');
    expect(component.getRoleDisplayName('ROLE_RECEPTIONIST')).toBe('Front Desk / Recepción');
    expect(component.getRoleDisplayName(undefined)).toBe('Huésped Registrado');
  });
});
