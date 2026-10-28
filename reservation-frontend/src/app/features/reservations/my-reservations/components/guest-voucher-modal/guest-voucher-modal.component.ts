import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Reservation } from '../../../../../core/models/reservation.model';
import { I18nService } from '../../../../../core/services/i18n.service';

@Component({
  selector: 'app-guest-voucher-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './guest-voucher-modal.component.html',
  styleUrl: './guest-voucher-modal.component.scss'
})
export class GuestVoucherModalComponent {
  readonly i18n = inject(I18nService);

  @Input() reservation: Reservation | null = null;
  @Input() isOpen: boolean = false;

  @Output() closeModal = new EventEmitter<void>();
  @Output() print = new EventEmitter<void>();

  onClose(): void {
    this.closeModal.emit();
  }

  onPrint(): void {
    this.print.emit();
  }
}
