import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reservation, ReservationRequest, ReservationStatus, PreCheckInRequest } from '../models/reservation.model';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/reservations`;
  private readonly storageKey = 'hospentra_payment_metadata';

  private getStoredMetadata(): Record<number, Partial<Reservation>> {
    try {
      const raw = localStorage.getItem(this.storageKey);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  }

  savePaymentMeta(id: number, meta: Partial<Reservation>): void {
    try {
      const all = this.getStoredMetadata();
      all[id] = { ...all[id], ...meta };
      localStorage.setItem(this.storageKey, JSON.stringify(all));
    } catch {}
  }

  getPaymentMeta(id: number): Partial<Reservation> | null {
    const all = this.getStoredMetadata();
    return all[id] || null;
  }

  enrichReservation(res: Reservation): Reservation {
    const meta = this.getPaymentMeta(res.id);
    return {
      ...res,
      paymentMethod: res.paymentMethod || meta?.paymentMethod || 'PAY_AT_CHECKIN',
      paymentStatus: res.paymentStatus || meta?.paymentStatus || (meta?.paymentMethod === 'CREDIT_CARD' ? 'PAID' : 'PENDING'),
      cardLast4: res.cardLast4 || meta?.cardLast4,
      cardBrand: res.cardBrand || meta?.cardBrand,
      authCode: res.authCode || meta?.authCode,
      refundAmount: res.refundAmount ?? meta?.refundAmount,
      penaltyAmount: res.penaltyAmount ?? meta?.penaltyAmount
    };
  }

  createReservation(request: ReservationRequest): Observable<Reservation> {
    return this.http.post<Reservation>(this.apiUrl, request).pipe(
      map(res => {
        if (request.paymentMethod) {
          const meta: Partial<Reservation> = {
            paymentMethod: request.paymentMethod,
            paymentStatus: request.paymentStatus || (request.paymentMethod === 'CREDIT_CARD' ? 'PAID' : 'PENDING'),
            cardLast4: request.cardLast4,
            cardBrand: request.cardBrand,
            authCode: request.authCode
          };
          this.savePaymentMeta(res.id, meta);
        }
        return this.enrichReservation(res);
      })
    );
  }

  getReservations(status?: ReservationStatus): Observable<Reservation[]> {
    const url = status ? `${this.apiUrl}?status=${status}` : this.apiUrl;
    return this.http.get<Reservation[]>(url).pipe(
      map(list => (list || []).map(r => this.enrichReservation(r)))
    );
  }

  getMyReservations(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.apiUrl}/my`).pipe(
      map(list => (list || []).map(r => this.enrichReservation(r)))
    );
  }

  getReservationById(id: number): Observable<Reservation> {
    return this.http.get<Reservation>(`${this.apiUrl}/${id}`).pipe(
      map(res => this.enrichReservation(res))
    );
  }

  getReservationsByGuest(guestId: number): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.apiUrl}/guest/${guestId}`).pipe(
      map(list => (list || []).map(r => this.enrichReservation(r)))
    );
  }

  cancelReservation(id: number): Observable<Reservation> {
    return this.http.patch<Reservation>(`${this.apiUrl}/${id}/cancel`, {}).pipe(
      map(res => this.enrichReservation(res))
    );
  }

  confirmReservation(id: number): Observable<Reservation> {
    return this.http.patch<Reservation>(`${this.apiUrl}/${id}/confirm`, {}).pipe(
      map(res => this.enrichReservation(res))
    );
  }

  checkInReservation(id: number): Observable<Reservation> {
    return this.http.patch<Reservation>(`${this.apiUrl}/${id}/check-in`, {}).pipe(
      map(res => {
        const meta = this.getPaymentMeta(id);
        if (meta?.paymentMethod === 'PAY_AT_CHECKIN') {
          this.savePaymentMeta(id, { paymentStatus: 'PAID' });
        }
        return this.enrichReservation(res);
      })
    );
  }

  completeReservation(id: number): Observable<Reservation> {
    return this.http.patch<Reservation>(`${this.apiUrl}/${id}/complete`, {}).pipe(
      map(res => this.enrichReservation(res))
    );
  }

  submitPreCheckIn(id: number, request: PreCheckInRequest): Observable<Reservation> {
    return this.http.patch<Reservation>(`${this.apiUrl}/${id}/pre-check-in`, request).pipe(
      map(res => this.enrichReservation(res))
    );
  }
}
