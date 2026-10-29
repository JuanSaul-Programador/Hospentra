import { Component, Input, Output, EventEmitter, inject, signal, computed, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Reservation } from '../../../core/models/reservation.model';
import { RoomOrder } from '../../../core/models/room-order.model';
import { RoomOrderService } from '../../../core/services/room-order.service';

@Component({
  selector: 'app-hotel-folio-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './hotel-folio-modal.component.html',
  styleUrl: './hotel-folio-modal.component.scss'
})
export class HotelFolioModalComponent implements OnChanges {
  private readonly roomOrderService = inject(RoomOrderService);

  @Input() reservation: Reservation | null = null;
  @Input() isOpen: boolean = false;
  @Output() closeModal = new EventEmitter<void>();

  readonly roomOrders = signal<RoomOrder[]>([]);
  readonly isLoadingOrders = signal<boolean>(false);

  readonly invoiceNumber = computed(() => {
    const id = this.reservation?.id ? String(this.reservation.id).padStart(5, '0') : '00001';
    return `INV-HOSP-${id}-2026`;
  });

  readonly issueDate = new Date();

  readonly stayNights = computed(() => {
    if (!this.reservation?.checkInDate || !this.reservation?.checkOutDate) return 1;
    const start = new Date(this.reservation.checkInDate).getTime();
    const end = new Date(this.reservation.checkOutDate).getTime();
    const diffDays = Math.ceil((end - start) / (1000 * 3600 * 24));
    return diffDays > 0 ? diffDays : 1;
  });

  readonly baseRoomTotal = computed(() => {
    return Number(this.reservation?.totalPrice) || 0;
  });

  readonly pricePerNight = computed(() => {
    const nights = this.stayNights();
    return nights > 0 ? this.baseRoomTotal() / nights : this.baseRoomTotal();
  });

  readonly extrasTotal = computed(() => {
    return this.roomOrders().reduce((sum, order) => sum + (Number(order.totalAmount) || 0), 0);
  });

  readonly grandTotal = computed(() => {
    return this.baseRoomTotal() + this.extrasTotal();
  });

  readonly taxAmount = computed(() => {
    return Math.round(this.grandTotal() * 0.16 * 100) / 100;
  });

  readonly netSubtotal = computed(() => {
    return Math.round((this.grandTotal() - this.taxAmount()) * 100) / 100;
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['reservation'] || changes['isOpen']) && this.isOpen && this.reservation?.id) {
      this.loadReservationOrders(this.reservation.id);
    }
  }

  loadReservationOrders(reservationId: number): void {
    this.isLoadingOrders.set(true);
    this.roomOrderService.getOrdersByReservation(reservationId).subscribe({
      next: (orders) => {
        this.roomOrders.set(orders || []);
        this.isLoadingOrders.set(false);
      },
      error: () => {
        this.roomOrders.set([]);
        this.isLoadingOrders.set(false);
      }
    });
  }

  onClose(): void {
    this.closeModal.emit();
  }

  onPrint(): void {
    window.print();
  }
}
