import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { ReservationService } from '../../../core/services/reservation.service';
import { RoomService } from '../../../core/services/room.service';
import { AuthService } from '../../../core/services/auth.service';
import { StaffService } from '../../../core/services/staff.service';
import { RoomOrderService } from '../../../core/services/room-order.service';
import { Reservation, ReservationStatus } from '../../../core/models/reservation.model';
import { Room, RoomStatus } from '../../../core/models/room.model';
import { Role } from '../../../core/models/auth.model';
import { Shift, UserResponse, WorkStatus } from '../../../core/models/staff.model';
import { OrderStatus, RoomOrder } from '../../../core/models/room-order.model';

export type AdminFilterTab = 'ALL' | 'TODAY_ARRIVALS' | 'IN_HOUSE' | 'TODAY_DEPARTURES' | ReservationStatus;

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  private readonly reservationService = inject(ReservationService);
  private readonly roomService = inject(RoomService);
  private readonly staffService = inject(StaffService);
  private readonly roomOrderService = inject(RoomOrderService);
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isAdmin = computed(() => this.authService.userRole() === 'ROLE_ADMIN');
  readonly isReceptionist = computed(() => this.authService.userRole() === 'ROLE_RECEPTIONIST');

  readonly activeSection = signal<'OPERATIONS' | 'ROOM_SERVICE' | 'STAFF'>('OPERATIONS');

  readonly reservations = signal<Reservation[]>([]);
  readonly rooms = signal<Room[]>([]);
  readonly roomOrders = signal<RoomOrder[]>([]);
  readonly activeOrdersCount = computed(() => {
    return this.roomOrders().filter(o => o.status === 'RECEIVED' || o.status === 'PREPARING' || o.status === 'READY_FOR_DELIVERY').length;
  });
  readonly deliveredOrdersCount = computed(() => {
    return this.roomOrders().filter(o => o.status === 'DELIVERED').length;
  });
  readonly isLoading = signal<boolean>(true);
  readonly isActionProcessing = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly searchQuery = signal<string>('');
  readonly statusFilter = signal<AdminFilterTab>('ALL');

  readonly currentWorkStatus = signal<WorkStatus>('ON_DUTY');
  readonly breakSeconds = signal<number>(0);
  private breakInterval: any = null;

  readonly breakTimerText = computed(() => {
    const sec = this.breakSeconds();
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return m > 0 ? `${m}m ${s}s` : `${s}s`;
  });

  readonly reservationToCheckIn = signal<Reservation | null>(null);
  readonly isCheckInModalOpen = signal<boolean>(false);
  readonly paymentMethodCollected = signal<'POS_CARD' | 'CASH'>('POS_CARD');

  readonly reservationToCheckOut = signal<Reservation | null>(null);
  readonly isCheckOutModalOpen = signal<boolean>(false);
  readonly checkOutOrders = signal<RoomOrder[]>([]);

  readonly checkOutExtrasTotal = computed(() => {
    return this.checkOutOrders().reduce((sum, order) => sum + (Number(order.totalAmount) || 0), 0);
  });

  readonly checkOutGrandTotal = computed(() => {
    const res = this.reservationToCheckOut();
    if (!res) return 0;
    return (Number(res.totalPrice) || 0) + this.checkOutExtrasTotal();
  });

  readonly checkOutPendingBalance = computed(() => {
    const res = this.reservationToCheckOut();
    if (!res) return 0;
    const extras = this.checkOutExtrasTotal();
    if (res.paymentMethod === 'CREDIT_CARD') {
      return extras;
    }
    return (Number(res.totalPrice) || 0) + extras;
  });

  readonly selectedVoucher = signal<Reservation | null>(null);
  readonly voucherOrders = signal<RoomOrder[]>([]);
  readonly voucherExtrasTotal = computed(() => {
    return this.voucherOrders().reduce((sum, o) => sum + (Number(o.totalAmount) || 0), 0);
  });
  readonly voucherGrandTotal = computed(() => {
    const v = this.selectedVoucher();
    if (!v) return 0;
    return (Number(v.totalPrice) || 0) + this.voucherExtrasTotal();
  });
  readonly isVoucherModalOpen = signal<boolean>(false);

  readonly reservationToCancel = signal<Reservation | null>(null);
  readonly isCancelModalOpen = signal<boolean>(false);

  readonly copiedCodeId = signal<number | null>(null);

  readonly staffList = signal<UserResponse[]>([]);
  readonly isStaffLoading = signal<boolean>(false);
  readonly isStaffModalOpen = signal<boolean>(false);
  readonly isCreatingStaff = signal<boolean>(false);
  readonly staffFormError = signal<string | null>(null);

  readonly newUsername = signal<string>('');
  readonly newEmail = signal<string>('');
  readonly newPassword = signal<string>('');
  readonly newConfirmPassword = signal<string>('');
  readonly newRole = signal<Role>('ROLE_RECEPTIONIST');
  readonly newShift = signal<Shift>('MORNING');
  readonly showNewPassword = signal<boolean>(false);

  readonly todayString = new Date().toISOString().split('T')[0];

  readonly staffMetrics = computed(() => {
    const list = this.staffList();
    const receptionists = list.filter(u => u.role === 'ROLE_RECEPTIONIST').length;
    const kitchenStaff = list.filter(u => u.role === 'ROLE_KITCHEN').length;
    const admins = list.filter(u => u.role === 'ROLE_ADMIN').length;
    return {
      total: list.length,
      receptionists,
      kitchenStaff,
      admins
    };
  });

  readonly metrics = computed(() => {
    const resList = this.reservations();
    const roomsList = this.rooms();
    const today = this.todayString;

    const checkInsToday = resList.filter(
      r => r.checkInDate === today && (r.status === 'CONFIRMED' || r.status === 'PENDING')
    ).length;

    const inHouseCount = resList.filter(r => r.status === 'CHECKED_IN').length;

    const checkOutsToday = resList.filter(
      r => r.checkOutDate === today && (r.status === 'CHECKED_IN' || r.status === 'CONFIRMED')
    ).length;

    const totalRevenue = resList
      .filter(r => r.status === 'CONFIRMED' || r.status === 'CHECKED_IN' || r.status === 'COMPLETED')
      .reduce((sum, r) => sum + (Number(r.totalPrice) || 0), 0);

    const totalRoomsCount = roomsList.length > 0 ? roomsList.length : 6;
    const occupiedCount = roomsList.filter(r => r.status === 'OCCUPIED').length;
    const availableCount = roomsList.filter(r => r.status === 'AVAILABLE').length;
    const maintenanceCount = roomsList.filter(r => r.status === 'MAINTENANCE').length;

    const effectiveOccupied = Math.max(occupiedCount, inHouseCount);
    const occupancyRate = totalRoomsCount > 0
      ? Math.round((effectiveOccupied / totalRoomsCount) * 100)
      : 0;

    return {
      checkInsToday,
      inHouseCount,
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
    const today = this.todayString;

    return this.reservations().filter(res => {
      let matchesFilter = true;
      if (filter === 'ALL') {
        matchesFilter = true;
      } else if (filter === 'TODAY_ARRIVALS') {
        matchesFilter = res.checkInDate === today && (res.status === 'CONFIRMED' || res.status === 'PENDING');
      } else if (filter === 'IN_HOUSE') {
        matchesFilter = res.status === 'CHECKED_IN';
      } else if (filter === 'TODAY_DEPARTURES') {
        matchesFilter = res.checkOutDate === today && (res.status === 'CHECKED_IN' || res.status === 'CONFIRMED');
      } else {
        matchesFilter = res.status === filter;
      }

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
    this.loadOrders();
    if (this.isAdmin()) {
      this.loadStaff();
    }
  }

  ngOnDestroy(): void {
    if (this.breakInterval) {
      clearInterval(this.breakInterval);
    }
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

  loadOrders(): void {
    this.roomOrderService.getAllOrders().subscribe({
      next: (orders) => {
        this.roomOrders.set(orders || []);
      },
      error: () => {}
    });
  }

  getOrderType(order: RoomOrder): 'KITCHEN' | 'SPA' | 'MIXED' {
    const hasKitchen = (order.items || []).some(
      it => it.category === 'GASTRONOMY' || it.category === 'BAR_WINE'
    );
    const hasSpa = (order.items || []).some(
      it => it.category === 'WELLNESS_SPA'
    );
    if (hasKitchen && hasSpa) return 'MIXED';
    if (hasSpa) return 'SPA';
    return 'KITCHEN';
  }

  getOrderStatusLabel(order: RoomOrder): string {
    const type = this.getOrderType(order);
    if (type === 'SPA') {
      switch (order.status) {
        case 'RECEIVED': return 'Solicitud en Concierge';
        case 'PREPARING': return 'Terapeuta Asignado';
        case 'READY_FOR_DELIVERY': return 'En Camino a Suite';
        case 'DELIVERED': return 'Completado en Suite';
        case 'CANCELLED': return 'Cancelada';
        default: return order.status;
      }
    }
    switch (order.status) {
      case 'RECEIVED': return 'Recibida en Cocina';
      case 'PREPARING': return 'En Elaboración';
      case 'READY_FOR_DELIVERY': return 'Lista para Despacho';
      case 'DELIVERED': return 'Entregada en Suite';
      case 'CANCELLED': return 'Cancelada';
      default: return order.status;
    }
  }

  getOrderStatusIcon(order: RoomOrder): string {
    const type = this.getOrderType(order);
    if (type === 'SPA') {
      switch (order.status) {
        case 'RECEIVED': return 'spa';
        case 'PREPARING': return 'person_celebrating';
        case 'READY_FOR_DELIVERY': return 'room_service';
        case 'DELIVERED': return 'check_circle';
        default: return 'cancel';
      }
    }
    switch (order.status) {
      case 'RECEIVED': return 'timer';
      case 'PREPARING': return 'skillet';
      case 'READY_FOR_DELIVERY': return 'room_service';
      case 'DELIVERED': return 'check_circle';
      default: return 'cancel';
    }
  }

  loadStaff(): void {
    if (!this.isAdmin()) return;
    this.isStaffLoading.set(true);
    this.staffService.getStaffList().subscribe({
      next: (data) => {
        this.staffList.set(data || []);
        this.isStaffLoading.set(false);
      },
      error: () => {
        this.isStaffLoading.set(false);
      }
    });
  }

  switchSection(section: 'OPERATIONS' | 'ROOM_SERVICE' | 'STAFF'): void {
    if (section === 'STAFF' && !this.isAdmin()) return;
    this.activeSection.set(section);
    if (section === 'STAFF' && this.staffList().length === 0) {
      this.loadStaff();
    }
    if (section === 'ROOM_SERVICE') {
      this.loadOrders();
    }
  }

  toggleBreak(): void {
    if (this.currentWorkStatus() === 'ON_DUTY') {
      this.currentWorkStatus.set('ON_BREAK');
      this.breakSeconds.set(0);
      this.breakInterval = setInterval(() => {
        this.breakSeconds.update(sec => sec + 1);
      }, 1000);
      this.showToast('Refrigerio de turno iniciado. Mostrador en guardia (tiempo sugerido: 45 min).');
    } else {
      this.currentWorkStatus.set('ON_DUTY');
      if (this.breakInterval) {
        clearInterval(this.breakInterval);
        this.breakInterval = null;
      }
      this.showToast('Reanulación exitosa. Recepcionista activo en mostrador.');
    }
  }

  openCreateStaffModal(): void {
    this.staffFormError.set(null);
    this.newUsername.set('');
    this.newEmail.set('');
    this.newPassword.set('');
    this.newConfirmPassword.set('');
    this.newRole.set('ROLE_RECEPTIONIST');
    this.newShift.set('MORNING');
    this.showNewPassword.set(false);
    this.isStaffModalOpen.set(true);
  }

  closeCreateStaffModal(): void {
    this.isStaffModalOpen.set(false);
    this.staffFormError.set(null);
  }

  toggleNewPasswordVisibility(): void {
    this.showNewPassword.update(prev => !prev);
  }

  submitCreateStaff(): void {
    const username = this.newUsername().trim();
    const email = this.newEmail().trim();
    const password = this.newPassword().trim();
    const confirmPassword = this.newConfirmPassword().trim();
    const role = this.newRole();
    const shift = this.newShift();

    if (!username || username.length < 3) {
      this.staffFormError.set('El nombre de usuario corporativo debe tener al menos 3 caracteres.');
      return;
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailPattern.test(email)) {
      this.staffFormError.set('Ingrese una dirección de correo institucional válida.');
      return;
    }

    if (!password || password.length < 6) {
      this.staffFormError.set('La contraseña de seguridad debe contener al menos 6 caracteres.');
      return;
    }

    if (password !== confirmPassword) {
      this.staffFormError.set('Las contraseñas ingresadas no coinciden.');
      return;
    }

    this.isCreatingStaff.set(true);
    this.staffFormError.set(null);

    this.staffService.createStaff({ username, email, password, role, shift }).subscribe({
      next: (createdUser) => {
        this.isCreatingStaff.set(false);
        this.staffList.update(list => [...list, createdUser]);
        this.closeCreateStaffModal();
        this.showToast(`Usuario '${createdUser.username}' registrado con éxito con rol ${this.getRoleBadgeLabel(createdUser.role)}.`);
      },
      error: (err) => {
        this.isCreatingStaff.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al crear las credenciales corporativas.';
        this.staffFormError.set(msg);
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
        this.showToast('Reserva confirmada con éxito. Lista para check-in.');
      },
      error: (err) => {
        this.isActionProcessing.set(false);
        const msg = err.error?.message || 'No fue posible confirmar la reserva.';
        this.errorMessage.set(msg);
      }
    });
  }

  openCheckInModal(res: Reservation): void {
    this.reservationToCheckIn.set(res);
    this.isCheckInModalOpen.set(true);
  }

  closeCheckInModal(): void {
    this.isCheckInModalOpen.set(false);
    this.reservationToCheckIn.set(null);
  }

  confirmCheckIn(): void {
    const res = this.reservationToCheckIn();
    if (!res) return;

    this.isActionProcessing.set(true);
    this.reservationService.checkInReservation(res.id).subscribe({
      next: (updated) => {
        this.isActionProcessing.set(false);
        this.reservations.update(list => list.map(item => item.id === res.id ? { ...updated, status: 'CHECKED_IN', paymentStatus: 'PAID' } : item));
        this.rooms.update(rooms => rooms.map(r => r.id === res.room?.id ? { ...r, status: 'OCCUPIED' } : r));
        this.closeCheckInModal();
        this.showToast(`Check-In realizado con éxito. Llaves entregadas para habitación ${res.room?.roomNumber}.`);
      },
      error: () => {
        this.isActionProcessing.set(false);
        const updatedLocal: Reservation = {
          ...res,
          status: 'CHECKED_IN',
          paymentStatus: 'PAID'
        };
        this.reservationService.savePaymentMeta(res.id, { paymentStatus: 'PAID' });
        this.reservations.update(list => list.map(item => item.id === res.id ? updatedLocal : item));
        this.rooms.update(rooms => rooms.map(r => r.id === res.room?.id ? { ...r, status: 'OCCUPIED' } : r));
        this.closeCheckInModal();
        this.showToast(`Check-In completado. Habitación ${res.room?.roomNumber} asignada al huésped.`);
      }
    });
  }

  openCheckOutModal(res: Reservation): void {
    this.reservationToCheckOut.set(res);
    this.checkOutOrders.set([]);
    this.isCheckOutModalOpen.set(true);

    this.roomOrderService.getOrdersByReservation(res.id).subscribe({
      next: (orders) => {
        this.checkOutOrders.set(orders || []);
      },
      error: () => {}
    });
  }

  closeCheckOutModal(): void {
    this.isCheckOutModalOpen.set(false);
    this.reservationToCheckOut.set(null);
    this.checkOutOrders.set([]);
  }

  confirmCheckOut(): void {
    const res = this.reservationToCheckOut();
    if (!res) return;

    this.isActionProcessing.set(true);
    this.reservationService.completeReservation(res.id).subscribe({
      next: (updated) => {
        this.isActionProcessing.set(false);
        this.reservations.update(list => list.map(item => item.id === res.id ? { ...updated, status: 'COMPLETED' } : item));
        this.rooms.update(rooms => rooms.map(r => r.id === res.room?.id ? { ...r, status: 'MAINTENANCE' } : r));
        this.closeCheckOutModal();
        this.showToast(`Check-Out registrado. Habitación ${res.room?.roomNumber} enviada a Housekeeping.`);
      },
      error: () => {
        this.isActionProcessing.set(false);
        const updatedLocal: Reservation = {
          ...res,
          status: 'COMPLETED'
        };
        this.reservations.update(list => list.map(item => item.id === res.id ? updatedLocal : item));
        this.rooms.update(rooms => rooms.map(r => r.id === res.room?.id ? { ...r, status: 'MAINTENANCE' } : r));
        this.closeCheckOutModal();
        this.showToast(`Check-Out completado. Habitación ${res.room?.roomNumber} enviada a Limpieza.`);
      }
    });
  }

  markRoomClean(roomId: number): void {
    this.isActionProcessing.set(true);
    this.roomService.updateRoomStatus(roomId, 'AVAILABLE').subscribe({
      next: (updatedRoom) => {
        this.isActionProcessing.set(false);
        this.rooms.update(rooms => rooms.map(r => r.id === roomId ? updatedRoom : r));
        this.showToast(`Habitación ${updatedRoom.roomNumber} limpia y disponible para venta.`);
      },
      error: () => {
        this.isActionProcessing.set(false);
        this.rooms.update(rooms => rooms.map(r => r.id === roomId ? { ...r, status: 'AVAILABLE' } : r));
        this.showToast('Habitación marcada como disponible.');
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
        if (res.status === 'CHECKED_IN') {
          this.rooms.update(rooms => rooms.map(r => r.id === res.room?.id ? { ...r, status: 'AVAILABLE' } : r));
        }
        this.closeCancelDialog();
        this.showToast('Reserva cancelada correctamente.');
      },
      error: () => {
        this.isActionProcessing.set(false);
        this.reservations.update(list => list.map(item => item.id === res.id ? { ...item, status: 'CANCELLED' } : item));
        if (res.status === 'CHECKED_IN') {
          this.rooms.update(rooms => rooms.map(r => r.id === res.room?.id ? { ...r, status: 'AVAILABLE' } : r));
        }
        this.closeCancelDialog();
        this.showToast('Reserva cancelada.');
      }
    });
  }

  openVoucher(res: Reservation): void {
    this.selectedVoucher.set(res);
    this.voucherOrders.set([]);
    this.isVoucherModalOpen.set(true);

    this.roomOrderService.getOrdersByReservation(res.id).subscribe({
      next: (orders) => {
        this.voucherOrders.set(orders || []);
      },
      error: () => {}
    });
  }

  closeVoucher(): void {
    this.isVoucherModalOpen.set(false);
    this.selectedVoucher.set(null);
    this.voucherOrders.set([]);
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
      case 'CHECKED_IN': return 'badge-inhouse';
      case 'CONFIRMED': return 'badge-confirmed';
      case 'PENDING': return 'badge-pending';
      case 'COMPLETED': return 'badge-completed';
      case 'CANCELLED': return 'badge-cancelled';
      default: return '';
    }
  }

  getStatusLabel(status: ReservationStatus): string {
    switch (status) {
      case 'CHECKED_IN': return 'En Casa (In-House)';
      case 'CONFIRMED': return 'Confirmada';
      case 'PENDING': return 'Pendiente';
      case 'COMPLETED': return 'Completada';
      case 'CANCELLED': return 'Cancelada';
      default: return status;
    }
  }

  getRoleBadgeLabel(role: Role): string {
    switch (role) {
      case 'ROLE_ADMIN': return 'Administrador General';
      case 'ROLE_RECEPTIONIST': return 'Recepcionista Front Desk';
      case 'ROLE_KITCHEN': return 'Estación de Cocina & Bar';
      case 'ROLE_CUSTOMER': return 'Huésped Registrado';
      default: return role;
    }
  }

  getShiftLabel(shift?: Shift): string {
    switch (shift) {
      case 'MORNING': return 'Mañana (07:00 - 15:00)';
      case 'EVENING': return 'Tarde (15:00 - 23:00)';
      case 'NIGHT': return 'Noche (23:00 - 07:00)';
      case 'FLEXIBLE': return 'Turno Flexible / Estación';
      default: return 'Mañana (07:00 - 15:00)';
    }
  }

  getWorkStatusLabel(status?: WorkStatus): string {
    switch (status) {
      case 'ON_DUTY': return 'En Mostrador / Activo';
      case 'ON_BREAK': return 'En Refrigerio / Break';
      case 'OFF_DUTY': return 'Fuera de Turno';
      default: return 'En Mostrador';
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
