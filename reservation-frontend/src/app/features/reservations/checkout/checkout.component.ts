import { Component, OnInit, AfterViewInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { RoomService } from '../../../core/services/room.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';
import { I18nService } from '../../../core/services/i18n.service';
import { PaymentService } from '../../../core/services/payment.service';
import { Room } from '../../../core/models/room.model';
import { Reservation, ReservationRequest, PaymentMethod } from '../../../core/models/reservation.model';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss'
})
export class CheckoutComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly roomService = inject(RoomService);
  private readonly reservationService = inject(ReservationService);
  private readonly paymentService = inject(PaymentService);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);

  readonly room = signal<Room | null>(null);
  readonly checkInDate = signal<string>('');
  readonly checkOutDate = signal<string>('');
  readonly guestsCount = signal<number>(2);
  readonly specialRequests = signal<string>('');

  readonly guestName = signal<string>('');
  readonly guestEmail = signal<string>('');
  readonly guestPhone = signal<string>('+52 55 1234 5678');

  readonly paymentMethod = signal<PaymentMethod>('CREDIT_CARD');
  readonly cardHolder = signal<string>('');
  readonly cardBrand = signal<string>('VISA');
  readonly cardLast4 = signal<string>('4242');

  readonly isAuthorizing = signal<boolean>(false);
  readonly authStepMessage = signal<string>('');
  readonly authCode = signal<string>('');

  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isSuccessModalOpen = signal<boolean>(false);
  readonly confirmedReservation = signal<Reservation | null>(null);

  private readonly roomImages: Record<number, string> = {
    1: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
    2: 'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=1200&q=80',
    3: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1200&q=80',
    4: 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80',
    5: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
    6: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80'
  };

  readonly minCheckInDate = computed(() => {
    return this.formatDate(new Date(Date.now() + 86400000));
  });

  readonly minCheckOutDate = computed(() => {
    const checkIn = new Date(this.checkInDate());
    if (isNaN(checkIn.getTime())) {
      return this.formatDate(new Date(Date.now() + 2 * 86400000));
    }
    const nextDay = new Date(checkIn.getTime() + 86400000);
    return this.formatDate(nextDay);
  });

  readonly nightsCount = computed(() => {
    const start = new Date(this.checkInDate());
    const end = new Date(this.checkOutDate());
    if (isNaN(start.getTime()) || isNaN(end.getTime())) {
      return 1;
    }
    const diffTime = end.getTime() - start.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 1;
  });

  readonly baseRateTotal = computed(() => {
    const rate = this.room()?.roomType?.pricePerNight ?? 380;
    return rate * this.nightsCount();
  });

  readonly serviceFee = computed(() => this.baseRateTotal() * 0.10);
  readonly taxes = computed(() => this.baseRateTotal() * 0.16);
  readonly grandTotal = computed(() => this.baseRateTotal() + this.serviceFee() + this.taxes());

  readonly maskedCardNumber = computed<string>(() => {
    return `•••• •••• •••• ${this.cardLast4()}`;
  });

  readonly displayExpiry = computed<string>(() => {
    return '12/28';
  });

  readonly displayCardHolder = computed<string>(() => {
    const name = this.cardHolder().trim() || this.guestName().trim();
    const fallback = this.i18n.currentLang() === 'en' ? 'CARDHOLDER NAME' : 'TITULAR DE LA TARJETA';
    return (name || fallback).toUpperCase();
  });

  readonly displayCheckInFormatted = computed<string>(() => {
    return this.formatDatePretty(this.checkInDate());
  });

  readonly displayCheckOutFormatted = computed<string>(() => {
    return this.formatDatePretty(this.checkOutDate());
  });

  private formatDatePretty(dateStr: string): string {
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

  private formatDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.guestName.set(user.username);
      this.guestEmail.set(user.email);
      this.cardHolder.set(user.username.toUpperCase());
    }

    const tomorrow = new Date(Date.now() + 86400000);
    const tomorrowStr = this.formatDate(tomorrow);
    const defaultOut = new Date(Date.now() + 5 * 86400000);
    const defaultOutStr = this.formatDate(defaultOut);

    this.route.queryParams.subscribe(params => {
      const roomIdParam = params['roomId'];
      const checkInParam = params['checkIn'];
      const checkOutParam = params['checkOut'];
      const guestsParam = params['guests'];

      if (checkInParam && checkInParam >= tomorrowStr) {
        this.checkInDate.set(checkInParam);
      } else {
        this.checkInDate.set(tomorrowStr);
      }

      if (checkOutParam && checkOutParam > this.checkInDate()) {
        this.checkOutDate.set(checkOutParam);
      } else {
        this.checkOutDate.set(defaultOutStr);
      }

      if (guestsParam) {
        this.guestsCount.set(+guestsParam);
      }

      const roomId = roomIdParam ? +roomIdParam : 1;
      this.loadRoomDetails(roomId);
    });
  }

  ngAfterViewInit(): void {
    if (this.paymentMethod() === 'CREDIT_CARD') {
      setTimeout(() => {
        this.mountStripeElement();
      }, 100);
    }
  }

  private mountStripeElement(): void {
    this.paymentService.mountCardElement('stripe-card-element', (event: any) => {
      if (event?.brand && event.brand !== 'unknown') {
        this.cardBrand.set(event.brand.toUpperCase());
      }
      if (event?.error) {
        this.errorMessage.set(event.error.message);
      } else {
        this.errorMessage.set(null);
      }
    });
  }

  ngOnDestroy(): void {
    this.paymentService.destroyCardElement();
  }

  openDatePicker(inputElement: HTMLInputElement): void {
    if (inputElement) {
      if (typeof (inputElement as any).showPicker === 'function') {
        try {
          (inputElement as any).showPicker();
          return;
        } catch {}
      }
      inputElement.focus();
      inputElement.click();
    }
  }

  onCheckInChange(newIn: string): void {
    this.errorMessage.set(null);
    this.checkInDate.set(newIn);
    if (this.checkOutDate() <= newIn) {
      const nextDay = new Date(new Date(newIn).getTime() + 86400000);
      this.checkOutDate.set(this.formatDate(nextDay));
    }
  }

  onCheckOutChange(newOut: string): void {
    this.errorMessage.set(null);
    this.checkOutDate.set(newOut);
  }

  loadRoomDetails(roomId: number): void {
    this.roomService.getRoomById(roomId).subscribe({
      next: (data) => {
        if (data) {
          this.room.set(data);
        } else {
          this.errorMessage.set(
            this.i18n.currentLang() === 'en'
              ? 'Selected suite not found. Please choose a valid suite from the catalog.'
              : 'La suite seleccionada no fue encontrada. Por favor seleccione una suite válida del catálogo.'
          );
        }
      },
      error: () => {
        this.errorMessage.set(
          this.i18n.currentLang() === 'en'
            ? 'Error loading suite information. Please try again.'
            : 'Error al cargar la información de la suite. Por favor intente nuevamente.'
        );
      }
    });
  }

  getRoomImageUrl(): string {
    const r = this.room();
    if (!r) return this.roomImages[1];
    return this.roomImages[r.id] ||
      this.roomImages[((r.id - 1) % 6) + 1] ||
      this.roomImages[1];
  }

  addSuggestionTag(text: string): void {
    const current = this.specialRequests().trim();
    if (current.length > 0) {
      this.specialRequests.set(`${current} • ${text}`);
    } else {
      this.specialRequests.set(text);
    }
  }

  setPaymentMethod(method: PaymentMethod): void {
    this.paymentMethod.set(method);
    this.errorMessage.set(null);

    if (method === 'CREDIT_CARD') {
      setTimeout(() => {
        this.mountStripeElement();
      }, 100);
    } else {
      this.paymentService.destroyCardElement();
    }
  }

  async confirmBooking(): Promise<void> {
    const currentRoom = this.room();
    if (!currentRoom) return;

    this.errorMessage.set(null);

    if (this.paymentMethod() === 'CREDIT_CARD') {
      if (!this.cardHolder().trim()) {
        this.errorMessage.set(
          this.i18n.currentLang() === 'en'
            ? 'Please enter the cardholder name as printed on the card.'
            : 'Ingrese el nombre del titular tal como figura en el plástico.'
        );
        return;
      }

      this.isAuthorizing.set(true);
      this.isSubmitting.set(true);
      this.authStepMessage.set(
        this.i18n.currentLang() === 'en'
          ? 'Connecting to secure bank gateway with 256-bit SSL encryption...'
          : 'Conectando con la pasarela bancaria segura con cifrado SSL 256-Bit...'
      );

      try {
        const result = await this.paymentService.tokenizeCard(this.cardHolder(), this.guestEmail());
        const intent = await firstValueFrom(
          this.paymentService.createPaymentIntent({
            amount: this.baseRateTotal(),
            currency: 'usd',
            description: `Suite ${currentRoom.roomNumber}`,
            guestEmail: this.guestEmail()
          })
        );
        await this.paymentService.confirmCardPayment(intent.clientSecret, result.token);
        this.cardBrand.set(result.brand);
        this.cardLast4.set(result.last4);
        this.authCode.set(result.authCode);
        this.authStepMessage.set(
          this.i18n.currentLang() === 'en'
            ? `Bank authorization approved (${result.authCode})`
            : `Autorización bancaria aprobada (${result.authCode})`
        );

        this.executeReservationCall(currentRoom, result.last4, result.authCode, intent.paymentIntentId, result.brand);
      } catch (err: any) {
        this.isAuthorizing.set(false);
        this.isSubmitting.set(false);
        this.errorMessage.set(
          err?.error?.detail || err?.error?.message || err?.message || 'Error al autorizar el pago con la pasarela bancaria.'
        );
      }
    } else {
      this.isSubmitting.set(true);
      this.executeReservationCall(currentRoom, undefined, 'RECEPTION-GUARANTEE');
    }
  }

  private executeReservationCall(room: Room, cardLast4?: string, authCode?: string, paymentIntentId?: string, cardBrand?: string): void {
    const isOnline = this.paymentMethod() === 'CREDIT_CARD';
    const request: ReservationRequest = {
      roomId: room.id,
      checkInDate: this.checkInDate(),
      checkOutDate: this.checkOutDate(),
      paymentMethod: this.paymentMethod(),
      paymentStatus: isOnline ? 'PAID' : 'PENDING',
      cardLast4: isOnline ? cardLast4 : undefined,
      cardBrand: isOnline ? (cardBrand || this.cardBrand()) : undefined,
      authCode: authCode,
      paymentIntentId: paymentIntentId
    };

    this.reservationService.createReservation(request).subscribe({
      next: (res) => {
        this.isAuthorizing.set(false);
        this.isSubmitting.set(false);
        const enriched: Reservation = {
          ...res,
          paymentMethod: request.paymentMethod,
          paymentStatus: request.paymentStatus,
          cardLast4: request.cardLast4,
          cardBrand: request.cardBrand,
          authCode: request.authCode,
          paymentIntentId: request.paymentIntentId
        };
        this.confirmedReservation.set(enriched);
        this.isSuccessModalOpen.set(true);
      },
      error: (err) => {
        this.isAuthorizing.set(false);
        this.isSubmitting.set(false);
        if (err.status === 409) {
          const detail = err.error?.detail || (
            this.i18n.currentLang() === 'en'
              ? 'The selected suite is already reserved on these dates. Please choose different dates or another suite.'
              : 'La suite seleccionada ya se encuentra reservada en estas fechas. Por favor elija otras fechas u otra suite disponible.'
          );
          this.errorMessage.set(detail);
        } else if (err.error?.errors) {
          const details = Object.entries(err.error.errors)
            .map(([field, msg]) => {
              if (field === 'checkInDate') return 'La fecha de llegada debe ser a partir de mañana';
              if (field === 'checkOutDate') return 'La fecha de salida debe ser posterior a la de llegada';
              if (field === 'roomId') return 'La habitación seleccionada no es válida';
              return `${field}: ${msg}`;
            })
            .join('. ');
          this.errorMessage.set(details);
        } else if (err.error?.detail) {
          this.errorMessage.set(err.error.detail);
        } else {
          this.errorMessage.set(
            this.i18n.currentLang() === 'en'
              ? 'Could not process your reservation due to a connection or server error. Please try again or contact front desk.'
              : 'No pudimos procesar su reservación debido a un inconveniente de conexión con el servidor. Por favor intente nuevamente o contacte a recepción.'
          );
        }
        if (typeof window !== 'undefined') {
          window.scrollTo({ top: 120, behavior: 'smooth' });
        }
      }
    });
  }

  closeSuccessModal(): void {
    this.isSuccessModalOpen.set(false);
    this.router.navigate(['/my-reservations']);
  }
}
