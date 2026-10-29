import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService } from '../../../../../core/services/analytics.service';
import { HotelAnalytics } from '../../../../../core/models/analytics.model';

@Component({
  selector: 'app-executive-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './executive-analytics.component.html',
  styleUrl: './executive-analytics.component.scss'
})
export class ExecutiveAnalyticsComponent implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);

  readonly analytics = signal<HotelAnalytics | null>(null);
  readonly isLoading = signal<boolean>(false);

  readonly maxMonthlyRevenue = computed(() => {
    const data = this.analytics();
    if (!data || !data.monthlyTrends || data.monthlyTrends.length === 0) return 1000;
    const max = Math.max(...data.monthlyTrends.map(m => m.totalRevenue));
    return max > 0 ? max : 1000;
  });

  readonly roomRevenuePercentage = computed(() => {
    const a = this.analytics();
    if (!a || a.totalRevenue <= 0) return 85;
    return Math.round((a.roomRevenue / a.totalRevenue) * 100);
  });

  readonly extrasRevenuePercentage = computed(() => {
    const a = this.analytics();
    if (!a || a.totalRevenue <= 0) return 15;
    return Math.round((a.extrasRevenue / a.totalRevenue) * 100);
  });

  readonly gastronomyPercentage = computed(() => {
    const a = this.analytics();
    if (!a || a.extrasRevenue <= 0) return 50;
    return Math.round((a.gastronomyRevenue / a.extrasRevenue) * 100);
  });

  readonly barPercentage = computed(() => {
    const a = this.analytics();
    if (!a || a.extrasRevenue <= 0) return 25;
    return Math.round((a.barRevenue / a.extrasRevenue) * 100);
  });

  readonly spaPercentage = computed(() => {
    const a = this.analytics();
    if (!a || a.extrasRevenue <= 0) return 25;
    return Math.round((a.spaRevenue / a.extrasRevenue) * 100);
  });

  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    this.isLoading.set(true);
    this.analyticsService.getExecutiveAnalytics().subscribe({
      next: (data) => {
        this.analytics.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  getBarHeightPercentage(amount: number): number {
    const max = this.maxMonthlyRevenue();
    return Math.min(100, Math.max(12, Math.round((amount / max) * 100)));
  }
}
