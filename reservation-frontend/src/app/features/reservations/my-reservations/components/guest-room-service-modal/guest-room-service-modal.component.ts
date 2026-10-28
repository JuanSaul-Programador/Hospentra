import { Component, EventEmitter, Input, Output, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Reservation } from '../../../../../core/models/reservation.model';
import { ExtraCategory, ExtraService, OrderStatus, RoomOrder } from '../../../../../core/models/room-order.model';
import { I18nService } from '../../../../../core/services/i18n.service';

export interface RoomOrderSubmitPayload {
  items: { extraServiceId: number; quantity: number }[];
  notes?: string;
  serviceLocation?: 'IN_SUITE' | 'SPA_CENTER';
  scheduledTime?: string;
}

@Component({
  selector: 'app-guest-room-service-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './guest-room-service-modal.component.html',
  styleUrl: './guest-room-service-modal.component.scss'
})
export class GuestRoomServiceModalComponent {
  readonly i18n = inject(I18nService);

  @Input() reservation: Reservation | null = null;
  @Input() isOpen: boolean = false;
  @Input() extraServices: ExtraService[] = [];
  @Input() activeReservationOrders: RoomOrder[] = [];
  @Input() isSubmitting: boolean = false;
  @Input() orderSuccessMessage: string | null = null;

  @Output() closeModal = new EventEmitter<void>();
  @Output() submitOrder = new EventEmitter<RoomOrderSubmitPayload>();

  readonly rsActiveView = signal<'MENU' | 'ORDERS'>('MENU');
  readonly selectedCategory = signal<ExtraCategory | 'ALL'>('ALL');
  readonly cartQuantities = signal<Record<number, number>>({});
  readonly orderNotes = signal<string>('');
  readonly spaLocation = signal<'IN_SUITE' | 'SPA_CENTER'>('IN_SUITE');
  readonly spaScheduledTime = signal<string>('17:00 hrs');
  readonly spaTimeSlots: string[] = ['10:00 hrs', '11:30 hrs', '13:00 hrs', '15:00 hrs', '16:30 hrs', '18:00 hrs', '19:30 hrs', '21:00 hrs'];

  readonly filteredExtraServices = computed(() => {
    const cat = this.selectedCategory();
    if (cat === 'ALL') return this.extraServices;
    return this.extraServices.filter(item => item.category === cat);
  });

  readonly cartTotalCount = computed(() => {
    const cart = this.cartQuantities();
    return Object.values(cart).reduce((sum, q) => sum + q, 0);
  });

  readonly cartTotalAmount = computed(() => {
    const cart = this.cartQuantities();
    let total = 0;
    for (const [idStr, qty] of Object.entries(cart)) {
      if (qty > 0) {
        const item = this.extraServices.find(s => s.id === Number(idStr));
        if (item) {
          total += item.price * qty;
        }
      }
    }
    return total;
  });

  readonly cartItems = computed(() => {
    const cart = this.cartQuantities();
    return Object.entries(cart)
      .filter(([_, qty]) => qty > 0)
      .map(([idStr, qty]) => {
        const item = this.extraServices.find(s => s.id === Number(idStr));
        return {
          service: item,
          quantity: qty,
          subtotal: item ? item.price * qty : 0
        };
      })
      .filter(entry => entry.service !== undefined);
  });

  readonly cartHasKitchenItems = computed(() => {
    return this.cartItems().some(
      it => it.service?.category === 'GASTRONOMY' || it.service?.category === 'BAR_WINE'
    );
  });

  readonly cartHasSpaItems = computed(() => {
    return this.cartItems().some(it => it.service?.category === 'WELLNESS_SPA');
  });

  readonly orderDestinationType = computed<'KITCHEN' | 'SPA' | 'MIXED'>(() => {
    const hasKitchen = this.cartHasKitchenItems();
    const hasSpa = this.cartHasSpaItems();
    if (hasKitchen && hasSpa) return 'MIXED';
    if (hasSpa) return 'SPA';
    return 'KITCHEN';
  });

