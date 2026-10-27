import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Reservation, ReservationStatus } from '../../../../../core/models/reservation.model';
import { Room } from '../../../../../core/models/room.model';

export interface TapeDay {
  date: Date;
  dateStr: string;
  dayName: string;
  dayNumber: number;
  monthName: string;
  isToday: boolean;
  isWeekend: boolean;
}

export interface TapeReservationBlock {
  reservation: Reservation;
  startCol: number;
  colSpan: number;
  isStartClipped: boolean;
  isEndClipped: boolean;
  nightsTotal: number;
  statusClass: string;
  statusLabel: string;
}

export interface TapeRoomRow {
  room: Room;
  blocks: TapeReservationBlock[];
}

export interface DailyOccupancy {
  dateStr: string;
  occupiedRooms: number;
  occupancyPercent: number;
}

@Component({
  selector: 'app-tape-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tape-chart.component.html',
  styleUrl: './tape-chart.component.scss'
})
export class TapeChartComponent {
  @Input() reservations: Reservation[] = [];
  @Input() rooms: Room[] = [];
  @Input() isActionProcessing: boolean = false;

  @Output() viewDetails = new EventEmitter<Reservation>();
  @Output() printVoucher = new EventEmitter<Reservation>();
  @Output() checkIn = new EventEmitter<Reservation>();
  @Output() checkOut = new EventEmitter<Reservation>();

  readonly activeBlockPopover = signal<Reservation | null>(null);
  readonly popoverPosition = signal<{ x: number; y: number } | null>(null);

  private readonly daysSpan = 14;
  readonly startDate = signal<Date>(this.getInitialStartDate());

  private getInitialStartDate(): Date {
    const today = new Date();
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    d.setDate(d.getDate() - 1);
    return d;
  }

