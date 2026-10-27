import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Room, RoomRequest, RoomStatus, RoomType, RoomTypeRequest } from '../models/room.model';

@Injectable({
  providedIn: 'root'
})
export class RoomService {
  private readonly http = inject(HttpClient);
  private readonly roomsUrl = `${environment.apiUrl}/rooms`;
  private readonly roomTypesUrl = `${environment.apiUrl}/room-types`;

  getRooms(status?: RoomStatus): Observable<Room[]> {
    if (status) {
      return this.http.get<Room[]>(`${this.roomsUrl}?status=${status}`);
    }
    return this.http.get<Room[]>(this.roomsUrl);
  }

  getRoomById(id: number): Observable<Room> {
    return this.http.get<Room>(`${this.roomsUrl}/${id}`);
  }

  createRoom(request: RoomRequest): Observable<Room> {
    return this.http.post<Room>(this.roomsUrl, request);
  }

  updateRoom(id: number, request: RoomRequest): Observable<Room> {
    return this.http.put<Room>(`${this.roomsUrl}/${id}`, request);
  }

  deleteRoom(id: number): Observable<void> {
    return this.http.delete<void>(`${this.roomsUrl}/${id}`);
  }

  updateRoomStatus(id: number, status: RoomStatus): Observable<Room> {
    return this.http.patch<Room>(`${this.roomsUrl}/${id}/status?status=${status}`, {});
  }

  getRoomTypes(): Observable<RoomType[]> {
    return this.http.get<RoomType[]>(this.roomTypesUrl);
  }

  getRoomTypeById(id: number): Observable<RoomType> {
    return this.http.get<RoomType>(`${this.roomTypesUrl}/${id}`);
  }

  createRoomType(request: RoomTypeRequest): Observable<RoomType> {
    return this.http.post<RoomType>(this.roomTypesUrl, request);
  }

  updateRoomType(id: number, request: RoomTypeRequest): Observable<RoomType> {
    return this.http.put<RoomType>(`${this.roomTypesUrl}/${id}`, request);
  }

  deleteRoomType(id: number): Observable<void> {
    return this.http.delete<void>(`${this.roomTypesUrl}/${id}`);
  }
}
