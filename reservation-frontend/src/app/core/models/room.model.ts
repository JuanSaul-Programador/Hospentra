export type RoomStatus = 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE';

export interface RoomType {
  id: number;
  name: string;
  nameEn?: string;
  description?: string;
  descriptionEn?: string;
  capacity: number;
  pricePerNight: number;
  coverImageUrl?: string;
}

export interface RoomImage {
  id: number;
  roomTypeId: number;
  imageUrl: string;
  isCover: boolean;
  displayOrder: number;
  createdAt?: string;
}

export interface Room {
  id: number;
  roomNumber: string;
  status: RoomStatus;
  roomType: RoomType;
  housekeeperName?: string;
  housekeepingNotes?: string;
  lastCleanedAt?: string;
  cleaningPriority?: 'HIGH' | 'NORMAL' | 'LOW';
}

export interface HousekeepingUpdateRequest {
  housekeeperName?: string;
  housekeepingNotes?: string;
  cleaningPriority?: string;
  status?: RoomStatus;
  markReady?: boolean;
}

export interface RoomTypeRequest {
  name: string;
  nameEn?: string;
  description?: string;
  descriptionEn?: string;
  capacity: number;
  pricePerNight: number;
}

export interface RoomRequest {
  roomNumber: string;
  status: RoomStatus;
  roomTypeId: number;
}
