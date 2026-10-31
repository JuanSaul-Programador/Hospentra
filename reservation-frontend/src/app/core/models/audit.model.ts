export interface AuditLogEntry {
  id: number;
  username: string;
  userRole: string;
  action: string;
  entityName: string;
  entityId: string;
  details: string;
  ipAddress: string;
  status: 'SUCCESS' | 'FAILURE' | string;
  createdAt: string;
}

export interface AuditMetrics {
  totalEvents: number;
  todayEvents: number;
  failedEvents: number;
  distinctOperators: number;
}

export interface AuditLogPageResponse {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface AuditFilterParams {
  action?: string;
  username?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}
