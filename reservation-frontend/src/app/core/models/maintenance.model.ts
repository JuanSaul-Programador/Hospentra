export type BlockType = 'OUT_OF_ORDER' | 'OUT_OF_SERVICE';

export type BlockStatus = 'ACTIVE' | 'RESOLVED' | 'CANCELLED';

export interface MaintenanceBlock {
  id: number;
  roomId: number;
  roomNumber: string;
  roomTypeName?: string;
  blockType: BlockType;
  startDate: string;
  endDate: string;
  reason: string;
  contractorName?: string;
  status: BlockStatus;
  resolvedAt?: string;
  createdBy?: string;
  createdAt: string;
  durationDays: number;
}

export interface CreateMaintenanceBlockRequest {
  roomId: number;
  blockType: BlockType;
  startDate: string;
  endDate: string;
  reason: string;
  contractor?: string;
}
