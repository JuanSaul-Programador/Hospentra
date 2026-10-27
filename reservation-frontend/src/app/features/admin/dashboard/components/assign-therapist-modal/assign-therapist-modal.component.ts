import { Component, EventEmitter, Input, Output, signal, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RoomOrder } from '../../../../../core/models/room-order.model';
import { Therapist } from '../../../../../core/models/therapist.model';

@Component({
  selector: 'app-assign-therapist-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './assign-therapist-modal.component.html',
  styleUrl: './assign-therapist-modal.component.scss'
})
export class AssignTherapistModalComponent implements OnChanges {
  @Input() isOpen: boolean = false;
  @Input() order: RoomOrder | null = null;
  @Input() activeTherapists: Therapist[] = [];
  @Input() busyTherapistIds: number[] = [];
  @Input() isAssigning: boolean = false;

  @Output() close = new EventEmitter<void>();
  @Output() assign = new EventEmitter<{ orderId: number; therapistId: number }>();
  @Output() openDirectory = new EventEmitter<void>();

  readonly selectedTherapistId = signal<number | null>(null);

  isBusy(id: number): boolean {
    return this.busyTherapistIds.includes(id);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.selectedTherapistId.set(null);
    }
  }

  selectTherapist(id: number): void {
    if (this.isBusy(id)) {
      return;
    }
    this.selectedTherapistId.set(id);
  }

  onConfirm(): void {
    if (!this.order || !this.selectedTherapistId()) {
      return;
    }
    this.assign.emit({
      orderId: this.order.id,
      therapistId: this.selectedTherapistId()!
    });
  }

  onOpenDirectory(): void {
    this.openDirectory.emit();
  }

  onClose(): void {
    this.close.emit();
  }
}
