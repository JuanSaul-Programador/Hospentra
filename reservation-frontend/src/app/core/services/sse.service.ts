import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Observable, Subject, filter, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenService } from './token.service';
import { NotificationEvent, NotificationEventType } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class SseService implements OnDestroy {
  private readonly tokenService = inject(TokenService);
  private eventSource: EventSource | null = null;
  private reconnectTimeout: any = null;

  readonly isConnected = signal<boolean>(false);
  private readonly eventSubject = new Subject<NotificationEvent>();

  readonly events$: Observable<NotificationEvent> = this.eventSubject.asObservable();

  readonly orderCreated$: Observable<NotificationEvent> = this.events$.pipe(
    filter(e => e.eventType === 'ORDER_CREATED')
  );

  readonly orderStatusChanged$: Observable<NotificationEvent> = this.events$.pipe(
    filter(e => e.eventType === 'ORDER_STATUS_CHANGED' || e.eventType === 'ORDER_THERAPIST_ASSIGNED')
  );

  readonly roomStatusChanged$: Observable<NotificationEvent> = this.events$.pipe(
    filter(e => e.eventType === 'ROOM_STATUS_CHANGED')
  );

  readonly reservationStatusChanged$: Observable<NotificationEvent> = this.events$.pipe(
    filter(e => e.eventType === 'RESERVATION_CREATED' || e.eventType === 'RESERVATION_STATUS_CHANGED')
  );

  connect(): void {
    if (typeof window === 'undefined') return;
    if (this.eventSource && this.eventSource.readyState !== EventSource.CLOSED) {
      return;
    }

    const token = this.tokenService.getToken();
    if (!token) return;

    this.disconnect();

    const streamUrl = `${environment.apiUrl}/notifications/stream?token=${encodeURIComponent(token)}`;

    try {
      this.eventSource = new EventSource(streamUrl);

      this.eventSource.onopen = () => {
        this.isConnected.set(true);
      };

      this.eventSource.onerror = () => {
        this.isConnected.set(false);
        this.disconnect();
        this.scheduleReconnect();
      };

      const eventNames: NotificationEventType[] = [
        'ORDER_CREATED',
        'ORDER_STATUS_CHANGED',
        'ORDER_THERAPIST_ASSIGNED',
        'ROOM_STATUS_CHANGED',
        'RESERVATION_CREATED',
        'RESERVATION_STATUS_CHANGED',
        'HANDSHAKE',
        'PING'
      ];

      for (const evName of eventNames) {
        this.eventSource.addEventListener(evName, (e: MessageEvent) => {
          this.handleIncomingMessage(e);
        });
      }
    } catch {
      this.scheduleReconnect();
    }
  }

  private handleIncomingMessage(e: MessageEvent): void {
    if (!e.data) return;
    try {
      const parsed: NotificationEvent = JSON.parse(e.data);
      if (parsed.eventType !== 'PING') {
        this.eventSubject.next(parsed);
      }
    } catch {}
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
    this.reconnectTimeout = setTimeout(() => {
      if (this.tokenService.hasToken()) {
        this.connect();
      }
    }, 5000);
  }

  disconnect(): void {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    this.isConnected.set(false);
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.eventSubject.complete();
  }
}
