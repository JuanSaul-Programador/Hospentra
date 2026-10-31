import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccountDeletionStatusResponse,
  ChangePasswordRequest,
  RequestAccountDeletionRequest,
  UpdateProfileRequest,
  UserProfileResponse
} from '../models/account.model';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/account`;

  getProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(`${this.apiUrl}/profile`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(`${this.apiUrl}/profile`, request);
  }

  requestPasswordChangeOtp(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/change-password/request-otp`, {});
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/change-password`, request);
  }

  requestAccountDeletion(request: RequestAccountDeletionRequest): Observable<AccountDeletionStatusResponse> {
    return this.http.post<AccountDeletionStatusResponse>(`${this.apiUrl}/request-deletion`, request);
  }

  cancelAccountDeletion(): Observable<AccountDeletionStatusResponse> {
    return this.http.post<AccountDeletionStatusResponse>(`${this.apiUrl}/cancel-deletion`, {});
  }
}
