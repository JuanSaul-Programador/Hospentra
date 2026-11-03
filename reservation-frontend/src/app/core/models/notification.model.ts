export type NotificationEventType =
  | 'ORDER_CREATED'
  | 'ORDER_STATUS_CHANGED'
  | 'ORDER_THERAPIST_ASSIGNED'
  | 'ROOM_STATUS_CHANGED'
  | 'RESERVATION_CREATED'
  | 'RESERVATION_STATUS_CHANGED'
  | 'PRE_CHECK_IN_COMPLETED'
  | 'CONCIERGE_MESSAGE'
  | 'CONCIERGE_CLAIM'
  | 'USER'
  | 'PRE_CHECK_IN'
  | 'ROOM_SERVICE'
  | 'RESERVATION'
  | 'CHECK_IN'
  | 'HANDSHAKE'
  | 'PING';

export interface NotificationEvent<T = any> {
  eventType: NotificationEventType;
  targetChannel: string;
  entityId: number | null;
  title: string;
  message: string;
  payload: T;
  timestamp: string;
}

export interface InAppNotification {
  id: number;
  title: string;
  message: string;
  type: string;
  read: boolean;
  link?: string;
  createdAt: string;
}
