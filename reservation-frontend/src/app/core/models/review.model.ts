export interface Review {
  id: number;
  reservationId: number;
  roomId: number;
  roomNumber: string;
  roomTypeName: string;
  roomTypeNameEn?: string;
  userId: number;
  guestName: string;
  rating: number;
  cleanlinessRating?: number;
  serviceRating?: number;
  comfortRating?: number;
  locationRating?: number;
  title?: string;
  comment: string;
  managementResponse?: string;
  managementRespondedAt?: string;
  createdAt: string;
  verifiedStay: boolean;
}

export interface ReviewSummary {
  roomId: number;
  averageRating: number;
  totalReviews: number;
  cleanlinessAverage?: number;
  serviceAverage?: number;
  comfortAverage?: number;
  locationAverage?: number;
  ratingDistribution: Record<number, number>;
  ratingPercentages: Record<number, number>;
}

export interface ReviewRequest {
  reservationId: number;
  rating: number;
  cleanlinessRating?: number;
  serviceRating?: number;
  comfortRating?: number;
  locationRating?: number;
  title?: string;
  comment: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
