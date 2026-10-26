import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { RoomOrderService } from '../../../core/services/room-order.service';
import { AuthService } from '../../../core/services/auth.service';
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
  private readonly router = inject(Router);

  readonly orders = signal<RoomOrder[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly isProcessingId = signal<number | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  private pollInterval: any = null;

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
    this.pollInterval = setInterval(() => {
      this.loadOrders(false);
    }, 10000);
  }

  ngOnDestroy(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  }

  loadOrders(showSpinner: boolean = true): void {
    if (showSpinner) {
      this.isLoading.set(true);
    }
    this.errorMessage.set(null);

    this.roomOrderService.getActiveKitchenOrders().subscribe({
      next: (data) => {
        this.orders.set(data || []);
        this.isLoading.set(false);
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
