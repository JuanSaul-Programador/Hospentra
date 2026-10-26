import { Role } from './auth.model';

export type Shift = 'MORNING' | 'EVENING' | 'NIGHT' | 'FLEXIBLE';
export type WorkStatus = 'ON_DUTY' | 'ON_BREAK' | 'OFF_DUTY';

export interface CreateStaffRequest {
  username: string;
  email: string;
  password: string;
  role: Role;
  shift?: Shift;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
  shift?: Shift;
  workStatus?: WorkStatus;
  breakStartedAt?: string | null;
}
