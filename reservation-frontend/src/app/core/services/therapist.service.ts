import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateTherapistRequest, Therapist, UpdateTherapistRequest } from '../models/therapist.model';

@Injectable({
  providedIn: 'root'
})
export class TherapistService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/therapists`;

  getTherapists(): Observable<Therapist[]> {
    return this.http.get<Therapist[]>(this.apiUrl);
  }

  getActiveTherapists(): Observable<Therapist[]> {
    return this.http.get<Therapist[]>(`${this.apiUrl}/active`);
  }

  getTherapistById(id: number): Observable<Therapist> {
    return this.http.get<Therapist>(`${this.apiUrl}/${id}`);
  }

  createTherapist(request: CreateTherapistRequest): Observable<Therapist> {
    return this.http.post<Therapist>(this.apiUrl, request);
  }

  updateTherapist(id: number, request: UpdateTherapistRequest): Observable<Therapist> {
    return this.http.put<Therapist>(`${this.apiUrl}/${id}`, request);
  }

  toggleStatus(id: number): Observable<Therapist> {
    return this.http.patch<Therapist>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  deleteTherapist(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
