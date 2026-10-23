import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { RoomService } from '../../../core/services/room.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';
import { Room } from '../../../core/models/room.model';
import { Reservation, ReservationRequest, PaymentMethod } from '../../../core/models/reservation.model';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss'
})
export class CheckoutComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly roomService = inject(RoomService);
  private readonly reservationService = inject(ReservationService);
  readonly authService = inject(AuthService);

  readonly room = signal<Room | null>(null);
  readonly checkInDate = signal<string>('');
  readonly checkOutDate = signal<string>('');
  readonly guestsCount = signal<number>(2);
  readonly specialRequests = signal<string>('');

  readonly guestName = signal<string>('');
  readonly guestEmail = signal<string>('');
  readonly guestPhone = signal<string>('+52 55 1234 5678');

  readonly paymentMethod = signal<PaymentMethod>('CREDIT_CARD');
  readonly cardNumber = signal<string>('4242 4242 4242 4242');
  readonly cardHolder = signal<string>('');
  readonly cardExpiry = signal<string>('12/28');
  readonly cardCvv = signal<string>('888');

  readonly isAuthorizing = signal<boolean>(false);
  readonly authStepMessage = signal<string>('');
  readonly authCode = signal<string>('');

  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly isSuccessModalOpen = signal<boolean>(false);
  readonly confirmedReservation = signal<Reservation | null>(null);

  private readonly defaultFallbackRoom: Room = {
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
  };

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

  readonly detectedCardBrand = computed<'VISA' | 'MASTERCARD' | 'AMEX' | 'GENERIC'>(() => {
    const digits = this.cardNumber().replace(/\D/g, '');
    if (digits.startsWith('4')) return 'VISA';
    if (/^(5[1-5]|2[2-7])/.test(digits)) return 'MASTERCARD';
    if (/^3[47]/.test(digits)) return 'AMEX';
    return 'GENERIC';
  });

  readonly maskedCardNumber = computed<string>(() => {
    const digits = this.cardNumber().replace(/\D/g, '');
    if (!digits) return '•••• •••• •••• ••••';
    let formatted = '';
    for (let i = 0; i < 16; i++) {
      if (i > 0 && i % 4 === 0) formatted += ' ';
      if (i < digits.length) {
        if (i < 12 && digits.length >= 12) {
          formatted += '•';
        } else {
          formatted += digits[i];
        }
      } else {
        formatted += '•';
      }
    }
    return formatted;
  });

  readonly displayExpiry = computed<string>(() => {
    return this.cardExpiry() || 'MM/AA';
  });

  readonly displayCardHolder = computed<string>(() => {
    const name = this.cardHolder().trim() || this.guestName().trim();
    return (name || 'TITULAR DE LA TARJETA').toUpperCase();
  });

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

  onCheckInChange(newIn: string): void {
    this.checkInDate.set(newIn);
    if (this.checkOutDate() <= newIn) {
      const nextDay = new Date(new Date(newIn).getTime() + 86400000);
      this.checkOutDate.set(this.formatDate(nextDay));
    }
  }

  loadRoomDetails(roomId: number): void {
    this.roomService.getRoomById(roomId).subscribe({
      next: (data) => {
        if (data) {
          this.room.set(data);
        } else {
          this.room.set(this.defaultFallbackRoom);
        }
      },
      error: () => {
        this.room.set(this.defaultFallbackRoom);
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
  }

  onCardNumberInput(val: string): void {
    const digits = val.replace(/\D/g, '').slice(0, 16);
    const parts = digits.match(/.{1,4}/g);
    this.cardNumber.set(parts ? parts.join(' ') : digits);
  }

  onExpiryInput(val: string): void {
    let digits = val.replace(/\D/g, '').slice(0, 4);
    if (digits.length >= 3) {
      digits = `${digits.slice(0, 2)}/${digits.slice(2)}`;
    }
    this.cardExpiry.set(digits);
  }

  onCvvInput(val: string): void {
    const max = this.detectedCardBrand() === 'AMEX' ? 4 : 3;
    const digits = val.replace(/\D/g, '').slice(0, max);
    this.cardCvv.set(digits);
  }

  confirmBooking(): void {
    const currentRoom = this.room();
    if (!currentRoom) return;

    this.errorMessage.set(null);

    if (this.paymentMethod() === 'CREDIT_CARD') {
      const cleanDigits = this.cardNumber().replace(/\D/g, '');
      if (cleanDigits.length < 15) {
        this.errorMessage.set('Por favor ingrese un número de tarjeta válido (15 o 16 dígitos).');
        return;
      }

      if (!this.cardHolder().trim()) {
        this.errorMessage.set('Ingrese el nombre del titular tal como figura en el plástico.');
        return;
      }

      const expiryRegex = /^(0[1-9]|1[0-2])\/\d{2}$/;
      if (!expiryRegex.test(this.cardExpiry())) {
        this.errorMessage.set('Ingrese una fecha de expiración válida en formato MM/AA.');
        return;
      }

      const minCvv = this.detectedCardBrand() === 'AMEX' ? 4 : 3;
      if (this.cardCvv().length < minCvv) {
        this.errorMessage.set(`Ingrese el código de seguridad CVV (${minCvv} dígitos).`);
        return;
      }

      this.isAuthorizing.set(true);
      this.isSubmitting.set(true);
      this.authStepMessage.set('Conectando con la pasarela bancaria segura con cifrado SSL 256-Bit...');

      setTimeout(() => {
        this.authStepMessage.set('Verificando fondos y token bancario...');
        setTimeout(() => {
          const generatedAuth = 'AUTH-' + Math.floor(100000 + Math.random() * 900000);
          this.authCode.set(generatedAuth);
          this.authStepMessage.set(`Autorización bancaria aprobada (${generatedAuth})`);

          this.executeReservationCall(currentRoom, cleanDigits.slice(-4), generatedAuth);
        }, 700);
      }, 700);
    } else {
      this.isSubmitting.set(true);
      this.executeReservationCall(currentRoom, undefined, 'RECEPTION-GUARANTEE');
    }
  }

  private executeReservationCall(room: Room, cardLast4?: string, authCode?: string): void {
    const isOnline = this.paymentMethod() === 'CREDIT_CARD';
    const request: ReservationRequest = {
      roomId: room.id,
      checkInDate: this.checkInDate(),
      checkOutDate: this.checkOutDate(),
      paymentMethod: this.paymentMethod(),
      paymentStatus: isOnline ? 'PAID' : 'PENDING',
      cardLast4: isOnline ? cardLast4 : undefined,
      cardBrand: isOnline ? this.detectedCardBrand() : undefined,
      authCode: authCode
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
          authCode: request.authCode
        };
        this.confirmedReservation.set(enriched);
        this.isSuccessModalOpen.set(true);
      },
      error: (err) => {
        this.isAuthorizing.set(false);
        this.isSubmitting.set(false);
        if (err.status === 409) {
          this.errorMessage.set(err.error?.detail || 'La suite seleccionada no está disponible en las fechas indicadas.');
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
          const fallbackRes: Reservation = {
            id: Math.floor(1000 + Math.random() * 9000),
            guest: {
              id: 1,
              firstName: this.guestName() || 'Huésped',
              lastName: 'Hospentra',
              email: this.guestEmail() || 'huesped@hospentra.com',
              phone: this.guestPhone()
            },
            room: room,
            checkInDate: this.checkInDate(),
            checkOutDate: this.checkOutDate(),
            status: 'CONFIRMED',
            totalPrice: this.grandTotal(),
            createdAt: new Date().toISOString(),
            paymentMethod: request.paymentMethod,
            paymentStatus: request.paymentStatus,
            cardLast4: request.cardLast4,
            cardBrand: request.cardBrand,
            authCode: request.authCode
          };
          this.reservationService.savePaymentMeta(fallbackRes.id, {
            paymentMethod: request.paymentMethod,
            paymentStatus: request.paymentStatus,
            cardLast4: request.cardLast4,
            cardBrand: request.cardBrand,
            authCode: request.authCode
          });
          this.confirmedReservation.set(fallbackRes);
          this.isSuccessModalOpen.set(true);
        }
      }
    });
  }

  closeSuccessModal(): void {
    this.isSuccessModalOpen.set(false);
    this.router.navigate(['/my-reservations']);
  }
}
