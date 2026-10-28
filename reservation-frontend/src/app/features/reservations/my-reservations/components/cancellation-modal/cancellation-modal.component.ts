import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Reservation } from '../../../../../core/models/reservation.model';
import { I18nService } from '../../../../../core/services/i18n.service';

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
  selector: 'app-cancellation-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cancellation-modal.component.html',
  styleUrl: './cancellation-modal.component.scss'
})
export class CancellationModalComponent {
  readonly i18n = inject(I18nService);

  @Input() reservation: Reservation | null = null;
  @Input() isOpen: boolean = false;
  @Input() isCancelling: boolean = false;
  @Input() cancelCalculation: RefundCalculation | null = null;

  @Input() isResultModalOpen: boolean = false;
  @Input() cancelResult: RefundCalculation | null = null;

  @Output() confirmCancel = new EventEmitter<void>();
  @Output() closeModal = new EventEmitter<void>();
  @Output() closeResultModal = new EventEmitter<void>();

  onConfirm(): void {
    this.confirmCancel.emit();
  }

  onClose(): void {
    this.closeModal.emit();
  }

  onCloseResult(): void {
    this.closeResultModal.emit();
  }
}
