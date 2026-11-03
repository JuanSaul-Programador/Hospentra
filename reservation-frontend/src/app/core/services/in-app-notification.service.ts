import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { InAppNotification, NotificationEvent } from '../models/notification.model';
import { SseService } from './sse.service';

@Injectable({
  providedIn: 'root'
})
export class InAppNotificationService {
  private readonly http = inject(HttpClient);
  private readonly sseService = inject(SseService);
  private readonly apiUrl = `${environment.apiUrl}/notifications`;

  readonly notifications = signal<InAppNotification[]>([]);
  readonly unreadCount = signal<number>(0);
  readonly activeToast = signal<InAppNotification | null>(null);
  readonly isDropdownOpen = signal<boolean>(false);
  private toastTimer: any = null;

  constructor() {
    this.initSseListener();
  }

  loadNotifications(limit = 20): void {
    this.http.get<InAppNotification[]>(`${this.apiUrl}?limit=${limit}`).subscribe({
      next: (data) => {
        this.notifications.set(data || []);
      },
      error: () => {}
    });
  }

  loadUnreadCount(): void {
    this.http.get<{ unreadCount: number }>(`${this.apiUrl}/unread-count`).subscribe({
      next: (res) => {
        this.unreadCount.set(res?.unreadCount || 0);
      },
      error: () => {}
    });
  }

  markAsRead(id: number): Observable<InAppNotification> {
    return this.http.patch<InAppNotification>(`${this.apiUrl}/${id}/read`, {}).pipe(
      tap((updated) => {
        this.notifications.update(list => list.map(n => n.id === id ? { ...n, read: true } : n));
        this.unreadCount.update(c => Math.max(0, c - 1));
      })
    );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/read-all`, {}).pipe(
      tap(() => {
        this.notifications.update(list => list.map(n => ({ ...n, read: true })));
        this.unreadCount.set(0);
      })
    );
  }

  toggleDropdown(): void {
    const nextState = !this.isDropdownOpen();
    this.isDropdownOpen.set(nextState);
    if (nextState) {
      this.loadNotifications();
      this.loadUnreadCount();
    }
  }

  closeDropdown(): void {
    this.isDropdownOpen.set(false);
  }

  private initSseListener(): void {
    this.sseService.events$.subscribe((event: NotificationEvent) => {
      if (!event || event.eventType === 'PING' || event.eventType === 'HANDSHAKE') return;

      if (event.payload && typeof event.payload === 'object' && 'title' in event.payload && 'message' in event.payload) {
        const notif: InAppNotification = {
          id: (event.payload as any).id || Date.now(),
          title: event.title || (event.payload as any).title,
          message: event.message || (event.payload as any).message,
          type: event.eventType,
          read: false,
          link: (event.payload as any).link || '/my-reservations',
          createdAt: new Date().toISOString()
        };

        this.notifications.update(current => [notif, ...current]);
        this.unreadCount.update(c => c + 1);
        this.showToast(notif);
      }
    });
  }

  showToast(notif: InAppNotification): void {
    this.activeToast.set(notif);
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.activeToast.set(null);
    }, 5000);
  }

  dismissToast(): void {
    this.activeToast.set(null);
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }
}