  readonly totalAccumulatedOrdersAmount = computed(() => {
    return this.activeReservationOrders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);
  });

  increaseCart(serviceId: number): void {
    const current = { ...this.cartQuantities() };
    current[serviceId] = (current[serviceId] || 0) + 1;
    this.cartQuantities.set(current);
  }

  decreaseCart(serviceId: number): void {
    const current = { ...this.cartQuantities() };
    const qty = (current[serviceId] || 0) - 1;
    if (qty <= 0) {
      delete current[serviceId];
    } else {
      current[serviceId] = qty;
    }
    this.cartQuantities.set(current);
  }

  clearCart(): void {
    this.cartQuantities.set({});
    this.orderNotes.set('');
  }

  onSubmit(): void {
    const items = Object.entries(this.cartQuantities())
      .filter(([_, qty]) => qty > 0)
      .map(([idStr, qty]) => ({ extraServiceId: Number(idStr), quantity: qty }));

    if (items.length === 0) return;

    this.submitOrder.emit({
      items,
      notes: this.orderNotes().trim() || undefined,
      serviceLocation: this.cartHasSpaItems() ? this.spaLocation() : undefined,
      scheduledTime: this.cartHasSpaItems() ? this.spaScheduledTime() : undefined
    });
  }

  onClose(): void {
    this.closeModal.emit();
  }

  getCategoryIcon(cat: ExtraCategory): string {
    switch (cat) {
      case 'GASTRONOMY': return 'restaurant';
      case 'BAR_WINE': return 'wine_bar';
      case 'WELLNESS_SPA': return 'spa';
      default: return 'room_service';
    }
  }

  getCategoryLabel(cat: ExtraCategory): string {
    switch (cat) {
      case 'GASTRONOMY': return this.i18n.t('roomService.catGastro');
      case 'BAR_WINE': return this.i18n.t('roomService.catWine');
      case 'WELLNESS_SPA': return this.i18n.t('roomService.catSpa');
      default: return cat;
    }
  }

  getOrderType(ord: RoomOrder): 'KITCHEN' | 'SPA' | 'MIXED' {
    const hasSpa = ord.items?.some(it => {
      const s = this.extraServices.find(es => es.id === it.extraServiceId);
      return s?.category === 'WELLNESS_SPA';
    }) || !!ord.therapistName;

    const hasKitchen = ord.items?.some(it => {
      const s = this.extraServices.find(es => es.id === it.extraServiceId);
      return s?.category === 'GASTRONOMY' || s?.category === 'BAR_WINE';
    });

    if (hasSpa && hasKitchen) return 'MIXED';
    if (hasSpa) return 'SPA';
    return 'KITCHEN';
  }

  getOrderStatusStep(status: OrderStatus): number {
    switch (status) {
      case 'RECEIVED': return 1;
      case 'PREPARING': return 2;
      case 'READY_FOR_DELIVERY': return 3;
      case 'DELIVERED': return 4;
      default: return 1;
    }
  }

  getOrderStatusLabel(status: OrderStatus, orderType: 'KITCHEN' | 'SPA' | 'MIXED'): string {
    if (orderType === 'SPA') {
      switch (status) {
        case 'RECEIVED': return this.i18n.t('roomService.statusSpaReceived');
        case 'PREPARING': return this.i18n.t('roomService.statusSpaPrep');
        case 'READY_FOR_DELIVERY': return this.i18n.t('roomService.statusSpaTransit');
        case 'DELIVERED': return this.i18n.t('roomService.statusSpaCompleted');
        default: return status;
      }
    }
    switch (status) {
      case 'RECEIVED': return this.i18n.t('roomService.statusReceived');
      case 'PREPARING': return this.i18n.t('roomService.statusPrep');
      case 'READY_FOR_DELIVERY': return this.i18n.t('roomService.statusOnWay');
      case 'DELIVERED': return this.i18n.t('roomService.statusDelivered');
      default: return status;
    }
  }
}
