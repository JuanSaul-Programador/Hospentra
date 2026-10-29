export interface SuitePerformance {
  roomId: number;
  roomNumber: string;
  roomTypeName: string;
  nightsOccupied: number;
  revenue: number;
  occupancyRate: number;
  adr: number;
}

export interface MonthlyTrend {
  month: string;
  roomRevenue: number;
  extrasRevenue: number;
  totalRevenue: number;
  occupancyRate: number;
}

export interface HotelAnalytics {
  totalRevenue: number;
  roomRevenue: number;
  extrasRevenue: number;
  gastronomyRevenue: number;
  barRevenue: number;
  spaRevenue: number;
  totalReservations: number;
  totalNightsSold: number;
  occupancyRate: number;
  adr: number;
  revpar: number;
  suitePerformances: SuitePerformance[];
  monthlyTrends: MonthlyTrend[];
}
