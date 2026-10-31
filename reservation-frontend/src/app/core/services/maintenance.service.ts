import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateMaintenanceBlockRequest,
  MaintenanceBlock
} from '../models/maintenance.model';

@Injectable({
  providedIn: 'root'
})
export class MaintenanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/maintenance-blocks`;

  createBlock(request: CreateMaintenanceBlockRequest): Observable<MaintenanceBlock> {
    return this.http.post<MaintenanceBlock>(this.baseUrl, request);
  }

  getBlocks(): Observable<MaintenanceBlock[]> {
    return this.http.get<MaintenanceBlock[]>(this.baseUrl);
  }

  getActiveBlocks(startDate?: string, endDate?: string): Observable<MaintenanceBlock[]> {
    let params = new HttpParams();
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    return this.http.get<MaintenanceBlock[]>(`${this.baseUrl}/active`, { params });
  }

  resolveBlock(id: number): Observable<MaintenanceBlock> {
    return this.http.patch<MaintenanceBlock>(`${this.baseUrl}/${id}/resolve`, {});
  }

  cancelBlock(id: number): Observable<MaintenanceBlock> {
    return this.http.patch<MaintenanceBlock>(`${this.baseUrl}/${id}/cancel`, {});
  }
}
