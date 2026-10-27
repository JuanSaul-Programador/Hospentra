import { Component, EventEmitter, Input, Output, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Room, RoomRequest, RoomStatus, RoomType, RoomTypeRequest } from '../../../../../core/models/room.model';
import { RoomService } from '../../../../../core/services/room.service';

@Component({
  selector: 'app-suite-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './suite-inventory.component.html',
  styleUrl: './suite-inventory.component.scss'
})
export class SuiteInventoryComponent {
  private readonly roomService = inject(RoomService);

  @Input() rooms: Room[] = [];
  @Input() roomTypes: RoomType[] = [];
  @Input() isLoading: boolean = false;

  @Output() refreshRooms = new EventEmitter<void>();
  @Output() refreshRoomTypes = new EventEmitter<void>();
  @Output() notify = new EventEmitter<{ type: 'success' | 'error'; message: string }>();

  readonly activeSubTab = signal<'ROOM_TYPES' | 'ROOMS'>('ROOM_TYPES');

  readonly isRoomTypeModalOpen = signal<boolean>(false);
  readonly editingRoomTypeId = signal<number | null>(null);
  readonly roomTypeForm = signal<RoomTypeRequest>({
    name: '',
    nameEn: '',
    description: '',
    descriptionEn: '',
    capacity: 2,
    pricePerNight: 250
  });

  readonly isRoomModalOpen = signal<boolean>(false);
  readonly editingRoomId = signal<number | null>(null);
  readonly roomForm = signal<RoomRequest>({
    roomNumber: '',
    status: 'AVAILABLE',
    roomTypeId: 0
  });

  readonly isDeleteModalOpen = signal<boolean>(false);
  readonly deleteTarget = signal<{ type: 'ROOM_TYPE' | 'ROOM'; id: number; name: string } | null>(null);
  readonly isSaving = signal<boolean>(false);
  readonly formError = signal<string | null>(null);

  readonly availableRoomsCount = computed(() => this.rooms.filter(r => r.status === 'AVAILABLE').length);
  readonly occupiedRoomsCount = computed(() => this.rooms.filter(r => r.status === 'OCCUPIED').length);
  readonly maintenanceRoomsCount = computed(() => this.rooms.filter(r => r.status === 'MAINTENANCE').length);

  countRoomsByType(roomTypeId: number): number {
    return this.rooms.filter(r => r.roomType?.id === roomTypeId).length;
  }

  getRoomTypeName(roomTypeId: number): string {
    const found = this.roomTypes.find(rt => rt.id === roomTypeId);
    return found ? found.name : 'No asignado';
  }

  openCreateRoomTypeModal(): void {
    this.editingRoomTypeId.set(null);
    this.formError.set(null);
    this.roomTypeForm.set({
      name: '',
      nameEn: '',
      description: '',
      descriptionEn: '',
      capacity: 2,
      pricePerNight: 280
    });
    this.isRoomTypeModalOpen.set(true);
  }

  openEditRoomTypeModal(rt: RoomType): void {
    this.editingRoomTypeId.set(rt.id);
    this.formError.set(null);
    this.roomTypeForm.set({
      name: rt.name,
      nameEn: rt.nameEn || '',
      description: rt.description || '',
      descriptionEn: rt.descriptionEn || '',
      capacity: rt.capacity,
      pricePerNight: rt.pricePerNight
    });
    this.isRoomTypeModalOpen.set(true);
  }

  closeRoomTypeModal(): void {
    this.isRoomTypeModalOpen.set(false);
    this.formError.set(null);
  }

