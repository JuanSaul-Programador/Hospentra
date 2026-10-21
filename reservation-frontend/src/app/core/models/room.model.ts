export type RoomStatus = 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE';

export interface RoomType {
  id: number;
  name: string;
  description?: string;
  capacity: number;
  pricePerNight: number;
}

export interface Room {
  id: number;
  roomNumber: string;
  status: RoomStatus;
  roomType: RoomType;
}

export interface RoomTypeRequest {
  name: string;
  description?: string;
  capacity: number;
  pricePerNight: number;
}

export interface RoomRequest {
  roomNumber: string;
  status: RoomStatus;
  roomTypeId: number;
}
