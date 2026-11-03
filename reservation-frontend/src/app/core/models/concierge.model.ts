export interface ConciergeMessageRequest {
  message: string;
  reservationId?: number | null;
  suiteNumber?: string | null;
  recipientId?: number | null;
}

export interface ConciergeMessageResponse {
  id: number;
  senderId: number;
  senderName: string;
  senderRole: string;
  recipientId?: number | null;
  reservationId?: number | null;
  suiteNumber?: string | null;
  message: string;
  readStatus: boolean;
  createdAt: string;
}

export interface ConciergeConversationSummary {
  guestUserId: number;
  guestName: string;
  guestEmail: string;
  suiteNumber?: string | null;
  lastMessage: string;
  lastMessageAt: string;
  unreadCount: number;
  claimedBy?: string | null;
  reservationId?: number | null;
}

export interface ConciergeStatusResponse {
  activeStay: boolean;
  suiteNumber?: string | null;
  reservationId?: number | null;
  guestName?: string | null;
}