  saveRoomType(): void {
    const form = this.roomTypeForm();
    if (!form.name.trim()) {
      this.formError.set('El nombre de la categoría en español es obligatorio.');
      return;
    }
    if (form.pricePerNight <= 0) {
      this.formError.set('La tarifa por noche debe ser un valor positivo mayor a 0.');
      return;
    }
    if (form.capacity < 1) {
      this.formError.set('La capacidad máxima debe ser de al menos 1 huésped.');
      return;
    }

    this.isSaving.set(true);
    this.formError.set(null);

    const payload: RoomTypeRequest = {
      name: form.name.trim(),
      nameEn: form.nameEn?.trim() || undefined,
      description: form.description?.trim() || undefined,
      descriptionEn: form.descriptionEn?.trim() || undefined,
      capacity: Number(form.capacity),
      pricePerNight: Number(form.pricePerNight)
    };

    const editId = this.editingRoomTypeId();
    if (editId) {
      this.roomService.updateRoomType(editId, payload).subscribe({
        next: (updated) => {
          this.isSaving.set(false);
          this.closeRoomTypeModal();
          this.refreshRoomTypes.emit();
          this.refreshRooms.emit();
          this.notify.emit({
            type: 'success',
            message: `Categoría de Suite "${updated.name}" actualizada con éxito.`
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          const msg = err.error?.detail || err.error?.message || 'Error al actualizar la categoría de suite.';
          this.formError.set(msg);
        }
      });
    } else {
      this.roomService.createRoomType(payload).subscribe({
        next: (created) => {
          this.isSaving.set(false);
          this.closeRoomTypeModal();
          this.refreshRoomTypes.emit();
          this.notify.emit({
            type: 'success',
            message: `Nueva categoría de suite "${created.name}" creada con éxito.`
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          const msg = err.error?.detail || err.error?.message || 'Error al registrar la categoría de suite.';
          this.formError.set(msg);
        }
      });
    }
  }

  openCreateRoomModal(): void {
    this.editingRoomId.set(null);
    this.formError.set(null);
    const defaultTypeId = this.roomTypes.length > 0 ? this.roomTypes[0].id : 0;
    this.roomForm.set({
      roomNumber: '',
      status: 'AVAILABLE',
      roomTypeId: defaultTypeId
    });
    this.isRoomModalOpen.set(true);
  }

  openEditRoomModal(room: Room): void {
    this.editingRoomId.set(room.id);
    this.formError.set(null);
    this.roomForm.set({
      roomNumber: room.roomNumber,
      status: room.status,
      roomTypeId: room.roomType?.id || (this.roomTypes.length > 0 ? this.roomTypes[0].id : 0)
    });
    this.isRoomModalOpen.set(true);
  }

  closeRoomModal(): void {
    this.isRoomModalOpen.set(false);
    this.formError.set(null);
  }

  saveRoom(): void {
    const form = this.roomForm();
    if (!form.roomNumber.trim()) {
      this.formError.set('El número de suite o habitación es obligatorio.');
      return;
    }
    if (!form.roomTypeId || form.roomTypeId === 0) {
      this.formError.set('Debe seleccionar una categoría de suite válida.');
      return;
    }

    this.isSaving.set(true);
    this.formError.set(null);

    const payload: RoomRequest = {
      roomNumber: form.roomNumber.trim(),
      status: form.status,
      roomTypeId: Number(form.roomTypeId)
    };

    const editId = this.editingRoomId();
    if (editId) {
      this.roomService.updateRoom(editId, payload).subscribe({
        next: (updated) => {
          this.isSaving.set(false);
          this.closeRoomModal();
          this.refreshRooms.emit();
          this.notify.emit({
            type: 'success',
            message: `Habitación ${updated.roomNumber} actualizada con éxito.`
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          const msg = err.error?.detail || err.error?.message || 'Error al actualizar la habitación.';
          this.formError.set(msg);
        }
      });
    } else {
      this.roomService.createRoom(payload).subscribe({
        next: (created) => {
          this.isSaving.set(false);
          this.closeRoomModal();
          this.refreshRooms.emit();
          this.notify.emit({
            type: 'success',
            message: `Habitación ${created.roomNumber} dada de alta exitosamente en el inventario.`
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          const msg = err.error?.detail || err.error?.message || 'Error al registrar la habitación. Verifique que el número no esté duplicado.';
          this.formError.set(msg);
        }
      });
    }
  }

  confirmDeleteRoomType(rt: RoomType): void {
    const assignedCount = this.countRoomsByType(rt.id);
    if (assignedCount > 0) {
      this.notify.emit({
        type: 'error',
        message: `No se puede eliminar la categoría "${rt.name}" porque tiene ${assignedCount} habitación(es) física(s) asignada(s). Reasigne o elimine primero dichas habitaciones.`
      });
      return;
    }
    this.deleteTarget.set({ type: 'ROOM_TYPE', id: rt.id, name: rt.name });
    this.isDeleteModalOpen.set(true);
  }

  confirmDeleteRoom(room: Room): void {
    if (room.status === 'OCCUPIED') {
      this.notify.emit({
        type: 'error',
        message: `No se puede eliminar la Habitación ${room.roomNumber} porque actualmente se encuentra OCUPADA con una estancia activa.`
      });
      return;
    }
    this.deleteTarget.set({ type: 'ROOM', id: room.id, name: `Habitación ${room.roomNumber}` });
    this.isDeleteModalOpen.set(true);
  }

  closeDeleteModal(): void {
    this.isDeleteModalOpen.set(false);
    this.deleteTarget.set(null);
  }

  executeDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;

    this.isSaving.set(true);
    if (target.type === 'ROOM_TYPE') {
      this.roomService.deleteRoomType(target.id).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeDeleteModal();
          this.refreshRoomTypes.emit();
          this.notify.emit({
            type: 'success',
            message: `Categoría "${target.name}" eliminada correctamente.`
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          const msg = err.error?.detail || err.error?.message || 'Error al eliminar la categoría de suite.';
          this.notify.emit({ type: 'error', message: msg });
        }
      });
    } else {
      this.roomService.deleteRoom(target.id).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeDeleteModal();
          this.refreshRooms.emit();
          this.notify.emit({
            type: 'success',
            message: `${target.name} eliminada del inventario del hotel.`
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          const msg = err.error?.detail || err.error?.message || 'Error al eliminar la habitación.';
          this.notify.emit({ type: 'error', message: msg });
        }
      });
    }
  }

  getStatusClass(status: RoomStatus): string {
    switch (status) {
      case 'AVAILABLE': return 'status-available';
      case 'OCCUPIED': return 'status-occupied';
      case 'MAINTENANCE': return 'status-maintenance';
      default: return '';
    }
  }

  getStatusLabel(status: RoomStatus): string {
    switch (status) {
      case 'AVAILABLE': return 'Disponible';
      case 'OCCUPIED': return 'Ocupada (In-House)';
      case 'MAINTENANCE': return 'En Mantenimiento';
      default: return status;
    }
  }
}
