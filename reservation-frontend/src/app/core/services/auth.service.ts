import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, Role } from '../models/auth.model';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  private readonly currentUserSignal = signal<{ username: string; email: string; role: Role } | null>(
    this.tokenService.getUser()
  );

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.currentUserSignal());
  readonly userRole = computed(() => this.currentUserSignal()?.role ?? null);

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response) => {
        this.tokenService.saveToken(response.token);
        if (response.refreshToken) {
          this.tokenService.saveRefreshToken(response.refreshToken);
        }
        const user = { username: response.username, email: response.email, role: response.role };
        this.tokenService.saveUser(user);
        this.currentUserSignal.set(user);
      })
    );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data).pipe(
      tap((response) => {
        if (!response.verificationRequired && response.token) {
          this.tokenService.saveToken(response.token);
          if (response.refreshToken) {
            this.tokenService.saveRefreshToken(response.refreshToken);
          }
          const user = { username: response.username, email: response.email, role: response.role };
          this.tokenService.saveUser(user);
          this.currentUserSignal.set(user);
        }
      })
    );
  }

  verifyEmail(email: string, code: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/verify-email`, { email, code }).pipe(
      tap((response) => {
        if (response.token) {
          this.tokenService.saveToken(response.token);
          if (response.refreshToken) {
            this.tokenService.saveRefreshToken(response.refreshToken);
          }
          const user = { username: response.username, email: response.email, role: response.role };
          this.tokenService.saveUser(user);
          this.currentUserSignal.set(user);
        }
      })
    );
  }

  resendVerification(email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/resend-verification`, { email });
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.tokenService.getRefreshToken();
    if (!refreshToken) {
      this.logout();
      throw new Error('No refresh token available');
    }

    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      tap({
        next: (response) => {
          this.tokenService.saveToken(response.token);
          if (response.refreshToken) {
            this.tokenService.saveRefreshToken(response.refreshToken);
          }
          const user = { username: response.username, email: response.email, role: response.role };
          this.tokenService.saveUser(user);
          this.currentUserSignal.set(user);
        },
        error: () => {
          this.logout();
        }
      })
    );
  }

  logout(): void {
    this.tokenService.clear();
    this.currentUserSignal.set(null);
  }

  hasRole(role: Role): boolean {
    return this.userRole() === role;
  }
}
