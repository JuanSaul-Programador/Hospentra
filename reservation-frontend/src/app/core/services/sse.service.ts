import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { Observable, Subject, filter } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenService } from './token.service';
import { NotificationEvent } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class SseService implements OnDestroy {
  private readonly tokenService = inject(TokenService);
  private abortController: AbortController | null = null;
  private reconnectTimeout: any = null;
  private isConnecting = false;

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
    if (this.isConnected() || this.isConnecting) return;

    const token = this.tokenService.getToken();
    if (!token) return;

    this.disconnect();
    this.isConnecting = true;
    this.abortController = new AbortController();

    this.startStreaming(token, this.abortController.signal);
  }

  private async startStreaming(token: string, signal: AbortSignal): Promise<void> {
    try {
      const response = await fetch(`${environment.apiUrl}/notifications/stream`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Accept': 'text/event-stream'
        },
        signal
      });

      if (!response.ok || !response.body) {
        throw new Error(`SSE stream failed with status ${response.status}`);
      }

      this.isConnecting = false;
      this.isConnected.set(true);

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';

      while (!signal.aborted) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const parts = buffer.split('\n\n');
        buffer = parts.pop() ?? '';

        for (const part of parts) {
          this.processSseMessage(part);
        }
      }
    } catch {
      if (!signal.aborted) {
        this.isConnected.set(false);
        this.isConnecting = false;
        this.scheduleReconnect();
      }
    } finally {
      if (signal.aborted) {
        this.isConnected.set(false);
        this.isConnecting = false;
      }
    }
  }

  private processSseMessage(raw: string): void {
    if (!raw.trim()) return;

    const lines = raw.split('\n');
    let eventType = '';
    let data = '';

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventType = line.replace('event:', '').trim();
      } else if (line.startsWith('data:')) {
        data = line.replace('data:', '').trim();
      }
    }

    if (!data) return;

    try {
      const parsed: NotificationEvent = JSON.parse(data);
      if (parsed.eventType !== 'PING') {
        this.eventSubject.next(parsed);
      }
    } catch {
      if (eventType && eventType !== 'PING') {
        this.eventSubject.next({
          eventType: eventType as any,
          targetChannel: 'ALL',
          entityId: null,
          title: 'Notification',
          message: data,
          payload: null,
          timestamp: new Date().toISOString()
        });
      }
    }
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
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
    this.isConnecting = false;
    this.isConnected.set(false);
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.eventSubject.complete();
  }
}
