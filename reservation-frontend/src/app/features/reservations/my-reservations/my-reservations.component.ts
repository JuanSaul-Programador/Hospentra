import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { ReservationService } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';
import { RoomOrderService } from '../../../core/services/room-order.service';
import { Reservation } from '../../../core/models/reservation.model';
import { ExtraCategory, ExtraService, RoomOrder } from '../../../core/models/room-order.model';

export interface RefundCalculation {
  hoursUntilCheckIn: number;
  isEligibleForFullRefund: boolean;
  refundAmount: number;
  penaltyAmount: number;
  policyMessage: string;
  badgeLabel: string;
  badgeType: 'full' | 'partial' | 'free';
}

@Component({
  selector: 'app-my-reservations',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './my-reservations.component.html',
  styleUrl: './my-reservations.component.scss'
})
export class MyReservationsComponent implements OnInit {
  private readonly reservationService = inject(ReservationService);
  private readonly roomOrderService = inject(RoomOrderService);
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly activeTab = signal<'upcoming' | 'history' | 'canceled'>('upcoming');
  readonly reservations = signal<Reservation[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);

  readonly selectedVoucherReservation = signal<Reservation | null>(null);
  readonly isVoucherModalOpen = signal<boolean>(false);

  readonly reservationToCancel = signal<Reservation | null>(null);
  readonly isCancelModalOpen = signal<boolean>(false);
  readonly isCancelling = signal<boolean>(false);

  readonly isRoomServiceModalOpen = signal<boolean>(false);
  readonly activeCheckedInReservation = signal<Reservation | null>(null);
  readonly extraServices = signal<ExtraService[]>([]);
  readonly selectedCategory = signal<ExtraCategory | 'ALL'>('ALL');
  readonly cartQuantities = signal<Record<number, number>>({});
  readonly orderNotes = signal<string>('');
  readonly isSubmittingOrder = signal<boolean>(false);
  readonly activeReservationOrders = signal<RoomOrder[]>([]);
  readonly orderSuccessMessage = signal<string | null>(null);

  readonly activeCancelCalculation = computed<RefundCalculation | null>(() => {
    const res = this.reservationToCancel();
    return res ? this.calculateRefund(res) : null;
  });

  readonly cancelResult = signal<RefundCalculation | null>(null);
  readonly isCancelResultModalOpen = signal<boolean>(false);

  readonly isConciergeModalOpen = signal<boolean>(false);
  readonly copiedCodeId = signal<number | null>(null);

  private readonly roomImages: Record<number, string> = {
    1: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
    2: 'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=1200&q=80',
    3: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1200&q=80',
    4: 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80',
    5: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
    6: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80'
  };

  readonly isStaffUser = computed(() => {
    const role = this.authService.userRole();
    return role === 'ROLE_ADMIN' || role === 'ROLE_RECEPTIONIST' || role === 'ROLE_KITCHEN';
  });

  readonly upcomingReservations = computed(() => {
    return this.reservations().filter(r => r.status === 'PENDING' || r.status === 'CONFIRMED' || r.status === 'CHECKED_IN');
  });

  readonly historyReservations = computed(() => {
    return this.reservations().filter(r => r.status === 'COMPLETED');
  });

  readonly canceledReservations = computed(() => {
    return this.reservations().filter(r => r.status === 'CANCELLED');
  });

  readonly filteredExtraServices = computed(() => {
    const cat = this.selectedCategory();
    const list = this.extraServices();
    if (cat === 'ALL') return list;
    return list.filter(item => item.category === cat);
  });

  readonly cartTotalCount = computed(() => {
    const cart = this.cartQuantities();
    return Object.values(cart).reduce((sum, q) => sum + q, 0);
  });

  readonly cartTotalAmount = computed(() => {
    const cart = this.cartQuantities();
    const services = this.extraServices();
    let total = 0;
    for (const [idStr, qty] of Object.entries(cart)) {
      if (qty > 0) {
        const item = services.find(s => s.id === Number(idStr));
        if (item) {
          total += item.price * qty;
        }
      }
    }
    return total;
  });

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.reservationService.getMyReservations().subscribe({
      next: (data) => {
        this.reservations.set(data || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.reservations.set([]);
        this.isLoading.set(false);
      }
    });
  }

