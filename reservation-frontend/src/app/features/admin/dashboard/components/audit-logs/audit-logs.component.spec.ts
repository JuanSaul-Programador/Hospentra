import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuditLogsComponent } from './audit-logs.component';
import { AuditService } from '../../../../../core/services/audit.service';
import { AuditLogPageResponse, AuditMetrics } from '../../../../../core/models/audit.model';

describe('AuditLogsComponent', () => {
  let component: AuditLogsComponent;
  let fixture: ComponentFixture<AuditLogsComponent>;
  let mockAuditService: any;

  const mockLogsPage: AuditLogPageResponse = {
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
      },
      {
        id: 2,
        username: 'recep1',
        userRole: 'ROLE_RECEPTIONIST',
        action: 'CHECK_OUT',
        entityName: 'Reservation',
        entityId: 'HOSP-2-2026',
        details: 'Check-out suite 202',
        ipAddress: '192.168.1.15',
        status: 'SUCCESS',
        createdAt: '2026-10-31T12:30:00'
      }
    ],
    totalElements: 2,
    totalPages: 1,
    size: 15,
    number: 0,
    first: true,
    last: true,
    empty: false
  };

  const mockMetrics: AuditMetrics = {
    totalEvents: 45,
    todayEvents: 8,
    failedEvents: 1,
    distinctOperators: 3
  };

  beforeEach(async () => {
    mockAuditService = {
      getLogs: vi.fn().mockReturnValue(of(mockLogsPage)),
      getActions: vi.fn().mockReturnValue(of(['CHECK_IN', 'CHECK_OUT', 'CREATE_STAFF'])),
      getMetrics: vi.fn().mockReturnValue(of(mockMetrics)),
      exportReservationsCsv: vi.fn().mockReturnValue(of(new Blob(['res csv'], { type: 'text/csv' }))),
      exportFinancialJournalCsv: vi.fn().mockReturnValue(of(new Blob(['fin csv'], { type: 'text/csv' })))
    };

    await TestBed.configureTestingModule({
      imports: [AuditLogsComponent],
      providers: [
        { provide: AuditService, useValue: mockAuditService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load initial logs, metrics, and actions', () => {
    expect(component).toBeTruthy();
    expect(mockAuditService.getLogs).toHaveBeenCalled();
    expect(mockAuditService.getMetrics).toHaveBeenCalled();
    expect(mockAuditService.getActions).toHaveBeenCalled();
    expect(component.logs().length).toBe(2);
    expect(component.metrics()?.totalEvents).toBe(45);
    expect(component.availableActions()).toContain('CHECK_IN');
    expect(component.isLoading()).toBe(false);
  });

  it('should reset page and reload logs when filter changes', () => {
    component.currentPage.set(2);
    component.filterAction.set('CHECK_IN');
    component.onFilterChange();

    expect(component.currentPage()).toBe(0);
    expect(mockAuditService.getLogs).toHaveBeenCalledWith(expect.objectContaining({
      action: 'CHECK_IN',
      page: 0
    }));
  });

  it('should clear all filters', () => {
    component.filterAction.set('CHECK_IN');
    component.filterUsername.set('admin1');
    component.filterStartDate.set('2026-10-01');
    component.filterEndDate.set('2026-10-31');
    component.currentPage.set(1);

    component.clearFilters();

    expect(component.filterAction()).toBe('');
    expect(component.filterUsername()).toBe('');
    expect(component.filterStartDate()).toBe('');
    expect(component.filterEndDate()).toBe('');
    expect(component.currentPage()).toBe(0);
    expect(mockAuditService.getLogs).toHaveBeenCalled();
  });

  it('should navigate pages correctly', () => {
    component.totalPages.set(3);
    component.currentPage.set(0);

    component.nextPage();
    expect(component.currentPage()).toBe(1);

    component.prevPage();
    expect(component.currentPage()).toBe(0);
  });

  it('should toggle row expansion', () => {
    expect(component.expandedLogId()).toBeNull();
    component.toggleExpand(1);
    expect(component.expandedLogId()).toBe(1);
    component.toggleExpand(1);
    expect(component.expandedLogId()).toBeNull();
  });

  it('should export reservations and trigger toast message', () => {
    component.exportReservations();

    expect(mockAuditService.exportReservationsCsv).toHaveBeenCalled();
    expect(component.toastMessage()).toContain('Libro de reservas exportado');
    expect(component.isExporting()).toBe(false);
  });

  it('should export financial journal and trigger toast message', () => {
    component.exportFinancialJournal();

    expect(mockAuditService.exportFinancialJournalCsv).toHaveBeenCalled();
    expect(component.toastMessage()).toContain('Diario financiero exportado');
    expect(component.isExporting()).toBe(false);
  });

  it('should map action icons and labels correctly', () => {
    expect(component.getActionIcon('CHECK_IN')).toBe('luggage');
    expect(component.getActionIcon('CHECK_OUT')).toBe('key');
    expect(component.getActionIcon('ASSIGN_THERAPIST')).toBe('self_improvement');
    expect(component.getActionIcon('UNKNOWN_ACTION')).toBe('history_edu');

    expect(component.formatAction('CHECK_IN')).toBe('Check-In Huésped');
    expect(component.formatAction('CHECK_OUT')).toBe('Check-Out Suite');
    expect(component.formatAction('CREATE_STAFF')).toBe('Alta de Personal');
  });
});
