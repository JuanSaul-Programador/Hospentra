export type NotificationEventType =
  | 'ORDER_CREATED'
  | 'ORDER_STATUS_CHANGED'
  | 'ORDER_THERAPIST_ASSIGNED'
  | 'ROOM_STATUS_CHANGED'
  | 'RESERVATION_CREATED'
  | 'RESERVATION_STATUS_CHANGED'
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
