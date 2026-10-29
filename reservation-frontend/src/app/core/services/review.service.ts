import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse, Review, ReviewAdminMetrics, ReviewRequest, ReviewSummary } from '../models/review.model';

@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reviews`;

  getReviewsByRoom(roomId: number, rating?: number, page: number = 0, size: number = 10): Observable<PageResponse<Review>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (rating !== undefined && rating !== null && rating > 0) {
      params = params.set('rating', rating.toString());
    }
    return this.http.get<PageResponse<Review>>(`${this.baseUrl}/room/${roomId}`, { params });
  }

  getReviewSummary(roomId: number): Observable<ReviewSummary> {
    return this.http.get<ReviewSummary>(`${this.baseUrl}/room/${roomId}/summary`);
  }

  createReview(request: ReviewRequest): Observable<Review> {
    return this.http.post<Review>(this.baseUrl, request);
  }

  getReviewByReservation(reservationId: number): Observable<Review | null> {
    return this.http.get<Review | null>(`${this.baseUrl}/reservation/${reservationId}`);
  }

  addManagementResponse(reviewId: number, responseText: string): Observable<Review> {
    return this.http.post<Review>(`${this.baseUrl}/${reviewId}/management-response`, { response: responseText });
  }

  getReviewsForAdmin(rating?: number, unansweredOnly?: boolean, page: number = 0, size: number = 15): Observable<PageResponse<Review>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (rating !== undefined && rating !== null && rating > 0) {
      params = params.set('rating', rating.toString());
    }
    if (unansweredOnly !== undefined && unansweredOnly !== null) {
      params = params.set('unansweredOnly', unansweredOnly.toString());
    }
    return this.http.get<PageResponse<Review>>(`${this.baseUrl}/admin`, { params });
  }

  getAdminMetrics(): Observable<ReviewAdminMetrics> {
    return this.http.get<ReviewAdminMetrics>(`${this.baseUrl}/admin/metrics`);
  }
}