  switchTab(tab: 'upcoming' | 'history' | 'canceled'): void {
    this.activeTab.set(tab);
  }

  getRoomImage(room: any): string {
    if (!room) return this.roomImages[1];
    return this.roomImages[room.id] ||
      this.roomImages[((room.id - 1) % 6) + 1] ||
      this.roomImages[1];
  }

  calculateNights(checkIn: string, checkOut: string): number {
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    if (isNaN(start.getTime()) || isNaN(end.getTime())) return 1;
    const diffDays = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 1;
  }

  openVoucher(res: Reservation): void {
    this.selectedVoucherReservation.set(res);
    this.isVoucherModalOpen.set(true);
  }

  closeVoucher(): void {
    this.isVoucherModalOpen.set(false);
    this.selectedVoucherReservation.set(null);
  }

  copyBookingCode(code: string, id: number): void {
    navigator.clipboard.writeText(code).then(() => {
      this.copiedCodeId.set(id);
      setTimeout(() => {
        if (this.copiedCodeId() === id) {
          this.copiedCodeId.set(null);
        }
      }, 2000);
    });
  }

  openRoomServiceModal(res: Reservation): void {
    this.activeCheckedInReservation.set(res);
    this.cartQuantities.set({});
    this.orderNotes.set('');
    this.selectedCategory.set('ALL');
    this.orderSuccessMessage.set(null);
    this.isRoomServiceModalOpen.set(true);

    this.roomOrderService.getExtraServices().subscribe({
      next: (data) => {
        this.extraServices.set(data || []);
      },
      error: () => {}
    });

    this.roomOrderService.getOrdersByReservation(res.id).subscribe({
      next: (orders) => {
        this.activeReservationOrders.set(orders || []);
      },
      error: () => {}
    });
  }

  closeRoomServiceModal(): void {
    this.isRoomServiceModalOpen.set(false);
    this.activeCheckedInReservation.set(null);
    this.cartQuantities.set({});
  }

  increaseCart(serviceId: number): void {
    this.cartQuantities.update(cart => ({
      ...cart,
      [serviceId]: (cart[serviceId] || 0) + 1
    }));
  }

  decreaseCart(serviceId: number): void {
    this.cartQuantities.update(cart => {
      const current = cart[serviceId] || 0;
      if (current <= 1) {
        const copy = { ...cart };
        delete copy[serviceId];
        return copy;
      }
      return {
        ...cart,
        [serviceId]: current - 1
      };
    });
  }

  submitRoomOrder(): void {
    const res = this.activeCheckedInReservation();
    if (!res) return;

    const cart = this.cartQuantities();
    const items = Object.entries(cart)
      .filter(([_, qty]) => qty > 0)
      .map(([idStr, qty]) => ({
        extraServiceId: Number(idStr),
        quantity: qty
      }));

    if (items.length === 0) return;

    this.isSubmittingOrder.set(true);
    this.roomOrderService.createOrder({
      reservationId: res.id,
      notes: this.orderNotes().trim(),
      items
    }).subscribe({
      next: (createdOrder) => {
        this.isSubmittingOrder.set(false);
        this.cartQuantities.set({});
        this.orderNotes.set('');
        this.activeReservationOrders.update(orders => [createdOrder, ...orders]);
        this.orderSuccessMessage.set(`¡Pedido #${createdOrder.id} enviado a Cocina & Bar! Se preparará de inmediato para su Suite.`);
        setTimeout(() => {
          this.orderSuccessMessage.set(null);
        }, 5000);
      },
      error: () => {
        this.isSubmittingOrder.set(false);
      }
    });
  }

