import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReservationService } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';
import { Reservation } from '../../../core/models/reservation.model';

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
  selector: 'app-my-reservations',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-reservations.component.html',
  styleUrl: './my-reservations.component.scss'
})
export class MyReservationsComponent implements OnInit {
  private readonly reservationService = inject(ReservationService);
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly activeTab = signal<'upcoming' | 'history' | 'canceled'>('upcoming');
  readonly reservations = signal<Reservation[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);

  readonly selectedVoucherReservation = signal<Reservation | null>(null);
  readonly isVoucherModalOpen = signal<boolean>(false);

  readonly reservationToCancel = signal<Reservation | null>(null);
  readonly isCancelModalOpen = signal<boolean>(false);
  readonly isCancelling = signal<boolean>(false);

  readonly activeCancelCalculation = computed<RefundCalculation | null>(() => {
    const res = this.reservationToCancel();
    return res ? this.calculateRefund(res) : null;
  });

  readonly cancelResult = signal<RefundCalculation | null>(null);
  readonly isCancelResultModalOpen = signal<boolean>(false);

  readonly isConciergeModalOpen = signal<boolean>(false);
  readonly copiedCodeId = signal<number | null>(null);

  private readonly roomImages: Record<number, string> = {
    1: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
    2: 'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=1200&q=80',
    3: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1200&q=80',
    4: 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80',
    5: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
    6: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80'
  };

  private readonly fallbackReservations: Reservation[] = [
    {
      id: 8841,
      guest: {
        id: 1,
        firstName: 'Juan',
        lastName: 'Pérez',
        email: 'juan@ejemplo.com',
        phone: '+51 987654321'
      },
      room: {
        id: 1,
        roomNumber: '101',
        status: 'AVAILABLE',
        roomType: {
          id: 1,
          name: 'Suite Royal Vista al Mar',
          description: 'Espaciosa suite con ventanales de piso a techo frente al océano, acabados en roble natural y terraza privada con jacuzzi.',
          capacity: 3,
          pricePerNight: 380.00
        }
      },
      checkInDate: '2026-11-18',
      checkOutDate: '2026-11-23',
      status: 'CONFIRMED',
      totalPrice: 2394.00,
      createdAt: new Date().toISOString(),
      paymentMethod: 'CREDIT_CARD',
      paymentStatus: 'PAID',
      cardLast4: '4242',
      cardBrand: 'VISA',
      authCode: 'AUTH-901844'
    },
    {
      id: 7102,
      guest: {
        id: 1,
        firstName: 'Juan',
        lastName: 'Pérez',
        email: 'juan@ejemplo.com',
        phone: '+51 987654321'
      },
      room: {
        id: 4,
        roomNumber: '401',
        status: 'AVAILABLE',
        roomType: {
          id: 4,
          name: 'Villa Serena con Alberca Privada',
          description: 'Refugio idílico rodeado de palmeras y vegetación tropical, máxima privacidad con alberca climatizada de agua salada.',
          capacity: 4,
          pricePerNight: 540.00
        }
      },
      checkInDate: '2026-06-12',
      checkOutDate: '2026-06-16',
      status: 'COMPLETED',
      totalPrice: 2160.00,
      createdAt: '2026-06-01T10:00:00',
      paymentMethod: 'CREDIT_CARD',
      paymentStatus: 'PAID',
      cardLast4: '8812',
      cardBrand: 'MASTERCARD',
      authCode: 'AUTH-520199'
    }
  ];

  readonly upcomingReservations = computed(() => {
    return this.reservations().filter(r => r.status === 'PENDING' || r.status === 'CONFIRMED' || r.status === 'CHECKED_IN');
  });

  readonly historyReservations = computed(() => {
    return this.reservations().filter(r => r.status === 'COMPLETED');
  });

