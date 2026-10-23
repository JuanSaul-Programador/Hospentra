import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Room, RoomStatus, RoomType } from '../models/room.model';

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

  updateRoomStatus(id: number, status: RoomStatus): Observable<Room> {
    return this.http.patch<Room>(`${this.roomsUrl}/${id}/status?status=${status}`, {});
  }

  getRoomTypes(): Observable<RoomType[]> {
    return this.http.get<RoomType[]>(this.roomTypesUrl);
  }

  getRoomTypeById(id: number): Observable<RoomType> {
    return this.http.get<RoomType>(`${this.roomTypesUrl}/${id}`);
  }
}
