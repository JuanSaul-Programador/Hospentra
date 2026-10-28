import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { RoomService } from '../../../core/services/room.service';
import { ReviewService } from '../../../core/services/review.service';
import { AuthService } from '../../../core/services/auth.service';
import { I18nService } from '../../../core/services/i18n.service';
import { Room } from '../../../core/models/room.model';
import { Review, ReviewSummary } from '../../../core/models/review.model';

@Component({
  selector: 'app-room-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './room-detail.component.html',
  styleUrl: './room-detail.component.scss'
})
export class RoomDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly roomService = inject(RoomService);
  private readonly reviewService = inject(ReviewService);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);

  readonly room = signal<Room | null>(null);
  readonly isLoadingRoom = signal<boolean>(true);
  readonly isLoadingReviews = signal<boolean>(true);
  readonly reviewSummary = signal<ReviewSummary | null>(null);
  readonly reviews = signal<Review[]>([]);
  readonly activeRatingFilter = signal<number | null>(null);
  readonly activeImageIndex = signal<number>(0);

  readonly minDate = this.formatDate(new Date());
  readonly checkInDate = signal<string>('');
  readonly checkOutDate = signal<string>('');

  readonly nightsCount = computed<number>(() => {
    const start = new Date(this.checkInDate());
    const end = new Date(this.checkOutDate());
    if (isNaN(start.getTime()) || isNaN(end.getTime())) return 1;
    const diff = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 1;
  });

  readonly totalEstimatedPrice = computed<number>(() => {
    const currentRoom = this.room();
    if (!currentRoom) return 0;
    return currentRoom.roomType.pricePerNight * this.nightsCount();
  });

  readonly galleryImages = computed<string[]>(() => {
    const currentRoom = this.room();
    if (!currentRoom) return [];
    return this.resolveGallery(currentRoom.id);
  });

  readonly ratingPercentage5 = computed<number>(() => this.getPercentage(5));
  readonly ratingPercentage4 = computed<number>(() => this.getPercentage(4));
  readonly ratingPercentage3 = computed<number>(() => this.getPercentage(3));
  readonly ratingPercentage2 = computed<number>(() => this.getPercentage(2));
  readonly ratingPercentage1 = computed<number>(() => this.getPercentage(1));

  readonly ratingCount5 = computed<number>(() => this.getCount(5));
  readonly ratingCount4 = computed<number>(() => this.getCount(4));
  readonly ratingCount3 = computed<number>(() => this.getCount(3));
  readonly ratingCount2 = computed<number>(() => this.getCount(2));
  readonly ratingCount1 = computed<number>(() => this.getCount(1));

  readonly recommendationRate = computed<number>(() => {
    const summary = this.reviewSummary();
    if (!summary || summary.totalReviews === 0) return 98;
    const pos = (summary.ratingDistribution[5] || 0) + (summary.ratingDistribution[4] || 0);
    return Math.round((pos * 100) / summary.totalReviews);
  });

  ngOnInit(): void {
    const today = new Date();
    const future = new Date(today.getTime() + 4 * 24 * 60 * 60 * 1000);
    this.checkInDate.set(this.formatDate(today));
    this.checkOutDate.set(this.formatDate(future));

    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const roomId = +idParam;
        this.loadRoom(roomId);
        this.loadReviewSummary(roomId);
        this.loadReviews(roomId, null);
      }
    });

    this.route.queryParamMap.subscribe(query => {
      const qIn = query.get('checkIn');
      const qOut = query.get('checkOut');
      if (qIn) this.checkInDate.set(qIn);
      if (qOut) this.checkOutDate.set(qOut);
    });
  }

  loadRoom(roomId: number): void {
    this.isLoadingRoom.set(true);
    this.roomService.getRoomById(roomId).subscribe({
      next: (data) => {
        this.room.set(data);
        this.isLoadingRoom.set(false);
      },
      error: () => {
        this.room.set({
          id: roomId,
          roomNumber: `${roomId}01`,
          status: 'AVAILABLE',
          roomType: {
            id: roomId,
            name: 'Suite Royal Vista al Mar',
            nameEn: 'Royal Ocean View Suite',
            description: 'Espaciosa suite con ventanales de piso a techo frente al océano, acabados en roble natural y terraza privada con jacuzzi.',
            descriptionEn: 'Spacious suite with floor-to-ceiling windows facing the ocean, natural oak finishes, and private terrace with jacuzzi.',
            capacity: 3,
            pricePerNight: 380
          }
        });
        this.isLoadingRoom.set(false);
      }
    });
  }

  loadReviewSummary(roomId: number): void {
    this.reviewService.getReviewSummary(roomId).subscribe({
      next: (summary) => this.reviewSummary.set(summary),
      error: () => {
        this.reviewSummary.set({
          roomId: roomId,
          averageRating: 4.8,
          totalReviews: 5,
          cleanlinessAverage: 4.9,
          serviceAverage: 4.8,
          comfortAverage: 4.9,
          locationAverage: 5.0,
          ratingDistribution: { 5: 3, 4: 1, 3: 1, 2: 0, 1: 0 },
          ratingPercentages: { 5: 60.0, 4: 20.0, 3: 20.0, 2: 0.0, 1: 0.0 }
        });
      }
    });
  }

  loadReviews(roomId: number, rating: number | null): void {
    this.isLoadingReviews.set(true);
    this.reviewService.getReviewsByRoom(roomId, rating || undefined).subscribe({
      next: (page) => {
        this.reviews.set(page.content);
        this.isLoadingReviews.set(false);
      },
      error: () => {
        this.reviews.set([]);
        this.isLoadingReviews.set(false);
      }
    });
  }

  setRatingFilter(rating: number | null): void {
    const current = this.activeRatingFilter();
    const next = current === rating ? null : rating;
    this.activeRatingFilter.set(next);
    const currentRoom = this.room();
    if (currentRoom) {
      this.loadReviews(currentRoom.id, next);
    }
  }

  setActiveImage(index: number): void {
    this.activeImageIndex.set(index);
  }

  proceedToCheckout(): void {
    const currentRoom = this.room();
    if (!currentRoom) return;
    this.router.navigate(['/reservations/checkout'], {
      queryParams: {
        roomId: currentRoom.id,
        checkIn: this.checkInDate(),
        checkOut: this.checkOutDate()
      }
    });
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
        const monthName = d.toLocaleDateString('en-US', { month: 'short' });
        return `${monthName} ${year}`;
      } else {
        const monthName = d.toLocaleDateString('es-ES', { month: 'short' });
        const capitalizedMonth = monthName.charAt(0).toUpperCase() + monthName.slice(1).replace('.', '');
        return `${capitalizedMonth} ${year}`;
      }
    } catch {
      return dateStr;
    }
  }

  private getPercentage(rating: number): number {
    const summary = this.reviewSummary();
    if (!summary || !summary.ratingPercentages) return 0;
    return summary.ratingPercentages[rating] || 0;
  }

  private getCount(rating: number): number {
    const summary = this.reviewSummary();
    if (!summary || !summary.ratingDistribution) return 0;
    return summary.ratingDistribution[rating] || 0;
  }

  private formatDate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  private resolveGallery(id: number): string[] {
    const baseMap: { [key: number]: string[] } = {
      1: [
        'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80',
        'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=800&q=80'
      ],
      2: [
        'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80',
        'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80'
      ],
      3: [
        'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1200&q=80',
        'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80'
      ],
      4: [
        'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80',
        'https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=800&q=80'
      ],
      5: [
        'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80',
        'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1591088398332-8a7791972843?auto=format&fit=crop&w=800&q=80'
      ],
      6: [
        'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80',
        'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80',
        'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80'
      ]
    };

    return baseMap[id] || baseMap[1];
  }
}