  readonly canceledReservations = computed(() => {
    return this.reservations().filter(r => r.status === 'CANCELLED');
  });

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.reservationService.getReservations().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.reservations.set(data);
        } else {
          this.reservations.set(this.fallbackReservations);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.reservations.set(this.fallbackReservations);
        this.isLoading.set(false);
      }
    });
  }

  switchTab(tab: 'upcoming' | 'history' | 'canceled'): void {
    this.activeTab.set(tab);
  }

  getRoomImage(room: any): string {
    if (!room) return this.roomImages[1];
    return this.roomImages[room.id] ||
      this.roomImages[((room.id - 1) % 6) + 1] ||
      this.roomImages[1];
  }

  calculateNights(checkIn: string, checkOut: string): number {
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    if (isNaN(start.getTime()) || isNaN(end.getTime())) return 1;
    const diffDays = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 1;
  }

  calculateRefund(res: Reservation): RefundCalculation {
    const checkInDateTime = new Date(`${res.checkInDate}T15:00:00`);
    const now = new Date();
    const diffMs = checkInDateTime.getTime() - now.getTime();
    const hoursUntilCheckIn = Math.round(diffMs / (1000 * 60 * 60));
    const isEligibleForFullRefund = hoursUntilCheckIn >= 48;

    if (res.paymentMethod === 'CREDIT_CARD') {
      if (isEligibleForFullRefund) {
        return {
          hoursUntilCheckIn,
          isEligibleForFullRefund: true,
          refundAmount: res.totalPrice,
          penaltyAmount: 0,
          policyMessage: `Reembolso del 100% procesado a su tarjeta •••• ${res.cardLast4 || '4242'}.`,
          badgeLabel: 'Reembolso 100% Aprobado',
          badgeType: 'full'
        };
      } else {
        const penalty = res.room?.roomType?.pricePerNight || (res.totalPrice / Math.max(1, this.calculateNights(res.checkInDate, res.checkOutDate)));
        const refund = Math.max(0, res.totalPrice - penalty);
        return {
          hoursUntilCheckIn,
          isEligibleForFullRefund: false,
          refundAmount: refund,
          penaltyAmount: penalty,
          policyMessage: `Cancelación tardía: Se retiene la primera noche por política de hotel boutique y se reembolsa el saldo restante.`,
          badgeLabel: 'Cancelación Tardía (<48h)',
          badgeType: 'partial'
        };
      }
    } else {
      return {
        hoursUntilCheckIn,
        isEligibleForFullRefund: true,
        refundAmount: 0,
        penaltyAmount: 0,
        policyMessage: 'Cancelación flexible sin cargos: No se efectuó ningún cobro.',
        badgeLabel: 'Sin Cargos Realizados',
        badgeType: 'free'
      };
    }
  }

  copyBookingCode(code: string, id: number): void {
    navigator.clipboard.writeText(code).then(() => {
      this.copiedCodeId.set(id);
      setTimeout(() => {
        if (this.copiedCodeId() === id) {
          this.copiedCodeId.set(null);
        }
      }, 2000);
    });
  }

  openVoucher(res: Reservation): void {
    this.selectedVoucherReservation.set(res);
    this.isVoucherModalOpen.set(true);
  }

  closeVoucher(): void {
    this.isVoucherModalOpen.set(false);
    this.selectedVoucherReservation.set(null);
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

    const calc = this.calculateRefund(res);
    this.isCancelling.set(true);

    const newPaymentStatus = res.paymentMethod === 'CREDIT_CARD'
      ? (calc.isEligibleForFullRefund ? 'REFUNDED' : 'PARTIALLY_REFUNDED')
      : 'PENDING';

    this.reservationService.cancelReservation(res.id).subscribe({
      next: () => {
        this.isCancelling.set(false);
        this.applyCancellationLocally(res.id, newPaymentStatus, calc);
      },
      error: () => {
        this.isCancelling.set(false);
        this.applyCancellationLocally(res.id, newPaymentStatus, calc);
      }
    });
  }

  private applyCancellationLocally(id: number, paymentStatus: any, calc: RefundCalculation): void {
    this.reservationService.savePaymentMeta(id, {
      paymentStatus: paymentStatus,
      refundAmount: calc.refundAmount,
      penaltyAmount: calc.penaltyAmount
    });

    this.reservations.update(list =>
      list.map(item => item.id === id ? {
        ...item,
        status: 'CANCELLED',
        paymentStatus: paymentStatus,
        refundAmount: calc.refundAmount,
        penaltyAmount: calc.penaltyAmount
      } : item)
    );

    this.closeCancelDialog();
    this.cancelResult.set(calc);
    this.isCancelResultModalOpen.set(true);
  }

  closeCancelResultModal(): void {
    this.isCancelResultModalOpen.set(false);
    this.cancelResult.set(null);
    this.activeTab.set('canceled');
  }

  openConcierge(): void {
    this.isConciergeModalOpen.set(true);
  }

  closeConcierge(): void {
    this.isConciergeModalOpen.set(false);
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'CHECKED_IN':
        return 'En Casa (Check-In Realizado)';
      case 'CONFIRMED':
        return 'Reserva Confirmada';
      case 'PENDING':
        return 'Pendiente de Confirmación';
      case 'COMPLETED':
        return 'Estancia Completada';
      case 'CANCELLED':
        return 'Reserva Cancelada';
      default:
        return status;
    }
  }

  printHistory(): void {
    window.print();
  }

  printVoucher(): void {
    window.print();
  }
}
