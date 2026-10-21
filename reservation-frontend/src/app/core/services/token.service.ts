import { Injectable } from '@angular/core';
import { Role } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly TOKEN_KEY = 'hospentra_token';
  private readonly USER_KEY = 'hospentra_user';

  saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  saveUser(user: { username: string; email: string; role: Role }): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  getUser(): { username: string; email: string; role: Role } | null {
    const user = localStorage.getItem(this.USER_KEY);
    return user ? JSON.parse(user) : null;
  }

  clear(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  hasToken(): boolean {
    return !!this.getToken();
  }
}
