import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuditFilterParams,
  AuditLogPageResponse,
  AuditMetrics
} from '../models/audit.model';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/audit`;

  getLogs(params: AuditFilterParams = {}): Observable<AuditLogPageResponse> {
    let httpParams = new HttpParams();

    if (params.action && params.action.trim()) {
      httpParams = httpParams.set('action', params.action.trim());
    }
    if (params.username && params.username.trim()) {
      httpParams = httpParams.set('username', params.username.trim());
    }
    if (params.startDate) {
      httpParams = httpParams.set('startDate', params.startDate);
    }
    if (params.endDate) {
      httpParams = httpParams.set('endDate', params.endDate);
    }
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<AuditLogPageResponse>(`${this.baseUrl}/logs`, { params: httpParams });
  }

  getActions(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/actions`);
  }

  getMetrics(): Observable<AuditMetrics> {
    return this.http.get<AuditMetrics>(`${this.baseUrl}/metrics`);
  }

  exportReservationsCsv(startDate?: string, endDate?: string): Observable<Blob> {
    let httpParams = new HttpParams();
    if (startDate) httpParams = httpParams.set('startDate', startDate);
    if (endDate) httpParams = httpParams.set('endDate', endDate);

    return this.http.get(`${this.baseUrl}/export/reservations`, {
      params: httpParams,
      responseType: 'blob'
    });
  }

  exportFinancialJournalCsv(startDate?: string, endDate?: string): Observable<Blob> {
    let httpParams = new HttpParams();
    if (startDate) httpParams = httpParams.set('startDate', startDate);
    if (endDate) httpParams = httpParams.set('endDate', endDate);

    return this.http.get(`${this.baseUrl}/export/financial-journal`, {
      params: httpParams,
      responseType: 'blob'
    });
  }
}
