import { Role } from './auth.model';

export interface CreateStaffRequest {
  username: string;
  email: string;
  password: string;
  role: Role;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}
