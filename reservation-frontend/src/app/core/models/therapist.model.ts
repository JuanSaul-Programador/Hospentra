export interface Therapist {
  id: number;
  name: string;
  specialty: string;
  phone: string;
  active: boolean;
  createdAt: string;
}

export interface CreateTherapistRequest {
  name: string;
  specialty: string;
  phone?: string;
  active?: boolean;
}

export interface UpdateTherapistRequest {
  name: string;
  specialty: string;
  phone?: string;
  active?: boolean;
}
