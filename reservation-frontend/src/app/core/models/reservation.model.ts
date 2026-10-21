import { Room } from './room.model';

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';

export interface Guest {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
}

export interface Reservation {
  id: number;
  guest: Guest;
  room: Room;
  checkInDate: string;
  checkOutDate: string;
  status: ReservationStatus;
  totalPrice: number;
  createdAt: string;
}

export interface ReservationRequest {
  roomId: number;
  checkInDate: string;
  checkOutDate: string;
  guestId?: number;
}
