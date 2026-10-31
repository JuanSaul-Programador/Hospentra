import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Room } from '../../../../../core/models/room.model';
import { BlockType, BlockStatus, CreateMaintenanceBlockRequest, MaintenanceBlock } from '../../../../../core/models/maintenance.model';

@Component({
  selector: 'app-maintenance-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './maintenance-modal.component.html',
  styleUrl: './maintenance-modal.component.scss'
})
export class MaintenanceModalComponent implements OnChanges {
  @Input() isOpen: boolean = false;
  @Input() rooms: Room[] = [];
  @Input() preselectedRoom: Room | null = null;
  @Input() blocks: MaintenanceBlock[] = [];
  @Input() isProcessing: boolean = false;
  @Input() externalErrorMessage: string | null = null;

  @Output() close = new EventEmitter<void>();
  @Output() createBlock = new EventEmitter<CreateMaintenanceBlockRequest>();
  @Output() resolveBlock = new EventEmitter<number>();
  @Output() cancelBlock = new EventEmitter<number>();

  activeTab = signal<'LIST' | 'CREATE'>('LIST');
  statusFilter = signal<'ALL' | 'ACTIVE' | 'RESOLVED' | 'CANCELLED'>('ACTIVE');

  formRoomId = signal<number | null>(null);
  formBlockType = signal<BlockType>('OUT_OF_ORDER');
  formStartDate = signal<string>('');
  formEndDate = signal<string>('');
  formReason = signal<string>('');
  formContractor = signal<string>('');
  formValidationError = signal<string | null>(null);

  activeBlocksCount = computed(() => this.blocks.filter(b => b.status === 'ACTIVE').length);

  filteredBlocks = computed(() => {
    const filter = this.statusFilter();
    if (filter === 'ALL') {
      return this.blocks;
    }
    return this.blocks.filter(b => b.status === filter);
  });

  calculatedNights = computed(() => {
    const startStr = this.formStartDate();
    const endStr = this.formEndDate();
    if (!startStr || !endStr) return 0;
    const start = new Date(startStr);
    const end = new Date(endStr);
    const diff = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 0;
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.resetForm();
      if (this.preselectedRoom) {
        this.formRoomId.set(this.preselectedRoom.id);
        this.activeTab.set('CREATE');
      } else {
        this.activeTab.set(this.activeBlocksCount() > 0 ? 'LIST' : 'CREATE');
      }
    }
  }

  private resetForm(): void {
    const today = new Date();
    const todayStr = this.formatDate(today);
    const future = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 3);
    const futureStr = this.formatDate(future);

    this.formRoomId.set(this.preselectedRoom ? this.preselectedRoom.id : (this.rooms.length > 0 ? this.rooms[0].id : null));
    this.formBlockType.set('OUT_OF_ORDER');
    this.formStartDate.set(todayStr);
    this.formEndDate.set(futureStr);
    this.formReason.set('');
    this.formContractor.set('');
    this.formValidationError.set(null);
  }

  private formatDate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  submitCreate(): void {
    this.formValidationError.set(null);

    const roomId = this.formRoomId();
    const blockType = this.formBlockType();
    const startDate = this.formStartDate();
    const endDate = this.formEndDate();
    const reason = this.formReason().trim();
    const contractor = this.formContractor().trim();

    if (!roomId) {
      this.formValidationError.set('Debe seleccionar una suite para aplicar el bloqueo.');
      return;
    }
    if (!startDate || !endDate) {
      this.formValidationError.set('Debe especificar la fecha de inicio y la fecha de finalización.');
      return;
    }
    if (endDate <= startDate) {
      this.formValidationError.set('La fecha de fin debe ser estrictamente posterior a la fecha de inicio.');
      return;
    }
    if (!reason) {
      this.formValidationError.set('El motivo técnico del bloqueo es obligatorio.');
      return;
    }

    const payload: CreateMaintenanceBlockRequest = {
      roomId,
      blockType,
      startDate,
      endDate,
      reason,
      contractor: contractor ? contractor : undefined
    };

    this.createBlock.emit(payload);
  }

  onResolve(id: number): void {
    this.resolveBlock.emit(id);
  }

  onCancel(id: number): void {
    this.cancelBlock.emit(id);
  }

  onClose(): void {
    this.close.emit();
  }
}
