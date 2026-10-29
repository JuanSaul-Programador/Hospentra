import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { RoomService } from '../../core/services/room.service';
import { ReviewService } from '../../core/services/review.service';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/services/i18n.service';
import { Room, RoomType } from '../../core/models/room.model';
import { AuthModalComponent } from '../../shared/components/auth-modal/auth-modal.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AuthModalComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  private readonly roomService = inject(RoomService);
  private readonly reviewService = inject(ReviewService);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  readonly rooms = signal<Room[]>([]);
  readonly roomRatings = signal<Record<number, { score: number; count: number }>>({});
  readonly isLoading = signal<boolean>(true);
  readonly selectedCategory = signal<string>('all');
  readonly sortBy = signal<string>('featured');

  readonly minDate = this.formatDate(new Date());
  readonly searchCheckIn = signal<string>(this.formatDate(new Date(Date.now() + 86400000)));
  readonly searchCheckOut = signal<string>(this.formatDate(new Date(Date.now() + 5 * 86400000)));
  readonly searchGuests = signal<number>(2);

  private formatDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  readonly isAuthModalOpen = signal<boolean>(false);
  readonly selectedRoomForBooking = signal<Room | null>(null);
  readonly selectedRoomImage = signal<string>('');

  private readonly defaultRooms: Room[] = [
    {
      id: 1,
      roomNumber: '101',
      status: 'AVAILABLE',
      roomType: {
        id: 1,
        name: 'Suite Royal Vista al Mar',
        nameEn: 'Royal Ocean View Suite',
        description: 'Espaciosa suite con ventanales de piso a techo frente al océano, terraza privada con tina de hidromasaje y servicio de mayordomo.',
        descriptionEn: 'Spacious suite with floor-to-ceiling oceanfront windows, natural oak finishes, and private terrace with hot tub.',
        capacity: 3,
        pricePerNight: 380.00
      }
    },
    {
      id: 2,
      roomNumber: '202',
      status: 'AVAILABLE',
      roomType: {
        id: 2,
        name: 'Habitación Doble Deluxe Jardín',
        nameEn: 'Deluxe Garden Double Room',
        description: 'Diseño contemporáneo con vistas a los jardines zen del hotel, aislamiento acústico total y amenities botánicos orgánicos.',
        descriptionEn: 'Contemporary boutique design overlooking tranquil zen gardens, acoustic soundproofing, and organic botanic amenities.',
        capacity: 2,
        pricePerNight: 250.00
      }
    },
    {
      id: 3,
      roomNumber: '301',
      status: 'AVAILABLE',
      roomType: {
        id: 3,
        name: 'Penthouse Panorámico Hospentra',
        nameEn: 'Panoramic Hospentra Penthouse',
        description: 'El máximo exponente del lujo boutique con 360 grados de horizonte, sala de estar independiente y acceso a lounge VIP.',
        descriptionEn: 'The pinnacle of boutique luxury with 360-degree horizon views, separate living sanctuary, and VIP lounge privileges.',
        capacity: 4,
        pricePerNight: 620.00
      }
    },
    {
      id: 4,
      roomNumber: '401',
      status: 'AVAILABLE',
      roomType: {
        id: 4,
        name: 'Villa Serena con Alberca Privada',
        nameEn: 'Serena Private Pool Villa',
        description: 'Refugio idílico rodeado de palmeras y vegetación tropical, máxima privacidad con alberca climatizada de agua salada.',
        descriptionEn: 'Secluded retreat surrounded by tropical palms, absolute privacy featuring a private heated saltwater plunge pool.',
        capacity: 4,
        pricePerNight: 540.00
      }
    },
    {
      id: 5,
      roomNumber: '502',
      status: 'AVAILABLE',
      roomType: {
        id: 5,
        name: 'Studio Loft Arquitectónico',
        nameEn: 'Architectural Studio Loft',
        description: 'Estética minimalista con doble altura, luz natural envolvente y mobiliario de autor en nogal y concreto pulido.',
        descriptionEn: 'Minimalist double-height aesthetic with immersive natural daylight and bespoke walnut and polished concrete furnishings.',
        capacity: 2,
        pricePerNight: 290.00
      }
    },
    {
      id: 6,
      roomNumber: '601',
      status: 'AVAILABLE',
      roomType: {
        id: 6,
        name: 'Grand Master Suite Presidencial',
        nameEn: 'Grand Master Presidential Suite',
        description: 'Exclusividad absoluta con dos niveles, comedor privado para seis comensales, solárium en terraza y cava de vinos.',
        descriptionEn: 'Uncompromising exclusivity spanning two levels, private six-guest dining room, rooftop solarium, and curated wine cellar.',
        capacity: 6,
        pricePerNight: 850.00
      }
    }
  ];

  private readonly roomImages: Record<number, string> = {
    1: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
    2: 'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=1200&q=80',
    3: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1200&q=80',
    4: 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80',
    5: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
    6: 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80'
  };

  readonly filteredRooms = computed(() => {
    let list = this.rooms();
    const category = this.selectedCategory();
    const sort = this.sortBy();

    if (category !== 'all') {
      list = list.filter(r =>
        r.roomType.name.toLowerCase().includes(category.toLowerCase()) ||
        (r.roomType.description && r.roomType.description.toLowerCase().includes(category.toLowerCase()))
      );
    }

    const sorted = [...list];
    switch (sort) {
      case 'price-asc':
        sorted.sort((a, b) => a.roomType.pricePerNight - b.roomType.pricePerNight);
        break;
      case 'price-desc':
        sorted.sort((a, b) => b.roomType.pricePerNight - a.roomType.pricePerNight);
        break;
      case 'capacity':
        sorted.sort((a, b) => b.roomType.capacity - a.roomType.capacity);
        break;
      default:
        break;
    }
    return sorted;
  });

  ngOnInit(): void {
    this.loadRooms();
  }

  loadRooms(): void {
    this.isLoading.set(true);
    this.roomService.getRooms().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          this.rooms.set(data);
          this.loadRoomRatings(data);
        } else {
          this.rooms.set(this.defaultRooms);
          this.loadRoomRatings(this.defaultRooms);
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.rooms.set(this.defaultRooms);
        this.loadRoomRatings(this.defaultRooms);
        this.isLoading.set(false);
      }
    });
  }

  loadRoomRatings(rooms: Room[]): void {
    rooms.forEach(r => {
      this.reviewService.getReviewSummary(r.id).subscribe({
        next: (summary) => {
          this.roomRatings.update(map => ({
            ...map,
            [r.id]: {
              score: summary.averageRating || 0,
              count: summary.totalReviews || 0
            }
          }));
        },
        error: () => {
          this.roomRatings.update(map => ({
            ...map,
            [r.id]: { score: 0, count: 0 }
          }));
        }
      });
    });
  }

  getRoomRating(room: Room): { score: number; count: number } {
    return this.roomRatings()[room.id] || { score: 0, count: 0 };
  }

  getRoomImage(room: Room): string {
    return this.roomImages[room.id] ||
      this.roomImages[((room.id - 1) % 6) + 1] ||
      'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80';
  }

  getCategoryBadge(name: string): string {
    const isEn = this.i18n.currentLang() === 'en';
    if (name.toLowerCase().includes('suite')) return isEn ? 'Luxury Suite' : 'Suite de Lujo';
    if (name.toLowerCase().includes('villa')) return isEn ? 'Private Villa' : 'Villa Privada';
    if (name.toLowerCase().includes('penthouse')) return isEn ? 'Signature Collection' : 'Colección Signature';
    if (name.toLowerCase().includes('loft')) return isEn ? 'Urban Edition' : 'Edición Urbana';
    return isEn ? 'Boutique Residence' : 'Residencia Boutique';
  }

  onFilterChange(category: string): void {
    this.selectedCategory.set(category);
  }

  onSortChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.sortBy.set(value);
  }

  onBook(room: Room): void {
    const img = this.getRoomImage(room);
    if (!this.authService.isAuthenticated()) {
      this.selectedRoomForBooking.set(room);
      this.selectedRoomImage.set(img);
      this.isAuthModalOpen.set(true);
    } else {
      this.router.navigate(['/reservations/checkout'], {
        queryParams: {
          roomId: room.id,
          checkIn: this.searchCheckIn(),
          checkOut: this.searchCheckOut(),
          guests: this.searchGuests()
        }
      });
    }
  }

  closeAuthModal(): void {
    this.isAuthModalOpen.set(false);
    this.selectedRoomForBooking.set(null);
  }

  onLoginSuccess(): void {
    this.isAuthModalOpen.set(false);
    const room = this.selectedRoomForBooking();
    if (room) {
      this.onBook(room);
    }
  }
}
