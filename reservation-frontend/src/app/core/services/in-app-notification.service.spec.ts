import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InAppNotificationService } from './in-app-notification.service';
import { SseService } from './sse.service';
import { Subject } from 'rxjs';
import { NotificationEvent } from '../models/notification.model';
import { environment } from '../../../environments/environment';

describe('InAppNotificationService', () => {
  let service: InAppNotificationService;
  let httpTesting: HttpTestingController;
  let mockEventSubject: Subject<NotificationEvent>;

  beforeEach(() => {
    mockEventSubject = new Subject<NotificationEvent>();

    const mockSseService = {
      events$: mockEventSubject.asObservable()
    };

    TestBed.configureTestingModule({
      providers: [
        InAppNotificationService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SseService, useValue: mockSseService }
      ]
    });

    service = TestBed.inject(InAppNotificationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created and have initial zero count', () => {
    expect(service).toBeTruthy();
    expect(service.unreadCount()).toBe(0);
    expect(service.notifications()).toEqual([]);
  });

  it('should load notifications via GET /api/notifications', () => {
    const mockData = [
      {
        id: 1,
        title: 'Fast-Pass Listo',
        message: 'Pre-Check-In confirmado',
        type: 'PRE_CHECK_IN',
        read: false,
        link: '/my-reservations',
        createdAt: '2026-11-03T10:00:00'
      }
    ];

    service.loadNotifications(20);

    const req = httpTesting.expectOne(`${environment.apiUrl}/notifications?limit=20`);
    expect(req.request.method).toBe('GET');
    req.flush(mockData);

    expect(service.notifications().length).toBe(1);
    expect(service.notifications()[0].title).toBe('Fast-Pass Listo');
  });

  it('should load unread count via GET /api/notifications/unread-count', () => {
    service.loadUnreadCount();

    const req = httpTesting.expectOne(`${environment.apiUrl}/notifications/unread-count`);
    expect(req.request.method).toBe('GET');
    req.flush({ unreadCount: 3 });

    expect(service.unreadCount()).toBe(3);
  });

  it('should mark single notification as read and decrement unreadCount', () => {
    service.notifications.set([
      { id: 5, title: 'Orden', message: 'Lista', type: 'ROOM_SERVICE', read: false, createdAt: '' }
    ]);
    service.unreadCount.set(2);

    service.markAsRead(5).subscribe();

    const req = httpTesting.expectOne(`${environment.apiUrl}/notifications/5/read`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ id: 5, title: 'Orden', message: 'Lista', type: 'ROOM_SERVICE', read: true, createdAt: '' });

    expect(service.notifications()[0].read).toBe(true);
    expect(service.unreadCount()).toBe(1);
  });

  it('should mark all notifications as read and reset unreadCount to 0', () => {
    service.notifications.set([
      { id: 1, title: 'A', message: 'A', type: 'GENERAL', read: false, createdAt: '' },
      { id: 2, title: 'B', message: 'B', type: 'GENERAL', read: false, createdAt: '' }
    ]);
    service.unreadCount.set(2);

    service.markAllAsRead().subscribe();

    const req = httpTesting.expectOne(`${environment.apiUrl}/notifications/read-all`);
    expect(req.request.method).toBe('PATCH');
    req.flush(null);

    expect(service.notifications().every(n => n.read)).toBe(true);
    expect(service.unreadCount()).toBe(0);
  });

  it('should toggle and close dropdown', () => {
    expect(service.isDropdownOpen()).toBe(false);

    service.toggleDropdown();
    expect(service.isDropdownOpen()).toBe(true);

    const reqNotifs = httpTesting.expectOne(`${environment.apiUrl}/notifications?limit=20`);
    reqNotifs.flush([]);
    const reqCount = httpTesting.expectOne(`${environment.apiUrl}/notifications/unread-count`);
    reqCount.flush({ unreadCount: 0 });

    service.closeDropdown();
    expect(service.isDropdownOpen()).toBe(false);
  });

  it('should show and dismiss toast', () => {
    const notif = { id: 9, title: 'Test', message: 'Msg', type: 'CHECK_IN', read: false, createdAt: '' };
    service.showToast(notif);
    expect(service.activeToast()?.title).toBe('Test');

    service.dismissToast();
    expect(service.activeToast()).toBeNull();
  });
});
