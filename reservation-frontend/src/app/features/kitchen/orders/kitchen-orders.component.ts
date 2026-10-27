import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { RoomOrderService } from '../../../core/services/room-order.service';
import { AuthService } from '../../../core/services/auth.service';
import { SseService } from '../../../core/services/sse.service';
import { OrderStatus, RoomOrder } from '../../../core/models/room-order.model';

@Component({
  selector: 'app-kitchen-orders',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './kitchen-orders.component.html',
  styleUrl: './kitchen-orders.component.scss'
})
export class KitchenOrdersComponent implements OnInit, OnDestroy {
  private readonly roomOrderService = inject(RoomOrderService);
  readonly authService = inject(AuthService);
  readonly sseService = inject(SseService);
  private readonly router = inject(Router);

  readonly orders = signal<RoomOrder[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly isProcessingId = signal<number | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly soundEnabled = signal<boolean>(typeof window !== 'undefined' ? localStorage.getItem('hospentra_kds_sound') !== 'false' : true);
  readonly hasSoundChimed = signal<boolean>(false);

  private pollInterval: any = null;
  private sseSub: Subscription | null = null;
  private audioCtx: AudioContext | null = null;
  private previousReceivedIds = new Set<number>();
  private isFirstLoad = true;

  readonly receivedOrders = computed(() =>
    this.orders().filter(o => o.status === 'RECEIVED')
  );

  readonly preparingOrders = computed(() =>
    this.orders().filter(o => o.status === 'PREPARING')
  );

  readonly readyOrders = computed(() =>
    this.orders().filter(o => o.status === 'READY_FOR_DELIVERY')
  );

  readonly deliveredRecent = computed(() =>
    this.orders().filter(o => o.status === 'DELIVERED').slice(0, 5)
  );

  ngOnInit(): void {
    this.loadOrders();
    this.sseService.connect();

    this.sseSub = new Subscription();

    this.sseSub.add(
      this.sseService.orderCreated$.subscribe({
        next: (event) => {
          this.playChime();
          this.showToast(event.message || '¡Nueva comanda recibida en cocina!');
          this.loadOrders(false);
        }
      })
    );

    this.sseSub.add(
      this.sseService.orderStatusChanged$.subscribe({
        next: (event) => {
          const updatedOrder = event.payload as RoomOrder;
          if (updatedOrder && updatedOrder.id) {
            this.orders.update(list => list.map(o => o.id === updatedOrder.id ? updatedOrder : o));
          } else {
            this.loadOrders(false);
          }
        }
      })
    );

    this.pollInterval = setInterval(() => {
      this.loadOrders(false);
    }, 60000);
  }

  ngOnDestroy(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
    if (this.sseSub) {
      this.sseSub.unsubscribe();
    }
    if (this.audioCtx && this.audioCtx.state !== 'closed') {
      this.audioCtx.close().catch(() => {});
    }
  }

  toggleSound(): void {
    const nextState = !this.soundEnabled();
    this.soundEnabled.set(nextState);
    if (typeof window !== 'undefined') {
      localStorage.setItem('hospentra_kds_sound', String(nextState));
    }
    if (nextState) {
      this.playChime();
      this.showToast('Alertas sonoras activadas en terminal de cocina.');
    } else {
      this.showToast('Alertas sonoras silenciadas.');
    }
  }

  private initAudioContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    try {
      const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
      if (!this.audioCtx && AudioContextClass) {
        this.audioCtx = new AudioContextClass();
      }
      if (this.audioCtx && this.audioCtx.state === 'suspended') {
        this.audioCtx.resume().catch(() => {});
      }
      return this.audioCtx;
    } catch {
      return null;
    }
  }

  playChime(): void {
    const ctx = this.initAudioContext();
    if (!ctx) return;

    try {
      const now = ctx.currentTime;

      const osc1 = ctx.createOscillator();
      const gain1 = ctx.createGain();
      osc1.type = 'sine';
      osc1.frequency.setValueAtTime(587.33, now);
      gain1.gain.setValueAtTime(0, now);
      gain1.gain.linearRampToValueAtTime(0.28, now + 0.03);
      gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.45);
      osc1.connect(gain1);
      gain1.connect(ctx.destination);
      osc1.start(now);
      osc1.stop(now + 0.45);

      const osc2 = ctx.createOscillator();
      const gain2 = ctx.createGain();
      osc2.type = 'sine';
      osc2.frequency.setValueAtTime(880, now + 0.12);
      gain2.gain.setValueAtTime(0, now + 0.12);
      gain2.gain.linearRampToValueAtTime(0.35, now + 0.15);
      gain2.gain.exponentialRampToValueAtTime(0.0008, now + 0.85);
      osc2.connect(gain2);
      gain2.connect(ctx.destination);
      osc2.start(now + 0.12);
      osc2.stop(now + 0.85);

      this.hasSoundChimed.set(true);
      setTimeout(() => {
        this.hasSoundChimed.set(false);
      }, 1500);
    } catch {}
  }

  loadOrders(showSpinner: boolean = true): void {
    if (showSpinner) {
      this.isLoading.set(true);
    }
    this.errorMessage.set(null);

    this.roomOrderService.getActiveKitchenOrders().subscribe({
      next: (data) => {
        const orderList = data || [];
        this.orders.set(orderList);
        this.isLoading.set(false);

        const currentReceived = orderList.filter(o => o.status === 'RECEIVED');
        const currentReceivedIds = new Set(currentReceived.map(o => o.id));

        if (!this.isFirstLoad && this.soundEnabled()) {
          const hasNewIncoming = Array.from(currentReceivedIds).some(id => !this.previousReceivedIds.has(id));
          if (hasNewIncoming) {
            this.playChime();
            this.showToast('¡Nueva comanda recibida en cocina! Revisar columna de inicio.');
          }
        }

        this.previousReceivedIds = currentReceivedIds;
        this.isFirstLoad = false;
      },
      error: () => {
        this.errorMessage.set('Error al sincronizar comandas de cocina con el servidor.');
        this.isLoading.set(false);
      }
    });
  }

  updateStatus(orderId: number, newStatus: OrderStatus): void {
    this.isProcessingId.set(orderId);
    this.roomOrderService.updateOrderStatus(orderId, newStatus).subscribe({
      next: (updatedOrder) => {
        this.isProcessingId.set(null);
        this.orders.update(list => list.map(o => o.id === orderId ? updatedOrder : o));
        
        let label = 'Comanda actualizada.';
        if (newStatus === 'PREPARING') label = `Comanda #${orderId} de Suite ${updatedOrder.roomNumber} en preparación en fogón.`;
        if (newStatus === 'READY_FOR_DELIVERY') label = `Comanda #${orderId} lista para servir. Notificado a runner de piso.`;
        if (newStatus === 'DELIVERED') label = `Servicio entregado con éxito en Suite ${updatedOrder.roomNumber}.`;

        this.showToast(label);
      },
      error: () => {
        this.isProcessingId.set(null);
        this.errorMessage.set('No se pudo actualizar el estado de la comanda.');
      }
    });
  }

  getElapsedMinutes(orderedAt: string): number {
    const start = new Date(orderedAt).getTime();
    const now = new Date().getTime();
    if (isNaN(start)) return 0;
    return Math.max(0, Math.floor((now - start) / 60000));
  }

  showToast(message: string): void {
    this.successMessage.set(message);
    setTimeout(() => {
      if (this.successMessage() === message) {
        this.successMessage.set(null);
      }
    }, 3500);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/admin/login']);
  }
}
