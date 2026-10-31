import { Role } from './auth.model';

export interface UserProfileResponse {
  id: number;
  username: string;
  email: string;
  role: Role;
  emailVerified: boolean;
  firstName: string;
  lastName: string;
  phone?: string | null;
  deletionRequestedAt?: string | null;
  scheduledDeletionDate?: string | null;
  pendingDeletion: boolean;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phone?: string | null;
}

export interface ChangePasswordRequest {
  code: string;
  newPassword: string;
}

export interface RequestAccountDeletionRequest {
  password: string;
  reason?: string;
}

export interface AccountDeletionStatusResponse {
  pendingDeletion: boolean;
  deletionRequestedAt?: string | null;
  scheduledDeletionDate?: string | null;
  message: string;
}