  calculateRefund(res: Reservation): RefundCalculation {
    const isOnlineCard = res.paymentMethod === 'CREDIT_CARD';
    const total = Number(res.totalPrice) || 0;

    const checkInDateTime = new Date(`${res.checkInDate}T15:00:00`);
    const now = new Date();
    const diffMs = checkInDateTime.getTime() - now.getTime();
    const hoursRemaining = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60)));

    if (!isOnlineCard) {
      return {
        hoursUntilCheckIn: hoursRemaining,
        isEligibleForFullRefund: true,
        refundAmount: 0,
        penaltyAmount: 0,
        policyMessage: 'Cancelación flexible sin cargos: Su modalidad de pago es en recepción, por lo que no se efectuó ningún débito previo.',
        badgeLabel: 'Cancelación Gratuita (Sin Cargos)',
        badgeType: 'free'
      };
    }

    if (hoursRemaining >= 48) {
      return {
        hoursUntilCheckIn: hoursRemaining,
        isEligibleForFullRefund: true,
        refundAmount: total,
        penaltyAmount: 0,
        policyMessage: `Reembolso íntegro del 100% procesado a su tarjeta •••• ${res.cardLast4 || '4242'}. Al cancelar con más de 48h de anticipación, recupera el monto total de su estancia.`,
        badgeLabel: 'Reembolso del 100% Garantizado',
        badgeType: 'full'
      };
    } else {
      const nights = this.calculateNights(res.checkInDate, res.checkOutDate);
      const pricePerNight = nights > 0 ? total / nights : total;
      const penalty = Math.min(total, Math.round(pricePerNight * 100) / 100);
      const refund = Math.max(0, Math.round((total - penalty) * 100) / 100);

      return {
        hoursUntilCheckIn: hoursRemaining,
        isEligibleForFullRefund: false,
        refundAmount: refund,
        penaltyAmount: penalty,
        policyMessage: `Cancelación tardía (quedan ${hoursRemaining}h para el check-in): Por política boutique se retiene el valor de la 1ª noche ($${penalty.toFixed(2)} USD) y se reembolsa el saldo restante ($${refund.toFixed(2)} USD) a su tarjeta •••• ${res.cardLast4 || '4242'}.`,
        badgeLabel: 'Retención de 1ª Noche (Saldo Reembolsado)',
        badgeType: 'partial'
      };
    }
  }

  openCancelDialog(res: Reservation): void {
    this.reservationToCancel.set(res);
    this.isCancelModalOpen.set(true);
  }

  closeCancelDialog(): void {
    this.isCancelModalOpen.set(false);
    this.reservationToCancel.set(null);
  }

  confirmCancel(): void {
    const res = this.reservationToCancel();
    if (!res) return;

    const calc = this.calculateRefund(res);
    this.isCancelling.set(true);

    this.reservationService.cancelReservation(res.id).subscribe({
      next: (updatedRes) => {
        this.isCancelling.set(false);
        this.reservations.update(list =>
          list.map(item => item.id === res.id ? { ...item, status: 'CANCELLED' } : item)
        );
        this.closeCancelDialog();
        this.showCancelResult(calc);
      },
      error: () => {
        this.isCancelling.set(false);
        this.reservations.update(list =>
          list.map(item => item.id === res.id ? { ...item, status: 'CANCELLED' } : item)
        );
        this.closeCancelDialog();
        this.showCancelResult(calc);
      }
    });
  }

  showCancelResult(calc: RefundCalculation): void {
    this.cancelResult.set(calc);
    this.isCancelResultModalOpen.set(true);
  }

  closeCancelResultModal(): void {
    this.isCancelResultModalOpen.set(false);
    this.cancelResult.set(null);
    this.activeTab.set('canceled');
  }

  openConcierge(): void {
    this.isConciergeModalOpen.set(true);
  }

  closeConcierge(): void {
    this.isConciergeModalOpen.set(false);
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'CHECKED_IN':
        return 'En Casa (Check-In Realizado)';
      case 'CONFIRMED':
        return 'Reserva Confirmada';
      case 'PENDING':
        return 'Pendiente de Confirmación';
      case 'COMPLETED':
        return 'Estancia Completada';
      case 'CANCELLED':
        return 'Reserva Cancelada';
      default:
        return status;
    }
  }

  printHistory(): void {
    window.print();
  }

  printVoucher(): void {
    window.print();
  }
}
