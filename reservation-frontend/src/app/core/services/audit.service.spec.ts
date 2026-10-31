import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuditService } from './audit.service';
import { environment } from '../../../environments/environment';
import { AuditLogPageResponse, AuditMetrics } from '../models/audit.model';

describe('AuditService', () => {
  let service: AuditService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/admin/audit`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuditService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch audit logs with query params', () => {
    const mockResponse: AuditLogPageResponse = {
      content: [
        {
          id: 1,
          username: 'admin1',
          userRole: 'ROLE_ADMIN',
          action: 'CHECK_IN',
          entityName: 'Reservation',
          entityId: 'HOSP-1-2026',
          details: 'Check-in suite 101',
          ipAddress: '127.0.0.1',
          status: 'SUCCESS',
          createdAt: '2026-10-31T12:00:00'
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false
    };

    service.getLogs({ action: 'CHECK_IN', page: 0, size: 20 }).subscribe(res => {
      expect(res.totalElements).toBe(1);
      expect(res.content[0].action).toBe('CHECK_IN');
    });

    const req = httpMock.expectOne(r => r.url === `${baseUrl}/logs` && r.params.get('action') === 'CHECK_IN');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should fetch distinct actions', () => {
    const mockActions = ['CHECK_IN', 'CHECK_OUT', 'CANCEL_RESERVATION'];

    service.getActions().subscribe(res => {
      expect(res).toEqual(mockActions);
    });

    const req = httpMock.expectOne(`${baseUrl}/actions`);
    expect(req.request.method).toBe('GET');
    req.flush(mockActions);
  });

  it('should fetch audit metrics', () => {
    const mockMetrics: AuditMetrics = {
      totalEvents: 100,
      todayEvents: 15,
      failedEvents: 2,
      distinctOperators: 4
    };

    service.getMetrics().subscribe(res => {
      expect(res.totalEvents).toBe(100);
      expect(res.todayEvents).toBe(15);
    });

    const req = httpMock.expectOne(`${baseUrl}/metrics`);
    expect(req.request.method).toBe('GET');
    req.flush(mockMetrics);
  });

  it('should download reservations CSV as blob', () => {
    const mockBlob = new Blob(['csv content'], { type: 'text/csv' });

    service.exportReservationsCsv('2026-10-01', '2026-10-31').subscribe(res => {
      expect(res).toBeTruthy();
    });

    const req = httpMock.expectOne(r => r.url === `${baseUrl}/export/reservations` && r.params.get('startDate') === '2026-10-01');
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(mockBlob);
  });

  it('should download financial journal CSV as blob', () => {
    const mockBlob = new Blob(['financial csv'], { type: 'text/csv' });

    service.exportFinancialJournalCsv().subscribe(res => {
      expect(res).toBeTruthy();
    });

    const req = httpMock.expectOne(`${baseUrl}/export/financial-journal`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(mockBlob);
  });
});
