import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MaintenanceService } from './maintenance.service';
import { environment } from '../../../environments/environment';
import { CreateMaintenanceBlockRequest, MaintenanceBlock } from '../models/maintenance.model';

describe('MaintenanceService', () => {
  let service: MaintenanceService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/admin/maintenance-blocks`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MaintenanceService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(MaintenanceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should post createBlock', () => {
    const payload: CreateMaintenanceBlockRequest = {
      roomId: 10,
      blockType: 'OUT_OF_ORDER',
      startDate: '2026-11-01',
      endDate: '2026-11-04',
      reason: 'Reparación de grifería',
      contractor: 'Servicios Hidráulicos'
    };

    const mockResponse: MaintenanceBlock = {
      id: 1,
      roomId: 10,
      roomNumber: '101',
      roomTypeName: 'Presidential Suite',
      blockType: 'OUT_OF_ORDER',
      startDate: '2026-11-01',
      endDate: '2026-11-04',
      reason: 'Reparación de grifería',
      contractorName: 'Servicios Hidráulicos',
      status: 'ACTIVE',
      createdBy: 'admin',
      createdAt: '2026-10-31T12:00:00',
      durationDays: 4
    };

    service.createBlock(payload).subscribe(res => {
      expect(res.id).toBe(1);
      expect(res.roomNumber).toBe('101');
      expect(res.status).toBe('ACTIVE');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockResponse);
  });

  it('should fetch all blocks', () => {
    const mockList: MaintenanceBlock[] = [
      {
        id: 1,
        roomId: 10,
        roomNumber: '101',
        blockType: 'OUT_OF_ORDER',
        startDate: '2026-11-01',
        endDate: '2026-11-04',
        reason: 'Reparación',
        status: 'ACTIVE',
        createdAt: '2026-10-31T12:00:00',
        durationDays: 4
      }
    ];

    service.getBlocks().subscribe(list => {
      expect(list.length).toBe(1);
      expect(list[0].id).toBe(1);
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mockList);
  });

  it('should fetch active blocks with date filters', () => {
    service.getActiveBlocks('2026-11-01', '2026-11-14').subscribe(list => {
      expect(list).toEqual([]);
    });

    const req = httpMock.expectOne(r =>
      r.url === `${baseUrl}/active` &&
      r.params.get('startDate') === '2026-11-01' &&
      r.params.get('endDate') === '2026-11-14'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should resolve block', () => {
    service.resolveBlock(5).subscribe(res => {
      expect(res.status).toBe('RESOLVED');
    });

    const req = httpMock.expectOne(`${baseUrl}/5/resolve`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 5, status: 'RESOLVED' });
  });

  it('should cancel block', () => {
    service.cancelBlock(5).subscribe(res => {
      expect(res.status).toBe('CANCELLED');
    });

    const req = httpMock.expectOne(`${baseUrl}/5/cancel`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 5, status: 'CANCELLED' });
  });
});
