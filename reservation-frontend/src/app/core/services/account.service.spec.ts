import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AccountService } from './account.service';
import { environment } from '../../../environments/environment';
import {
  AccountDeletionStatusResponse,
  ChangePasswordRequest,
  RequestAccountDeletionRequest,
  UpdateProfileRequest,
  UserProfileResponse
} from '../models/account.model';

describe('AccountService', () => {
  let service: AccountService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/account`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AccountService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AccountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch user profile via GET', () => {
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

    service.getProfile().subscribe(res => {
      expect(res).toEqual(mockProfile);
      expect(res.email).toBe('client1@hotel.com');
      expect(res.pendingDeletion).toBe(false);
    });

    const req = httpMock.expectOne(`${baseUrl}/profile`);
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
  });

  it('should update user profile via PUT', () => {
    const updateReq: UpdateProfileRequest = {
      firstName: 'Elena Maria',
      lastName: 'Rostova',
      phone: '+1 555-9999'
    };

    const mockUpdated: UserProfileResponse = {
      id: 1,
      username: 'client1',
      email: 'client1@hotel.com',
      role: 'ROLE_CUSTOMER',
      emailVerified: true,
      firstName: 'Elena Maria',
      lastName: 'Rostova',
      phone: '+1 555-9999',
      deletionRequestedAt: null,
      scheduledDeletionDate: null,
      pendingDeletion: false
    };

    service.updateProfile(updateReq).subscribe(res => {
      expect(res.firstName).toBe('Elena Maria');
      expect(res.phone).toBe('+1 555-9999');
    });

    const req = httpMock.expectOne(`${baseUrl}/profile`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updateReq);
    req.flush(mockUpdated);
  });

  it('should request password change OTP via POST', () => {
    service.requestPasswordChangeOtp().subscribe();

    const req = httpMock.expectOne(`${baseUrl}/change-password/request-otp`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(null);
  });

  it('should submit password change with OTP via POST', () => {
    const changeReq: ChangePasswordRequest = {
      code: '123456',
      newPassword: 'NewSanctuaryPass2026!'
    };

    service.changePassword(changeReq).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/change-password`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(changeReq);
    req.flush(null);
  });

  it('should request account deletion via POST', () => {
    const delReq: RequestAccountDeletionRequest = {
      password: 'CurrentPass123!'
    };

    const mockStatus: AccountDeletionStatusResponse = {
      deletionRequestedAt: '2026-10-31T12:00:00',
      scheduledDeletionDate: '2026-11-30T12:00:00',
      pendingDeletion: true,
      message: 'Account scheduled for deletion'
    };

    service.requestAccountDeletion(delReq).subscribe(res => {
      expect(res.pendingDeletion).toBe(true);
      expect(res.scheduledDeletionDate).toBe('2026-11-30T12:00:00');
    });

    const req = httpMock.expectOne(`${baseUrl}/request-deletion`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(delReq);
    req.flush(mockStatus);
  });

  it('should cancel account deletion via POST', () => {
    const mockStatus: AccountDeletionStatusResponse = {
      deletionRequestedAt: null,
      scheduledDeletionDate: null,
      pendingDeletion: false,
      message: 'Deletion protocol cancelled'
    };

    service.cancelAccountDeletion().subscribe(res => {
      expect(res.pendingDeletion).toBe(false);
      expect(res.scheduledDeletionDate).toBeNull();
    });

    const req = httpMock.expectOne(`${baseUrl}/cancel-deletion`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(mockStatus);
  });
});
