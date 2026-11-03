import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ConciergeChatService } from './concierge-chat.service';
import { SseService } from './sse.service';
import { AuthService } from './auth.service';
import { Subject } from 'rxjs';
import { NotificationEvent } from '../models/notification.model';
import { environment } from '../../../environments/environment';
import {
  ConciergeConversationSummary,
  ConciergeMessageResponse,
  ConciergeStatusResponse
} from '../models/concierge.model';

describe('ConciergeChatService', () => {
  let service: ConciergeChatService;
  let httpTesting: HttpTestingController;
  let mockEventSubject: Subject<NotificationEvent>;

  const mockAuthService = {
    currentUser: () => ({ id: 10, username: 'juan.perez', role: 'ROLE_CUSTOMER' })
  };

  beforeEach(() => {
    mockEventSubject = new Subject<NotificationEvent>();

    const mockSseService = {
      events$: mockEventSubject.asObservable()
    };

    TestBed.configureTestingModule({
      providers: [
        ConciergeChatService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SseService, useValue: mockSseService },
        { provide: AuthService, useValue: mockAuthService }
      ]
    });

    service = TestBed.inject(ConciergeChatService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created with initial empty state', () => {
    expect(service).toBeTruthy();
    expect(service.messages()).toEqual([]);
    expect(service.unreadCount()).toBe(0);
    expect(service.isWidgetOpen()).toBe(false);
    expect(service.hasActiveStay()).toBe(false);
  });

  it('should load concierge status via GET /api/concierge/status', () => {
    const statusRes: ConciergeStatusResponse = {
      activeStay: true,
      suiteNumber: '101',
      reservationId: 50,
      guestName: 'Juan Perez'
    };

    service.loadStatus().subscribe((res) => {
      expect(res.activeStay).toBe(true);
      expect(res.suiteNumber).toBe('101');
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/concierge/status`);
    expect(req.request.method).toBe('GET');
    req.flush(statusRes);

    const countReq = httpTesting.expectOne(`${environment.apiUrl}/concierge/unread-count`);
    expect(countReq.request.method).toBe('GET');
    countReq.flush({ unreadCount: 1 });

    expect(service.conciergeStatus()).toEqual(statusRes);
    expect(service.hasActiveStay()).toBe(true);
  });

  it('should load my conversation via GET /api/concierge/my-messages', () => {
    const mockMessages: ConciergeMessageResponse[] = [
      {
        id: 1,
        senderId: 10,
        senderName: 'Juan Perez',
        senderRole: 'ROLE_CUSTOMER',
        recipientId: 2,
        reservationId: 5,
        suiteNumber: '101',
        message: 'Hola, solicito toallas extras',
        readStatus: true,
        createdAt: '2026-11-03T10:00:00'
      }
    ];

    service.loadMyConversation();

    const req = httpTesting.expectOne(`${environment.apiUrl}/concierge/my-messages`);
    expect(req.request.method).toBe('GET');
    req.flush(mockMessages);

    expect(service.messages().length).toBe(1);
    expect(service.messages()[0].message).toBe('Hola, solicito toallas extras');
  });

  it('should send a message via POST /api/concierge/messages', () => {
    const newMsg: ConciergeMessageResponse = {
      id: 2,
      senderId: 10,
      senderName: 'Juan Perez',
      senderRole: 'ROLE_CUSTOMER',
      recipientId: 2,
      reservationId: 5,
      suiteNumber: '101',
      message: 'Gracias por la atencion',
      readStatus: false,
      createdAt: '2026-11-03T10:05:00'
    };

    service.sendMessage({
      message: 'Gracias por la atencion',
      reservationId: 5,
      suiteNumber: '101'
    }).subscribe(res => {
      expect(res.id).toBe(2);
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/concierge/messages`);
    expect(req.request.method).toBe('POST');
    req.flush(newMsg);

    expect(service.messages().length).toBe(1);
    expect(service.messages()[0].id).toBe(2);
  });

  it('should claim conversation via PATCH /api/concierge/conversations/:guestId/claim', () => {
    service.activeConversations.set([
      {
        guestUserId: 10,
        guestName: 'Juan Perez',
        guestEmail: 'juan@example.com',
        suiteNumber: '101',
        lastMessage: 'Hola',
        lastMessageAt: '2026-11-03T10:00:00',
        unreadCount: 1,
        claimedBy: null,
        reservationId: 50
      }
    ]);

    const updatedSummary: ConciergeConversationSummary = {
      guestUserId: 10,
      guestName: 'Juan Perez',
      guestEmail: 'juan@example.com',
      suiteNumber: '101',
      lastMessage: 'Hola',
      lastMessageAt: '2026-11-03T10:00:00',
      unreadCount: 1,
      claimedBy: 'lucia.reception',
      reservationId: 50
    };

    service.claimConversation(10).subscribe(res => {
      expect(res.claimedBy).toBe('lucia.reception');
    });

    const req = httpTesting.expectOne(`${environment.apiUrl}/concierge/conversations/10/claim`);
    expect(req.request.method).toBe('PATCH');
    req.flush(updatedSummary);

    expect(service.activeConversations()[0].claimedBy).toBe('lucia.reception');
  });

  it('should mark conversation as read via PATCH /api/concierge/conversations/:guestId/read', () => {
    service.markConversationAsRead(10).subscribe();

    const req = httpTesting.expectOne(`${environment.apiUrl}/concierge/conversations/10/read`);
    expect(req.request.method).toBe('PATCH');
    req.flush(null);

    const countReq = httpTesting.expectOne(`${environment.apiUrl}/concierge/unread-count`);
    expect(countReq.request.method).toBe('GET');
    countReq.flush({ unreadCount: 0 });

    expect(service.unreadCount()).toBe(0);
  });

  it('should toggle widget open/close and load unread count when opening', () => {
    expect(service.isWidgetOpen()).toBe(false);

    service.toggleWidget();
    expect(service.isWidgetOpen()).toBe(true);

    const msgReq = httpTesting.expectOne(`${environment.apiUrl}/concierge/my-messages`);
    msgReq.flush([]);

    const countReq = httpTesting.expectOne(`${environment.apiUrl}/concierge/unread-count`);
    countReq.flush({ unreadCount: 0 });

    service.toggleWidget();
    expect(service.isWidgetOpen()).toBe(false);
  });

  it('should receive incoming CONCIERGE_MESSAGE from SSE and append it', () => {
    const incoming: ConciergeMessageResponse = {
      id: 99,
      senderId: 2,
      senderName: 'Recepcionista Maria',
      senderRole: 'ROLE_RECEPTIONIST',
      recipientId: 10,
      reservationId: 5,
      suiteNumber: '101',
      message: 'Enseguida le enviamos las toallas a la Suite 101.',
      readStatus: false,
      createdAt: '2026-11-03T10:08:00'
    };

    mockEventSubject.next({
      eventType: 'CONCIERGE_MESSAGE',
      targetChannel: 'GUEST',
      entityId: 99,
      title: 'Respuesta de Concierge',
      message: 'Enseguida le enviamos las toallas a la Suite 101.',
      payload: incoming,
      timestamp: '2026-11-03T10:08:00'
    });

    expect(service.messages().length).toBe(1);
    expect(service.messages()[0].id).toBe(99);
    expect(service.unreadCount()).toBe(1);
  });

  it('should receive incoming CONCIERGE_CLAIM from SSE and update claimedBy on matching conversation', () => {
    service.activeConversations.set([
      {
        guestUserId: 10,
        guestName: 'Juan Perez',
        guestEmail: 'juan@example.com',
        suiteNumber: '101',
        lastMessage: 'Hola',
        lastMessageAt: '2026-11-03T10:00:00',
        unreadCount: 1,
        claimedBy: null,
        reservationId: 50
      }
    ]);

    mockEventSubject.next({
      eventType: 'CONCIERGE_CLAIM',
      targetChannel: 'FRONT_DESK',
      entityId: 10,
      title: 'Conversación asignada',
      message: 'Carlos está atendiendo',
      payload: { guestUserId: 10, claimedBy: 'Carlos' },
      timestamp: '2026-11-03T10:10:00'
    });

    expect(service.activeConversations()[0].claimedBy).toBe('Carlos');
  });
});
