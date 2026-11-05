import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ReservationService } from '../../../core/services/reservation.service';
import { ReviewService } from '../../../core/services/review.service';
import { ConciergeChatService } from '../../../core/services/concierge-chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { RoomOrderService } from '../../../core/services/room-order.service';
import { SseService } from '../../../core/services/sse.service';
import { I18nService } from '../../../core/services/i18n.service';
import { Reservation } from '../../../core/models/reservation.model';
import { ExtraService, RoomOrder } from '../../../core/models/room-order.model';
import { GuestVoucherModalComponent } from './components/guest-voucher-modal/guest-voucher-modal.component';
import { CancellationModalComponent, RefundCalculation } from './components/cancellation-modal/cancellation-modal.component';
import { GuestRoomServiceModalComponent, RoomOrderSubmitPayload } from './components/guest-room-service-modal/guest-room-service-modal.component';
import { HotelFolioModalComponent } from '../../../shared/components/hotel-folio-modal/hotel-folio-modal.component';

export type { RefundCalculation };

@Component({
  selector: 'app-my-reservations',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    GuestVoucherModalComponent,
    CancellationModalComponent,
    GuestRoomServiceModalComponent,
    HotelFolioModalComponent
  ],
  templateUrl: './my-reservations.component.html',
  styleUrl: './my-reservations.component.scss'
})
export class MyReservationsComponent implements OnInit, OnDestroy {
  private readonly reservationService = inject(ReservationService);
  private readonly roomOrderService = inject(RoomOrderService);
  readonly authService = inject(AuthService);
  readonly sseService = inject(SseService);
  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);
  private readonly reviewService = inject(ReviewService);
  readonly conciergeChatService = inject(ConciergeChatService);

  readonly reviewedReservationIds = signal<Set<number>>(new Set());
  readonly isReviewModalOpen = signal<boolean>(false);
  readonly selectedReviewReservation = signal<Reservation | null>(null);
  readonly reviewRating = signal<number>(5);
  readonly reviewCleanliness = signal<number>(5);
  readonly reviewServiceRating = signal<number>(5);
  readonly reviewComfort = signal<number>(5);
  readonly reviewLocation = signal<number>(5);
  readonly reviewTitle = signal<string>('');
  readonly reviewComment = signal<string>('');
  readonly isSubmittingReview = signal<boolean>(false);
  readonly reviewSuccessMessage = signal<string>('');
  readonly reviewErrorMessage = signal<string>('');

  private sseSub: Subscription | null = null;

  readonly activeTab = signal<'upcoming' | 'history' | 'canceled'>('upcoming');
  readonly reservations = signal<Reservation[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);

  readonly selectedVoucherReservation = signal<Reservation | null>(null);
  readonly isVoucherModalOpen = signal<boolean>(false);
  readonly selectedFolioReservation = signal<Reservation | null>(null);
  readonly isFolioModalOpen = signal<boolean>(false);

  readonly reservationToCancel = signal<Reservation | null>(null);
  readonly isCancelModalOpen = signal<boolean>(false);
  readonly isCancelling = signal<boolean>(false);

  readonly isRoomServiceModalOpen = signal<boolean>(false);
  readonly activeCheckedInReservation = signal<Reservation | null>(null);
  readonly extraServices = signal<ExtraService[]>([]);
  readonly isSubmittingOrder = signal<boolean>(false);
  readonly activeReservationOrders = signal<RoomOrder[]>([]);
  readonly orderSuccessMessage = signal<string | null>(null);

  readonly activeCancelCalculation = computed<RefundCalculation | null>(() => {
    const res = this.reservationToCancel();
    return res ? this.calculateRefund(res) : null;
  });

  readonly cancelResult = signal<RefundCalculation | null>(null);
  readonly isCancelResultModalOpen = signal<boolean>(false);

  readonly isConciergeModalOpen = signal<boolean>(false);
  readonly copiedCodeId = signal<number | null>(null);

  readonly selectedPreCheckInReservation = signal<Reservation | null>(null);
  readonly isPreCheckInModalOpen = signal<boolean>(false);
  readonly isSubmittingPreCheckIn = signal<boolean>(false);
  readonly preCheckInDocumentType = signal<string>('PASSPORT');
  readonly preCheckInDocumentNumber = signal<string>('');
  readonly preCheckInNationality = signal<string>('Española');
  readonly preCheckInArrivalTime = signal<string>('14:00 - 16:00');
  readonly preCheckInSpecialRequests = signal<string>('');
  readonly preCheckInError = signal<string | null>(null);

  readonly selectedExpressCheckoutReservation = signal<Reservation | null>(null);
  readonly isExpressCheckoutModalOpen = signal<boolean>(false);
  readonly isSubmittingExpressCheckout = signal<boolean>(false);
  readonly expressCheckoutOrders = signal<RoomOrder[]>([]);
  readonly expressCheckoutSuccess = signal<boolean>(false);
  readonly expressCheckoutError = signal<string | null>(null);
  readonly expressPaymentMethod = signal<'SAVED_CARD' | 'NEW_CARD'>('SAVED_CARD');
  readonly expressCardNumber = signal<string>('');
  readonly expressCardExp = signal<string>('');
  readonly expressCardCvc = signal<string>('');
  readonly expressKeyDropped = signal<boolean>(false);
  readonly expressNotes = signal<string>('');
  readonly expressReceipt = signal<{ authCode: string; amountPaid: number; settledAt: string; roomNumber: string; guestName: string } | null>(null);

  readonly expressOrdersTotal = computed(() => {
    return this.expressCheckoutOrders().reduce((sum, o) => sum + (Number(o.totalAmount) || 0), 0);
  });

  readonly expressPendingBalance = computed(() => {
    const res = this.selectedExpressCheckoutReservation();
    if (!res) return 0;
    const extras = this.expressOrdersTotal();
    if (res.paymentMethod === 'CREDIT_CARD') {
      return extras;
    }
    return (Number(res.totalPrice) || 0) + extras;
  });

  readonly userRoleLabel = computed<string>(() => {
    const role = this.authService.currentUser()?.role;
    if (role === 'ROLE_CUSTOMER') {
      return this.i18n.t('myReservations.guestTierBadge');
    }
    if (role === 'ROLE_ADMIN') {
      return this.i18n.t('nav.admin');
    }
    if (role === 'ROLE_RECEPTIONIST') {
      return this.i18n.t('nav.receptionist');
    }
    if (role === 'ROLE_KITCHEN') {
      return this.i18n.currentLang() === 'en' ? 'Culinary Staff' : 'Personal de Cocina';
    }
    return this.i18n.t('myReservations.guestTierBadge');
  });

  private readonly roomImages: Record<number, string> = {
    1: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
    2: 'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=1200&q=80',
    3: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1200&q=80',
    4: 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80',
    5: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
    6: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80'
  };

  readonly isStaffUser = computed(() => {
    const role = this.authService.userRole();
    return role === 'ROLE_ADMIN' || role === 'ROLE_RECEPTIONIST' || role === 'ROLE_KITCHEN';
  });

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

    this.sseService.connect();
    this.sseSub = this.sseService.events$.subscribe(event => {
      if (
        event.eventType === 'RESERVATION_CREATED' ||
        event.eventType === 'RESERVATION_STATUS_CHANGED'
      ) {
        this.loadReservations();
      }

      if (
        event.eventType === 'ORDER_CREATED' ||
        event.eventType === 'ORDER_STATUS_CHANGED' ||
        event.eventType === 'ORDER_THERAPIST_ASSIGNED'
      ) {
        const checkedIn = this.activeCheckedInReservation();
        if (checkedIn && this.isRoomServiceModalOpen()) {
          this.roomOrderService.getOrdersByReservation(checkedIn.id).subscribe({
            next: (orders) => {
              this.activeReservationOrders.set(orders || []);
            }
          });
        }
      }
    });
  }

  ngOnDestroy(): void {
    if (this.sseSub) {
      this.sseSub.unsubscribe();
    }
  }

  loadReservations(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.reservationService.getMyReservations().subscribe({
      next: (data) => {
        const list = data || [];
        this.reservations.set(list);
        this.isLoading.set(false);
        const completed = list.filter(r => r.status === 'COMPLETED');
        this.checkReviewedStatus(completed);
      },
      error: () => {
        this.reservations.set([]);
        this.isLoading.set(false);
      }
    });
  }

  private checkReviewedStatus(completed: Reservation[]): void {
    completed.forEach(res => {
      this.reviewService.getReviewByReservation(res.id).subscribe({
        next: (review) => {
          if (review) {
            this.reviewedReservationIds.update(set => {
              const next = new Set(set);
              next.add(res.id);
              return next;
            });
          }
        }
      });
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

  formatDatePretty(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const parts = dateStr.split('-');
      if (parts.length !== 3) return dateStr;
      const year = +parts[0];
      const month = +parts[1];
      const day = +parts[2];
      const d = new Date(year, month - 1, day);
      if (isNaN(d.getTime())) return dateStr;

      const isEn = this.i18n.currentLang() === 'en';
      if (isEn) {
        const weekday = d.toLocaleDateString('en-US', { weekday: 'short' });
        const monthName = d.toLocaleDateString('en-US', { month: 'short' });
        return `${weekday}, ${monthName} ${day}, ${year}`;
      } else {
        const weekday = d.toLocaleDateString('es-ES', { weekday: 'short' });
        const monthName = d.toLocaleDateString('es-ES', { month: 'short' });
        const capitalizedWeekday = weekday.charAt(0).toUpperCase() + weekday.slice(1).replace('.', '');
        const capitalizedMonth = monthName.charAt(0).toUpperCase() + monthName.slice(1).replace('.', '');
        return `${capitalizedWeekday}, ${day} ${capitalizedMonth} ${year}`;
      }
    } catch {
      return dateStr;
    }
  }

  openVoucher(res: Reservation): void {
    this.selectedVoucherReservation.set(res);
    this.isVoucherModalOpen.set(true);
  }

  closeVoucher(): void {
    this.isVoucherModalOpen.set(false);
    this.selectedVoucherReservation.set(null);
  }

  openFolio(res: Reservation): void {
    this.selectedFolioReservation.set(res);
    this.isFolioModalOpen.set(true);
  }

  closeFolio(): void {
    this.isFolioModalOpen.set(false);
    this.selectedFolioReservation.set(null);
  }

  hasReviewed(reservationId: number): boolean {
    return this.reviewedReservationIds().has(reservationId);
  }

  openReviewModal(res: Reservation): void {
    this.selectedReviewReservation.set(res);
    this.reviewRating.set(5);
    this.reviewCleanliness.set(5);
    this.reviewServiceRating.set(5);
    this.reviewComfort.set(5);
    this.reviewLocation.set(5);
    this.reviewTitle.set('');
    this.reviewComment.set('');
    this.reviewSuccessMessage.set('');
    this.reviewErrorMessage.set('');
    this.isReviewModalOpen.set(true);
  }

  closeReviewModal(): void {
    this.isReviewModalOpen.set(false);
    this.selectedReviewReservation.set(null);
  }

  setReviewRating(val: number): void {
    this.reviewRating.set(val);
  }

  submitGuestReview(): void {
    const res = this.selectedReviewReservation();
    if (!res) return;
    const comment = this.reviewComment().trim();
    if (comment.length < 5) {
      this.reviewErrorMessage.set(this.i18n.currentLang() === 'en' ? 'Please write a review with at least 5 characters.' : 'Por favor escriba una opinión de al menos 5 caracteres.');
      return;
    }

    this.isSubmittingReview.set(true);
    this.reviewErrorMessage.set('');

    this.reviewService.createReview({
      reservationId: res.id,
      rating: this.reviewRating(),
      cleanlinessRating: this.reviewCleanliness(),
      serviceRating: this.reviewServiceRating(),
      comfortRating: this.reviewComfort(),
      locationRating: this.reviewLocation(),
      title: this.reviewTitle().trim() || undefined,
      comment: comment
    }).subscribe({
      next: () => {
        this.isSubmittingReview.set(false);
        this.reviewedReservationIds.update(set => {
          const next = new Set(set);
          next.add(res.id);
          return next;
        });
        this.reviewSuccessMessage.set(this.i18n.t('myReservations.reviewSuccess'));
        setTimeout(() => {
          this.closeReviewModal();
        }, 1500);
      },
      error: (err) => {
        this.isSubmittingReview.set(false);
        const msg = err?.error?.message || (this.i18n.currentLang() === 'en' ? 'Unable to submit review. Please try again.' : 'No fue posible registrar la reseña. Intente nuevamente.');
        this.reviewErrorMessage.set(msg);
      }
    });
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

  openRoomServiceModal(res: Reservation): void {
    this.activeCheckedInReservation.set(res);
    this.orderSuccessMessage.set(null);
    this.isRoomServiceModalOpen.set(true);

    this.roomOrderService.getExtraServices().subscribe({
      next: (data) => {
        this.extraServices.set(data || []);
      },
      error: () => {}
    });

    this.roomOrderService.getOrdersByReservation(res.id).subscribe({
      next: (orders) => {
        this.activeReservationOrders.set(orders || []);
      },
      error: () => {}
    });
  }

  closeRoomServiceModal(): void {
    this.isRoomServiceModalOpen.set(false);
    this.activeCheckedInReservation.set(null);
  }

  submitRoomOrder(payload: RoomOrderSubmitPayload): void {
    const res = this.activeCheckedInReservation();
    if (!res) return;

    this.isSubmittingOrder.set(true);
    this.roomOrderService.createOrder({
      reservationId: res.id,
      notes: payload.notes,
      serviceLocation: payload.serviceLocation || 'IN_SUITE',
      scheduledTime: payload.scheduledTime,
      items: payload.items
    }).subscribe({
      next: (createdOrder) => {
        this.isSubmittingOrder.set(false);
        this.activeReservationOrders.update(orders => [createdOrder, ...orders]);
        this.orderSuccessMessage.set(
          this.i18n.currentLang() === 'en'
            ? `Order #${createdOrder.id} successfully placed! It will be delivered to your suite promptly.`
            : `¡Comanda #${createdOrder.id} registrada con éxito! Será atendida de inmediato para su suite.`
        );
        setTimeout(() => {
          this.orderSuccessMessage.set(null);
        }, 5000);
      },
      error: () => {
        this.isSubmittingOrder.set(false);
      }
    });
  }

  calculateRefund(res: Reservation): RefundCalculation {
    const isOnlineCard = res.paymentMethod === 'CREDIT_CARD';
    const total = Number(res.totalPrice) || 0;

    const checkInDateTime = new Date(`${res.checkInDate}T15:00:00`);
    const now = new Date();
    const diffMs = checkInDateTime.getTime() - now.getTime();
    const hoursRemaining = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60)));
    const isEn = this.i18n.currentLang() === 'en';

    if (!isOnlineCard) {
      return {
        hoursUntilCheckIn: hoursRemaining,
        isEligibleForFullRefund: true,
        refundAmount: 0,
        penaltyAmount: 0,
        policyMessage: isEn
          ? 'Flexible cancellation without charges: Your payment method is upon arrival at reception, so no prior debit was made.'
          : 'Cancelación flexible sin cargos: Su modalidad de pago es en recepción, por lo que no se efectuó ningún débito previo.',
        badgeLabel: isEn ? 'Free Cancellation (No Charges)' : 'Cancelación Gratuita (Sin Cargos)',
        badgeType: 'free'
      };
    }

    if (hoursRemaining >= 48) {
      return {
        hoursUntilCheckIn: hoursRemaining,
        isEligibleForFullRefund: true,
        refundAmount: total,
        penaltyAmount: 0,
        policyMessage: isEn
          ? `Full 100% refund processed to your card •••• ${res.cardLast4 || '4242'}. By cancelling more than 48h in advance, you receive the full stay amount.`
          : `Reembolso íntegro del 100% procesado a su tarjeta •••• ${res.cardLast4 || '4242'}. Al cancelar con más de 48h de anticipación, recupera el monto total de su estancia.`,
        badgeLabel: isEn ? '100% Guaranteed Refund' : 'Reembolso del 100% Garantizado',
        badgeType: 'full'
      };
    } else {
      const nights = this.calculateNights(res.checkInDate, res.checkOutDate);
      const pricePerNight = nights > 0 ? total / nights : total;
      const penalty = Math.min(total, Math.round(pricePerNight * 100) / 100);
      const refund = Math.max(0, Math.round((total - penalty) * 100) / 100);

      return {
        hoursUntilCheckIn: hoursRemaining,
        isEligibleForFullRefund: false,
        refundAmount: refund,
        penaltyAmount: penalty,
        policyMessage: isEn
          ? `Late cancellation (${hoursRemaining}h remaining until check-in): Per boutique policy, 1st night ($${penalty.toFixed(2)} USD) is retained and remaining balance ($${refund.toFixed(2)} USD) refunded to your card •••• ${res.cardLast4 || '4242'}.`
          : `Cancelación tardía (quedan ${hoursRemaining}h para el check-in): Por política boutique se retiene el valor de la 1ª noche ($${penalty.toFixed(2)} USD) y se reembolsa el saldo restante ($${refund.toFixed(2)} USD) a su tarjeta •••• ${res.cardLast4 || '4242'}.`,
        badgeLabel: isEn ? '1st Night Retained (Balance Refunded)' : 'Retención de 1ª Noche (Saldo Reembolsado)',
        badgeType: 'partial'
      };
    }
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

    this.reservationService.cancelReservation(res.id).subscribe({
      next: () => {
        this.isCancelling.set(false);
        this.reservations.update(list =>
          list.map(item => item.id === res.id ? { ...item, status: 'CANCELLED' } : item)
        );
        this.closeCancelDialog();
        this.showCancelResult(calc);
      },
      error: () => {
        this.isCancelling.set(false);
        this.reservations.update(list =>
          list.map(item => item.id === res.id ? { ...item, status: 'CANCELLED' } : item)
        );
        this.closeCancelDialog();
        this.showCancelResult(calc);
      }
    });
  }

  showCancelResult(calc: RefundCalculation): void {
    this.cancelResult.set(calc);
    this.isCancelResultModalOpen.set(true);
  }

  closeCancelResultModal(): void {
    this.isCancelResultModalOpen.set(false);
    this.cancelResult.set(null);
    this.activeTab.set('canceled');
  }

  openConcierge(res?: Reservation): void {
    if (res?.status === 'CHECKED_IN' || this.conciergeChatService.hasActiveStay()) {
      this.conciergeChatService.openWidget();
      return;
    }
    this.isConciergeModalOpen.set(true);
  }

  launchLiveChatFromModal(): void {
    this.closeConcierge();
    this.conciergeChatService.openWidget();
  }

  closeConcierge(): void {
    this.isConciergeModalOpen.set(false);
  }

  getStatusLabel(status: string): string {
    const isEn = this.i18n.currentLang() === 'en';
    switch (status) {
      case 'CHECKED_IN':
        return isEn ? 'In-House (Checked-In)' : 'En Casa (Check-In Realizado)';
      case 'CONFIRMED':
        return isEn ? 'Confirmed Reservation' : 'Reserva Confirmada';
      case 'PENDING':
        return isEn ? 'Pending Confirmation' : 'Pendiente de Confirmación';
      case 'COMPLETED':
        return isEn ? 'Completed Stay' : 'Estancia Completada';
      case 'CANCELLED':
        return isEn ? 'Cancelled' : 'Reserva Cancelada';
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

  openPreCheckInModal(reservation: Reservation): void {
    this.selectedPreCheckInReservation.set(reservation);
    this.preCheckInDocumentType.set(reservation.documentType || 'PASSPORT');
    this.preCheckInDocumentNumber.set(reservation.documentNumber || '');
    this.preCheckInNationality.set(reservation.nationality || 'Española');
    this.preCheckInArrivalTime.set(reservation.estimatedArrivalTime || '14:00 - 16:00');
    this.preCheckInSpecialRequests.set(reservation.specialRequests || '');
    this.preCheckInError.set(null);
    this.isPreCheckInModalOpen.set(true);
  }

  closePreCheckInModal(): void {
    this.isPreCheckInModalOpen.set(false);
    this.selectedPreCheckInReservation.set(null);
    this.preCheckInError.set(null);
  }

  submitPreCheckIn(): void {
    const res = this.selectedPreCheckInReservation();
    if (!res) return;

    const docType = this.preCheckInDocumentType().trim();
    const docNum = this.preCheckInDocumentNumber().trim();
    const nationality = this.preCheckInNationality().trim();
    const eta = this.preCheckInArrivalTime().trim();
    const specialRequests = this.preCheckInSpecialRequests().trim();

    if (!docNum) {
      this.preCheckInError.set('Por favor ingrese su número de documento de identidad oficial.');
      return;
    }

    if (!nationality) {
      this.preCheckInError.set('Por favor indique su nacionalidad o país de emisión.');
      return;
    }

    if (!eta) {
      this.preCheckInError.set('Por favor seleccione su rango estimado de llegada.');
      return;
    }

    this.isSubmittingPreCheckIn.set(true);
    this.preCheckInError.set(null);

    this.reservationService.submitPreCheckIn(res.id, {
      documentType: docType,
      documentNumber: docNum,
      nationality: nationality,
      estimatedArrivalTime: eta,
      specialRequests: specialRequests || undefined
    }).subscribe({
      next: (updated) => {
        this.isSubmittingPreCheckIn.set(false);
        this.reservations.update(list => list.map(r => r.id === updated.id ? updated : r));
        this.closePreCheckInModal();
      },
      error: (err) => {
        this.isSubmittingPreCheckIn.set(false);
        const msg = err.error?.message || err.error?.detail || 'No fue posible registrar el Pre-Check-In.';
        this.preCheckInError.set(msg);
      }
    });
  }

  openExpressCheckoutModal(reservation: Reservation): void {
    this.selectedExpressCheckoutReservation.set(reservation);
    this.expressCheckoutOrders.set([]);
    this.expressCheckoutSuccess.set(false);
    this.expressCheckoutError.set(null);
    this.expressKeyDropped.set(false);
    this.expressNotes.set('');
    this.expressPaymentMethod.set(reservation.cardLast4 ? 'SAVED_CARD' : 'NEW_CARD');
    this.expressReceipt.set(null);
    this.isExpressCheckoutModalOpen.set(true);

    this.roomOrderService.getOrdersByReservation(reservation.id).subscribe({
      next: (orders) => {
        this.expressCheckoutOrders.set(orders || []);
      },
      error: () => {}
    });
  }

  closeExpressCheckoutModal(): void {
    const wasSuccess = this.expressCheckoutSuccess();
    this.isExpressCheckoutModalOpen.set(false);
    this.selectedExpressCheckoutReservation.set(null);
    this.expressCheckoutOrders.set([]);
    this.expressCheckoutSuccess.set(false);
    this.expressCheckoutError.set(null);
    this.expressReceipt.set(null);
    if (wasSuccess) {
      this.loadReservations();
    }
  }

  confirmExpressCheckout(): void {
    const res = this.selectedExpressCheckoutReservation();
    if (!res) return;

    if (!this.expressKeyDropped()) {
      this.expressCheckoutError.set('Debe confirmar que ha depositado las llaves físicas en la suite.');
      return;
    }

    const pending = this.expressPendingBalance();
    let cardLast4 = res.cardLast4 || '4242';

    if (pending > 0 && this.expressPaymentMethod() === 'NEW_CARD') {
      const cleanNum = this.expressCardNumber().replace(/\D/g, '');
      if (cleanNum.length < 15) {
        this.expressCheckoutError.set('Por favor ingrese un número de tarjeta válido (mínimo 15 dígitos).');
        return;
      }
      if (!this.expressCardExp().trim() || !this.expressCardCvc().trim()) {
        this.expressCheckoutError.set('Por favor ingrese la fecha de vencimiento y el código CVC.');
        return;
      }
      cardLast4 = cleanNum.slice(-4);
    }

    this.isSubmittingExpressCheckout.set(true);
    this.expressCheckoutError.set(null);

    const payload = {
      amount: pending,
      cardLast4: cardLast4,
      paymentIntentId: `pi_exp_${Date.now().toString().slice(-8)}`,
      notes: this.expressNotes().trim() || 'Check-Out Express Digital completado por el huésped'
    };

    this.reservationService.expressCheckOut(res.id, payload).subscribe({
      next: (updated) => {
        this.isSubmittingExpressCheckout.set(false);
        this.expressCheckoutSuccess.set(true);
        this.expressReceipt.set({
          authCode: updated.authCode || `AUTH-EXP-${res.id}`,
          amountPaid: pending,
          settledAt: updated.settledAt || new Date().toISOString(),
          roomNumber: res.room?.roomNumber || '',
          guestName: `${res.guest?.firstName || ''} ${res.guest?.lastName || ''}`.trim()
        });
        this.reservations.update(list => list.map(r => r.id === updated.id ? { ...updated, status: 'COMPLETED' } : r));
      },
      error: (err) => {
        this.isSubmittingExpressCheckout.set(false);
        const msg = err.error?.message || err.error?.detail || 'No fue posible procesar el Check-Out Express. Intente nuevamente o consulte en recepción.';
        this.expressCheckoutError.set(msg);
      }
    });
  }
}
