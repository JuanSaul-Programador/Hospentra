import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { RoomService } from '../../../core/services/room.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';
import { Room } from '../../../core/models/room.model';
import { Reservation } from '../../../core/models/reservation.model';

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

  confirmBooking(): void {
    const currentRoom = this.room();
    if (!currentRoom) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const request = {
      roomId: currentRoom.id,
      checkInDate: this.checkInDate(),
      checkOutDate: this.checkOutDate()
    };

    this.reservationService.createReservation(request).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.confirmedReservation.set(res);
        this.isSuccessModalOpen.set(true);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        if (err.status === 409) {
          this.errorMessage.set(err.error?.detail || 'La suite seleccionada no está disponible en las fechas indicadas o su cuenta requiere perfil de huésped.');
        } else if (err.error?.errors) {
          const details = Object.entries(err.error.errors)
            .map(([field, msg]) => {
              if (field === 'checkInDate') return 'La fecha de llegada debe ser futura (a partir de mañana)';
              if (field === 'checkOutDate') return 'La fecha de salida debe ser posterior a la fecha de llegada';
              if (field === 'roomId') return 'La habitación seleccionada no es válida';
              return `${field}: ${msg}`;
            })
            .join('. ');
          this.errorMessage.set(details);
        } else if (err.error?.detail) {
          this.errorMessage.set(err.error.detail);
        } else {
          this.errorMessage.set('No se pudo procesar la reserva. Por favor verifique los datos ingresados.');
        }
      }
    });
  }

  closeSuccessModal(): void {
    this.isSuccessModalOpen.set(false);
    this.router.navigate(['/my-reservations']);
  }
}
