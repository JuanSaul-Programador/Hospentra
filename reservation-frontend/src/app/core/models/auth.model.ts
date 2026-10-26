export type Role = 'ROLE_ADMIN' | 'ROLE_RECEPTIONIST' | 'ROLE_KITCHEN' | 'ROLE_CUSTOMER';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  username: string;
  email: string;
  role: Role;
}
