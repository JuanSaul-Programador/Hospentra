import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HousekeepingUpdateRequest, Room, RoomStatus } from '../../../../../core/models/room.model';

@Component({
  selector: 'app-room-rack',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './room-rack.component.html',
  styleUrl: './room-rack.component.scss'
})
export class RoomRackComponent {
  @Input() rooms: Room[] = [];
  @Input() isActionProcessing: boolean = false;
  @Output() markClean = new EventEmitter<number>();
  @Output() saveHousekeeping = new EventEmitter<{ roomId: number; request: HousekeepingUpdateRequest }>();

  selectedFilter = signal<'ALL' | '1' | '2' | '3' | '4' | 'CLEANING'>('ALL');
  searchQuery = signal<string>('');

  activeModalRoom = signal<Room | null>(null);
  modalHousekeeper = signal<string>('');
  modalPriority = signal<'HIGH' | 'NORMAL' | 'LOW'>('NORMAL');
  modalNotes = signal<string>('');
  modalIncidentReported = signal<boolean>(false);
  modalChecklist = signal<string[]>([]);
  isSavingModal = signal<boolean>(false);

  readonly housekeepersList: string[] = [
    'Elena Rostova (Gobernanta General)',
    'María Cruz (Camarista Pisos 1 y 2)',
    'Rosa Morales (Camarista Pisos 3)',
    'Carmen Delgado (Especialista Penthouse)',
    'Lucía Mendoza (Camarista de Guardia)',
    'Valeria Silva (Supervisora de Calidad)'
  ];

  readonly standardChecklist: string[] = [
    'Minibar auditado y reabastecido',
    'Lencería egipcia y toallas renovadas',
    'Jacuzzi & terraza higienizados',
    'Amenities botánicos completos',
    'Aromaterapia ambiental lista'
  ];

  totalRoomsCount = computed(() => this.rooms.length);
  availableRoomsCount = computed(() => this.rooms.filter(r => r.status === 'AVAILABLE').length);
  maintenanceRoomsCount = computed(() => this.rooms.filter(r => r.status === 'MAINTENANCE').length);
  occupiedRoomsCount = computed(() => this.rooms.filter(r => r.status === 'OCCUPIED').length);
  highPriorityCount = computed(() => this.rooms.filter(r => r.status === 'MAINTENANCE' && r.cleaningPriority === 'HIGH').length);

  filteredRooms = computed(() => {
    const list = this.rooms;
    const filter = this.selectedFilter();
    const query = this.searchQuery().trim().toLowerCase();

    return list.filter(room => {
      if (filter === 'CLEANING' && room.status !== 'MAINTENANCE') {
        return false;
      }
      if (['1', '2', '3', '4'].includes(filter)) {
        if (!room.roomNumber.startsWith(filter)) {
          return false;
        }
      }
      if (query) {
        const matchNumber = room.roomNumber.toLowerCase().includes(query);
        const matchType = room.roomType?.name?.toLowerCase().includes(query);
        const matchStaff = room.housekeeperName?.toLowerCase().includes(query);
        const matchNotes = room.housekeepingNotes?.toLowerCase().includes(query);
        return matchNumber || matchType || matchStaff || matchNotes;
      }
      return true;
    });
  });

  setFilter(filter: 'ALL' | '1' | '2' | '3' | '4' | 'CLEANING'): void {
    this.selectedFilter.set(filter);
  }

  getFloorLabel(roomNumber: string): string {
    if (!roomNumber) return 'Nivel Principal';
    const firstDigit = roomNumber.charAt(0);
    switch (firstDigit) {
      case '1': return 'Piso 1 • Jardín & Terrazas';
      case '2': return 'Piso 2 • Vista al Océano';
      case '3': return 'Piso 3 • Nivel Panorámico';
      case '4': return 'Piso 4 • Penthouses & Master Suites';
      default: return `Piso ${firstDigit}`;
    }
  }

  formatCleanedDate(dateStr?: string): string {
    if (!dateStr) return 'Sin registro reciente';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return 'Hoy';
      const hours = d.getHours().toString().padStart(2, '0');
      const minutes = d.getMinutes().toString().padStart(2, '0');
      const day = d.getDate();
      const monthNames = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
      const month = monthNames[d.getMonth()];
      return `${day} ${month}, ${hours}:${minutes} hrs`;
    } catch {
      return 'Registrado';
    }
  }

  onQuickClean(room: Room): void {
    this.markClean.emit(room.id);
  }

  openHousekeepingModal(room: Room): void {
    this.activeModalRoom.set(room);
    this.modalHousekeeper.set(room.housekeeperName || '');
    this.modalPriority.set((room.cleaningPriority as any) || 'NORMAL');

    const notes = room.housekeepingNotes || '';
    const hasIncident = notes.includes('[INCIDENCIA]') || notes.includes('Reporte de Incidencia');
    this.modalIncidentReported.set(hasIncident);

    const activeChecks: string[] = [];
    this.standardChecklist.forEach(item => {
      if (notes.includes(item)) {
        activeChecks.push(item);
      }
    });
    this.modalChecklist.set(activeChecks);

    const cleanNotes = notes
      .replace(/\[INCIDENCIA\]/g, '')
      .replace(/Reporte de Incidencia:\s*/g, '')
      .trim();
    this.modalNotes.set(cleanNotes);
  }

  closeHousekeepingModal(): void {
    this.activeModalRoom.set(null);
    this.modalHousekeeper.set('');
    this.modalPriority.set('NORMAL');
    this.modalNotes.set('');
    this.modalIncidentReported.set(false);
    this.modalChecklist.set([]);
    this.isSavingModal.set(false);
  }

  toggleChecklistItem(item: string): void {
    const current = this.modalChecklist();
    if (current.includes(item)) {
      this.modalChecklist.set(current.filter(i => i !== item));
    } else {
      this.modalChecklist.set([...current, item]);
    }
  }

  toggleIncidentReport(): void {
    this.modalIncidentReported.set(!this.modalIncidentReported());
  }

  saveModal(markReady: boolean = false): void {
    const room = this.activeModalRoom();
    if (!room) return;

    this.isSavingModal.set(true);

    const parts: string[] = [];
    if (this.modalIncidentReported()) {
      parts.push('[INCIDENCIA]');
    }
    const checks = this.modalChecklist();
    if (checks.length > 0) {
      parts.push(checks.join(' • '));
    }
    const textNote = this.modalNotes().trim();
    if (textNote) {
      parts.push(textNote);
    }

    const finalNotes = parts.join(' | ');

    const payload: HousekeepingUpdateRequest = {
      housekeeperName: this.modalHousekeeper(),
      housekeepingNotes: finalNotes,
      cleaningPriority: this.modalPriority(),
      markReady: markReady
    };

    this.saveHousekeeping.emit({
      roomId: room.id,
      request: payload
    });

    this.closeHousekeepingModal();
  }
}