  private formatDateStr(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private parseDateStr(str: string): Date {
    const parts = str.split('T')[0].split('-').map(Number);
    return new Date(parts[0], parts[1] - 1, parts[2]);
  }

  private diffDays(start: Date, end: Date): number {
    const utc1 = Date.UTC(start.getFullYear(), start.getMonth(), start.getDate());
    const utc2 = Date.UTC(end.getFullYear(), end.getMonth(), end.getDate());
    return Math.floor((utc2 - utc1) / (1000 * 60 * 60 * 24));
  }

  readonly visibleDays = computed<TapeDay[]>(() => {
    const start = this.startDate();
    const todayStr = this.formatDateStr(new Date());
    const dayNames = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
    const monthNames = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

    const days: TapeDay[] = [];
    for (let i = 0; i < this.daysSpan; i++) {
      const cur = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
      const dateStr = this.formatDateStr(cur);
      const dayOfWeek = cur.getDay();

      days.push({
        date: cur,
        dateStr,
        dayName: dayNames[dayOfWeek],
        dayNumber: cur.getDate(),
        monthName: monthNames[cur.getMonth()],
        isToday: dateStr === todayStr,
        isWeekend: dayOfWeek === 0 || dayOfWeek === 6
      });
    }
    return days;
  });

  readonly rangeLabel = computed<string>(() => {
    const days = this.visibleDays();
    if (days.length === 0) return '';
    const first = days[0];
    const last = days[days.length - 1];
    return `${first.dayNumber} ${first.monthName} — ${last.dayNumber} ${last.monthName} ${last.date.getFullYear()}`;
  });

  readonly roomRows = computed<TapeRoomRow[]>(() => {
    const roomsList = [...this.rooms].sort((a, b) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true }));
    const days = this.visibleDays();
    if (days.length === 0) return [];

    const windowStart = days[0].date;
    const windowEnd = new Date(days[days.length - 1].date.getFullYear(), days[days.length - 1].date.getMonth(), days[days.length - 1].date.getDate() + 1);

    return roomsList.map(room => {
      const roomReservations = this.reservations.filter(r =>
        (r.room?.id === room.id || r.room?.roomNumber === room.roomNumber) &&
        r.status !== 'CANCELLED'
      );

      const blocks: TapeReservationBlock[] = [];

      for (const res of roomReservations) {
        if (!res.checkInDate || !res.checkOutDate) continue;

        const checkIn = this.parseDateStr(res.checkInDate);
        const checkOut = this.parseDateStr(res.checkOutDate);

        if (checkOut <= windowStart || checkIn >= windowEnd) {
          continue;
        }

        const nightsTotal = Math.max(1, this.diffDays(checkIn, checkOut));
        const rawStartOffset = this.diffDays(windowStart, checkIn);
        const rawEndOffset = this.diffDays(windowStart, checkOut);

        const startCol = Math.max(0, rawStartOffset);
        const endCol = Math.min(this.daysSpan, rawEndOffset);
        const colSpan = Math.max(1, endCol - startCol);

        const isStartClipped = rawStartOffset < 0;
        const isEndClipped = rawEndOffset > this.daysSpan;

        let statusClass = 'status-confirmed';
        let statusLabel = 'Confirmada';
        if (res.status === 'CHECKED_IN') {
          statusClass = 'status-inhouse';
          statusLabel = 'En Casa';
        } else if (res.status === 'COMPLETED') {
          statusClass = 'status-completed';
          statusLabel = 'Completada';
        } else if (res.status === 'PENDING') {
          statusClass = 'status-pending';
          statusLabel = 'Pendiente';
        }

        blocks.push({
          reservation: res,
          startCol,
          colSpan,
          isStartClipped,
          isEndClipped,
          nightsTotal,
          statusClass,
          statusLabel
        });
      }

      return {
        room,
        blocks
      };
    });
  });

  readonly dailyOccupancy = computed<DailyOccupancy[]>(() => {
    const days = this.visibleDays();
    const totalRooms = Math.max(1, this.rooms.length);

    return days.map(day => {
      const activeCount = this.reservations.filter(res => {
        if (res.status === 'CANCELLED' || !res.checkInDate || !res.checkOutDate) return false;
        const checkInStr = res.checkInDate.split('T')[0];
        const checkOutStr = res.checkOutDate.split('T')[0];
        return day.dateStr >= checkInStr && day.dateStr < checkOutStr;
      }).length;

      const occupancyPercent = Math.min(100, Math.round((activeCount / totalRooms) * 100));

      return {
        dateStr: day.dateStr,
        occupiedRooms: activeCount,
        occupancyPercent
      };
    });
  });

  prevPeriod(): void {
    const current = this.startDate();
    const next = new Date(current.getFullYear(), current.getMonth(), current.getDate() - 7);
    this.startDate.set(next);
    this.closePopover();
  }

  nextPeriod(): void {
    const current = this.startDate();
    const next = new Date(current.getFullYear(), current.getMonth(), current.getDate() + 7);
    this.startDate.set(next);
    this.closePopover();
  }

  goToToday(): void {
    this.startDate.set(this.getInitialStartDate());
    this.closePopover();
  }

  onBlockClick(event: MouseEvent, res: Reservation): void {
    event.stopPropagation();
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    this.activeBlockPopover.set(res);
    this.popoverPosition.set({
      x: Math.min(window.innerWidth - 320, Math.max(16, rect.left)),
      y: rect.bottom + 8
    });
  }

  closePopover(): void {
    this.activeBlockPopover.set(null);
    this.popoverPosition.set(null);
  }

  onViewDetails(res: Reservation): void {
    this.closePopover();
    this.viewDetails.emit(res);
  }

  onPrintVoucher(res: Reservation): void {
    this.closePopover();
    this.printVoucher.emit(res);
  }

  onCheckIn(res: Reservation): void {
    this.closePopover();
    this.checkIn.emit(res);
  }

  onCheckOut(res: Reservation): void {
    this.closePopover();
    this.checkOut.emit(res);
  }

  isCheckInEligible(res: Reservation): boolean {
    const today = this.formatDateStr(new Date());
    return res.status === 'CONFIRMED' && res.checkInDate.split('T')[0] <= today;
  }

  isCheckOutEligible(res: Reservation): boolean {
    return res.status === 'CHECKED_IN';
  }
}
