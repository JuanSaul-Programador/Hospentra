import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ConciergeConversationSummary,
  ConciergeMessageRequest,
  ConciergeMessageResponse,
  ConciergeStatusResponse
} from '../models/concierge.model';
import { SseService } from './sse.service';
import { AuthService } from './auth.service';
import { NotificationEvent } from '../models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class ConciergeChatService {
  private readonly http = inject(HttpClient);
  private readonly sseService = inject(SseService);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = `${environment.apiUrl}/concierge`;

  readonly messages = signal<ConciergeMessageResponse[]>([]);
  readonly activeConversations = signal<ConciergeConversationSummary[]>([]);
  readonly unreadCount = signal<number>(0);
  readonly selectedGuestId = signal<number | null>(null);
  readonly isWidgetOpen = signal<boolean>(false);
  readonly isSending = signal<boolean>(false);
  readonly isLoading = signal<boolean>(false);
  readonly isClaiming = signal<boolean>(false);
  readonly conciergeStatus = signal<ConciergeStatusResponse | null>(null);
  readonly hasActiveStay = computed(() => this.conciergeStatus()?.activeStay ?? false);

  constructor() {
    this.initSseListener();
  }

  loadStatus(): Observable<ConciergeStatusResponse> {
    return this.http.get<ConciergeStatusResponse>(`${this.apiUrl}/status`).pipe(
      tap({
        next: (res) => {
          this.conciergeStatus.set(res);
          if (res?.activeStay) {
            this.loadUnreadCount();
          }
        },
        error: () => {
          this.conciergeStatus.set(null);
        }
      })
    );
  }

  loadMyConversation(): void {
    this.isLoading.set(true);
    this.http.get<ConciergeMessageResponse[]>(`${this.apiUrl}/my-messages`).subscribe({
      next: (data) => {
        this.messages.set(data || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  loadActiveConversations(): void {
    this.isLoading.set(true);
    this.http.get<ConciergeConversationSummary[]>(`${this.apiUrl}/conversations`).subscribe({
      next: (data) => {
        this.activeConversations.set(data || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  loadConversationWithGuest(guestUserId: number): void {
    this.selectedGuestId.set(guestUserId);
    this.isLoading.set(true);
    this.http.get<ConciergeMessageResponse[]>(`${this.apiUrl}/conversations/${guestUserId}`).subscribe({
      next: (data) => {
        this.messages.set(data || []);
        this.isLoading.set(false);
        this.markConversationAsRead(guestUserId).subscribe();
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  sendMessage(req: ConciergeMessageRequest): Observable<ConciergeMessageResponse> {
    this.isSending.set(true);
    return this.http.post<ConciergeMessageResponse>(`${this.apiUrl}/messages`, req).pipe(
      tap((newMsg) => {
        this.isSending.set(false);
        this.messages.update(list => [...list, newMsg]);
        if (this.isStaff()) {
          this.loadActiveConversations();
        }
      })
    );
  }

  claimConversation(guestUserId: number): Observable<ConciergeConversationSummary> {
    this.isClaiming.set(true);
    return this.http.patch<ConciergeConversationSummary>(`${this.apiUrl}/conversations/${guestUserId}/claim`, {}).pipe(
      tap({
        next: (summary) => {
          this.isClaiming.set(false);
          this.activeConversations.update(list =>
            list.map(c => c.guestUserId === guestUserId ? { ...c, claimedBy: summary.claimedBy } : c)
          );
        },
        error: () => {
          this.isClaiming.set(false);
        }
      })
    );
  }

  markConversationAsRead(guestUserId: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/conversations/${guestUserId}/read`, {}).pipe(
      tap(() => {
        this.activeConversations.update(list =>
          list.map(c => c.guestUserId === guestUserId ? { ...c, unreadCount: 0 } : c)
        );
        this.loadUnreadCount();
      })
    );
  }

  markMyConversationAsRead(): Observable<void> {
    return this.markConversationAsRead(0);
  }

  loadUnreadCount(): void {
    this.http.get<{ unreadCount: number }>(`${this.apiUrl}/unread-count`).subscribe({
      next: (res) => {
        this.unreadCount.set(res?.unreadCount || 0);
      },
      error: () => {}
    });
  }

  toggleWidget(): void {
    const nextState = !this.isWidgetOpen();
    this.isWidgetOpen.set(nextState);
    if (nextState) {
      this.loadMyConversation();
      this.loadUnreadCount();
    }
  }

  openWidget(): void {
    this.isWidgetOpen.set(true);
    this.loadMyConversation();
    this.loadUnreadCount();
  }

  closeWidget(): void {
    this.isWidgetOpen.set(false);
  }

  private isStaff(): boolean {
    const role = this.authService.currentUser()?.role;
    return role === 'ROLE_ADMIN' || role === 'ROLE_RECEPTIONIST';
  }

  private initSseListener(): void {
    this.sseService.events$.subscribe((event: NotificationEvent) => {
      if (!event) return;

      if (event.eventType === 'CONCIERGE_CLAIM') {
        const payload = event.payload as { guestUserId?: number; claimedBy?: string };
        if (payload?.guestUserId) {
          this.activeConversations.update(list =>
            list.map(c => c.guestUserId === payload.guestUserId ? { ...c, claimedBy: payload.claimedBy ?? null } : c)
          );
        }
        return;
      }

      if (event.eventType !== 'CONCIERGE_MESSAGE') return;

      const payload = event.payload as ConciergeMessageResponse;
      if (!payload || !payload.id) return;

      if (!this.isStaff()) {
        const exists = this.messages().some(m => m.id === payload.id);
        if (!exists) {
          this.messages.update(list => [...list, payload]);
          if (!this.isWidgetOpen()) {
            this.unreadCount.update(c => c + 1);
          } else if (payload.senderRole !== 'ROLE_CUSTOMER') {
            this.markMyConversationAsRead().subscribe();
          }
        }
      } else {
        const currentSelected = this.selectedGuestId();
        if (currentSelected && (payload.senderId === currentSelected || payload.recipientId === currentSelected)) {
          const exists = this.messages().some(m => m.id === payload.id);
          if (!exists) {
            this.messages.update(list => [...list, payload]);
            this.markConversationAsRead(currentSelected).subscribe();
          }
        }
        this.loadActiveConversations();
        this.loadUnreadCount();
      }
    });
  }
}
