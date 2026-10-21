import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { ReservationService } from '../../../core/services/reservation.service';
import { RoomService } from '../../../core/services/room.service';
import { AuthService } from '../../../core/services/auth.service';
import { Reservation, ReservationStatus } from '../../../core/models/reservation.model';
import { Room } from '../../../core/models/room.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  private readonly reservationService = inject(ReservationService);
  private readonly roomService = inject(RoomService);
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly reservations = signal<Reservation[]>([]);
  readonly rooms = signal<Room[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly isActionProcessing = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly searchQuery = signal<string>('');
  readonly statusFilter = signal<'ALL' | ReservationStatus>('ALL');

  readonly selectedVoucher = signal<Reservation | null>(null);
  readonly isVoucherModalOpen = signal<boolean>(false);

  readonly reservationToCancel = signal<Reservation | null>(null);
  readonly isCancelModalOpen = signal<boolean>(false);

  readonly copiedCodeId = signal<number | null>(null);

  readonly todayString = new Date().toISOString().split('T')[0];

  readonly metrics = computed(() => {
    const resList = this.reservations();
    const roomsList = this.rooms();

    const today = this.todayString;

    const checkInsToday = resList.filter(
      r => r.checkInDate === today && (r.status === 'CONFIRMED' || r.status === 'PENDING')
    ).length;

    const checkOutsToday = resList.filter(
      r => r.checkOutDate === today && r.status === 'CONFIRMED'
    ).length;

    const totalRevenue = resList
      .filter(r => r.status === 'CONFIRMED' || r.status === 'COMPLETED')
      .reduce((sum, r) => sum + (Number(r.totalPrice) || 0), 0);

    const totalRoomsCount = roomsList.length > 0 ? roomsList.length : 6;
    const occupiedCount = roomsList.filter(r => r.status === 'OCCUPIED').length;
    const availableCount = roomsList.filter(r => r.status === 'AVAILABLE').length;
    const maintenanceCount = roomsList.filter(r => r.status === 'MAINTENANCE').length;

    const activeReservationsCount = resList.filter(
      r => r.status === 'CONFIRMED' && r.checkInDate <= today && r.checkOutDate >= today
    ).length;

    const effectiveOccupied = Math.max(occupiedCount, activeReservationsCount);
    const occupancyRate = totalRoomsCount > 0
      ? Math.round((effectiveOccupied / totalRoomsCount) * 100)
      : 0;

    return {
      checkInsToday,
      checkOutsToday,
      totalRevenue,
      occupancyRate,
      totalRoomsCount,
      occupiedCount: effectiveOccupied,
      availableCount: Math.max(0, totalRoomsCount - effectiveOccupied - maintenanceCount),
      maintenanceCount,
      totalReservations: resList.length
    };
  });

  readonly filteredReservations = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const filter = this.statusFilter();

    return this.reservations().filter(res => {
      const matchesFilter = filter === 'ALL' || res.status === filter;
      if (!matchesFilter) return false;

      if (!query) return true;

      const code = `hosp-${res.id}-2026`.toLowerCase();
      const guestName = `${res.guest?.firstName || ''} ${res.guest?.lastName || ''}`.toLowerCase();
      const email = (res.guest?.email || '').toLowerCase();
      const phone = (res.guest?.phone || '').toLowerCase();
      const roomNum = (res.room?.roomNumber || '').toLowerCase();
      const suiteName = (res.room?.roomType?.name || '').toLowerCase();

      return (
        code.includes(query) ||
        guestName.includes(query) ||
        email.includes(query) ||
        phone.includes(query) ||
        roomNum.includes(query) ||
        suiteName.includes(query)
      );
    });
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.reservationService.getReservations().subscribe({
      next: (resData) => {
        this.reservations.set(resData || []);
        this.roomService.getRooms().subscribe({
          next: (roomsData) => {
            this.rooms.set(roomsData || []);
            this.isLoading.set(false);
          },
          error: () => {
            this.isLoading.set(false);
          }
        });
      },
      error: () => {
        this.errorMessage.set('Error al sincronizar las reservas con el servidor.');
        this.isLoading.set(false);
      }
    });
  }

  confirmReservation(id: number): void {
    this.isActionProcessing.set(true);
    this.errorMessage.set(null);
    this.reservationService.confirmReservation(id).subscribe({
      next: (updated) => {
        this.isActionProcessing.set(false);
        this.reservations.update(list => list.map(item => item.id === id ? updated : item));
        this.showToast('Reserva confirmada con éxito. Habitación asignada.');
      },
      error: (err) => {
        this.isActionProcessing.set(false);
        const msg = err.error?.message || 'No fue posible confirmar la reserva.';
        this.errorMessage.set(msg);
      }
    });
  }

  completeReservation(id: number): void {
    this.isActionProcessing.set(true);
    this.errorMessage.set(null);
    this.reservationService.completeReservation(id).subscribe({
      next: (updated) => {
        this.isActionProcessing.set(false);
        this.reservations.update(list => list.map(item => item.id === id ? updated : item));
        this.showToast('Check-out completado. Estancia cerrada con éxito.');
      },
      error: (err) => {
        this.isActionProcessing.set(false);
        const msg = err.error?.message || 'No fue posible completar el check-out en este momento.';
        this.errorMessage.set(msg);
      }
    });
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

    this.isActionProcessing.set(true);
    this.reservationService.cancelReservation(res.id).subscribe({
      next: (updated) => {
        this.isActionProcessing.set(false);
        this.reservations.update(list => list.map(item => item.id === res.id ? updated : item));
        this.closeCancelDialog();
        this.showToast('Reserva cancelada correctamente.');
      },
      error: (err) => {
        this.isActionProcessing.set(false);
        const msg = err.error?.message || 'Error al procesar la cancelación.';
        this.errorMessage.set(msg);
        this.closeCancelDialog();
      }
    });
  }

  openVoucher(res: Reservation): void {
    this.selectedVoucher.set(res);
    this.isVoucherModalOpen.set(true);
  }

  closeVoucher(): void {
    this.isVoucherModalOpen.set(false);
    this.selectedVoucher.set(null);
  }

  printVoucher(): void {
    window.print();
  }

  copyCode(code: string, id: number): void {
    navigator.clipboard.writeText(code).then(() => {
      this.copiedCodeId.set(id);
      setTimeout(() => {
        if (this.copiedCodeId() === id) {
          this.copiedCodeId.set(null);
        }
      }, 2000);
    });
  }

  calculateNights(checkIn: string, checkOut: string): number {
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    if (isNaN(start.getTime()) || isNaN(end.getTime())) return 1;
    const diff = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 1;
  }

  getStatusBadgeClass(status: ReservationStatus): string {
    switch (status) {
      case 'CONFIRMED': return 'badge-confirmed';
      case 'PENDING': return 'badge-pending';
      case 'COMPLETED': return 'badge-completed';
      case 'CANCELLED': return 'badge-cancelled';
      default: return '';
    }
  }

  getStatusLabel(status: ReservationStatus): string {
    switch (status) {
      case 'CONFIRMED': return 'Confirmada';
      case 'PENDING': return 'Pendiente';
      case 'COMPLETED': return 'Completada';
      case 'CANCELLED': return 'Cancelada';
      default: return status;
    }
  }

  showToast(message: string): void {
    this.successMessage.set(message);
    setTimeout(() => {
      if (this.successMessage() === message) {
        this.successMessage.set(null);
      }
    }, 4000);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/admin/login']);
  }
}
