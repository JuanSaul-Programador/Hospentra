import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReviewService } from '../../../../../core/services/review.service';
import { AuthService } from '../../../../../core/services/auth.service';
import { Review, ReviewAdminMetrics } from '../../../../../core/models/review.model';

@Component({
  selector: 'app-review-moderation',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './review-moderation.component.html',
  styleUrl: './review-moderation.component.scss'
})
export class ReviewModerationComponent implements OnInit {
  private readonly reviewService = inject(ReviewService);
  readonly authService = inject(AuthService);

  readonly reviews = signal<Review[]>([]);
  readonly metrics = signal<ReviewAdminMetrics | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly selectedRatingFilter = signal<number | null>(null);
  readonly unansweredOnlyFilter = signal<boolean | null>(null);
  readonly searchQuery = signal<string>('');

  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly totalElements = signal<number>(0);
  readonly pageSize = 10;

  readonly respondingReviewId = signal<number | null>(null);
  readonly responseText = signal<string>('');
  readonly isSubmittingResponse = signal<boolean>(false);
  readonly feedbackMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  readonly isAdmin = computed(() => this.authService.userRole() === 'ROLE_ADMIN');

  readonly filteredReviews = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const list = this.reviews();
    if (!query) {
      return list;
    }
    return list.filter(r =>
      r.guestName.toLowerCase().includes(query) ||
      (r.roomNumber && r.roomNumber.toLowerCase().includes(query)) ||
      (r.roomTypeName && r.roomTypeName.toLowerCase().includes(query)) ||
      r.comment.toLowerCase().includes(query)
    );
  });

  ngOnInit(): void {
    this.loadMetrics();
    this.loadReviews();
  }

  loadMetrics(): void {
    this.reviewService.getAdminMetrics().subscribe({
      next: (m) => this.metrics.set(m),
      error: () => {}
    });
  }

  loadReviews(page: number = 0): void {
    this.isLoading.set(true);
    const rating = this.selectedRatingFilter() ?? undefined;
    const unanswered = this.unansweredOnlyFilter() ?? undefined;

    this.reviewService.getReviewsForAdmin(rating, unanswered, page, this.pageSize).subscribe({
      next: (res) => {
        this.reviews.set(res.content || []);
        this.currentPage.set(res.number || 0);
        this.totalPages.set(res.totalPages || 0);
        this.totalElements.set(res.totalElements || 0);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  setRatingFilter(rating: number | null): void {
    this.selectedRatingFilter.set(rating);
    this.unansweredOnlyFilter.set(null);
    this.loadReviews(0);
  }

  setUnansweredFilter(unanswered: boolean | null): void {
    this.unansweredOnlyFilter.set(unanswered);
    this.selectedRatingFilter.set(null);
    this.loadReviews(0);
  }

  clearFilters(): void {
    this.selectedRatingFilter.set(null);
    this.unansweredOnlyFilter.set(null);
    this.searchQuery.set('');
    this.loadReviews(0);
  }

  openResponseForm(review: Review): void {
    this.respondingReviewId.set(review.id);
    this.responseText.set(review.managementResponse || '');
    this.feedbackMessage.set(null);
  }

  cancelResponse(): void {
    this.respondingReviewId.set(null);
    this.responseText.set('');
  }

  submitResponse(reviewId: number): void {
    const text = this.responseText().trim();
    if (!text) {
      this.feedbackMessage.set({ type: 'error', text: 'El texto de la réplica no puede estar vacío.' });
      return;
    }

    this.isSubmittingResponse.set(true);
    this.reviewService.addManagementResponse(reviewId, text).subscribe({
      next: (updated) => {
        this.reviews.update(current =>
          current.map(r => (r.id === updated.id ? updated : r))
        );
        this.respondingReviewId.set(null);
        this.responseText.set('');
        this.isSubmittingResponse.set(false);
        this.feedbackMessage.set({ type: 'success', text: 'Réplica corporativa de gerencia publicada exitosamente.' });
        this.loadMetrics();
        setTimeout(() => this.feedbackMessage.set(null), 4000);
      },
      error: () => {
        this.isSubmittingResponse.set(false);
        this.feedbackMessage.set({ type: 'error', text: 'No se pudo publicar la réplica. Inténtelo de nuevo.' });
      }
    });
  }

  getStarArray(rating: number): number[] {
    return Array.from({ length: 5 }, (_, i) => i + 1);
  }
}
