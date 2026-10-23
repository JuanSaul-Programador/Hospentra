import { Room } from './room.model';

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CHECKED_IN' | 'CANCELLED' | 'COMPLETED';

export type PaymentMethod = 'CREDIT_CARD' | 'PAY_AT_CHECKIN';

export type PaymentStatus = 'PENDING' | 'PAID' | 'REFUNDED' | 'PARTIALLY_REFUNDED';

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
  paymentMethod?: PaymentMethod;
  paymentStatus?: PaymentStatus;
  cardLast4?: string;
  cardBrand?: string;
  authCode?: string;
  refundAmount?: number;
  penaltyAmount?: number;
}

export interface ReservationRequest {
  roomId: number;
  checkInDate: string;
  checkOutDate: string;
  guestId?: number;
  paymentMethod?: PaymentMethod;
  paymentStatus?: PaymentStatus;
  cardLast4?: string;
  cardBrand?: string;
  authCode?: string;
}
