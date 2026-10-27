import { Component, EventEmitter, Input, Output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Reservation, ReservationStatus } from '../../../../../core/models/reservation.model';
import { RoomOrder } from '../../../../../core/models/room-order.model';

@Component({
  selector: 'app-voucher-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './voucher-modal.component.html',
  styleUrl: './voucher-modal.component.scss'
})
export class VoucherModalComponent {
  @Input() reservation: Reservation | null = null;
  @Input() folioOrders: RoomOrder[] = [];
  @Output() close = new EventEmitter<void>();

  get extrasTotal(): number {
    return this.folioOrders.reduce((sum, order) => sum + Number(order.totalAmount || 0), 0);
  }

  get grandTotal(): number {
    return (Number(this.reservation?.totalPrice || 0)) + this.extrasTotal;
  }

  getStatusLabel(status: ReservationStatus): string {
    switch (status) {
      case 'PENDING': return 'Pendiente';
      case 'CONFIRMED': return 'Confirmada';
      case 'CHECKED_IN': return 'En Casa';
      case 'COMPLETED': return 'Completada';
      case 'CANCELLED': return 'Cancelada';
      default: return status;
    }
  }

  printVoucher(): void {
    window.print();
  }

  onClose(): void {
    this.close.emit();
  }
}
