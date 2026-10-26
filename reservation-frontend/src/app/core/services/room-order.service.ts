import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateRoomOrderRequest, ExtraService, OrderStatus, RoomOrder } from '../models/room-order.model';

@Injectable({
  providedIn: 'root'
})
export class RoomOrderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/room-orders`;
  private readonly extraServicesUrl = `${environment.apiUrl}/extra-services`;

  getExtraServices(): Observable<ExtraService[]> {
    return this.http.get<ExtraService[]>(this.extraServicesUrl);
  }

  createOrder(request: CreateRoomOrderRequest): Observable<RoomOrder> {
    return this.http.post<RoomOrder>(this.apiUrl, request);
  }

  getActiveKitchenOrders(): Observable<RoomOrder[]> {
    return this.http.get<RoomOrder[]>(`${this.apiUrl}/active`);
  }

  getAllOrders(): Observable<RoomOrder[]> {
    return this.http.get<RoomOrder[]>(this.apiUrl);
  }

  getOrdersByReservation(reservationId: number): Observable<RoomOrder[]> {
    return this.http.get<RoomOrder[]>(`${this.apiUrl}/reservation/${reservationId}`);
  }

  updateOrderStatus(orderId: number, status: OrderStatus): Observable<RoomOrder> {
    return this.http.patch<RoomOrder>(`${this.apiUrl}/${orderId}/status`, { status });
  }
}
