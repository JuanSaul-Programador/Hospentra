export type ExtraCategory = 'GASTRONOMY' | 'BAR_WINE' | 'WELLNESS_SPA';
export type OrderStatus = 'RECEIVED' | 'PREPARING' | 'READY_FOR_DELIVERY' | 'DELIVERED' | 'CANCELLED';

export interface ExtraService {
  id: number;
  name: string;
  category: ExtraCategory;
  description: string;
  price: number;
  imageUrl: string;
  available: boolean;
}

export interface RoomOrderItem {
  id: number;
  extraServiceId: number;
  serviceName: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface RoomOrder {
  id: number;
  reservationId: number;
  roomNumber: string;
  guestName: string;
  totalAmount: number;
  status: OrderStatus;
  notes: string;
  orderedAt: string;
  deliveredAt?: string | null;
  items: RoomOrderItem[];
}

export interface CreateRoomOrderItem {
  extraServiceId: number;
  quantity: number;
}

export interface CreateRoomOrderRequest {
  reservationId: number;
  notes?: string;
  items: CreateRoomOrderItem[];
}
