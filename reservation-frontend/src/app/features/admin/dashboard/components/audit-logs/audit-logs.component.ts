import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditService } from '../../../../../core/services/audit.service';
import { AuditLogEntry, AuditMetrics } from '../../../../../core/models/audit.model';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-logs.component.html',
  styleUrl: './audit-logs.component.scss'
})
export class AuditLogsComponent implements OnInit {
  private readonly auditService = inject(AuditService);

  readonly metrics = signal<AuditMetrics | null>(null);
  readonly logs = signal<AuditLogEntry[]>([]);
  readonly availableActions = signal<string[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly isExporting = signal<boolean>(false);

  readonly filterAction = signal<string>('');
  readonly filterUsername = signal<string>('');
  readonly filterStartDate = signal<string>('');
  readonly filterEndDate = signal<string>('');

  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(15);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(0);

  readonly expandedLogId = signal<number | null>(null);
  readonly toastMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadActions();
    this.loadMetrics();
    this.loadLogs();
  }

  loadActions(): void {
    this.auditService.getActions().subscribe({
      next: (actions) => this.availableActions.set(actions),
      error: () => {}
    });
  }

  loadMetrics(): void {
    this.auditService.getMetrics().subscribe({
      next: (m) => this.metrics.set(m),
      error: () => {}
    });
  }

  loadLogs(): void {
    this.isLoading.set(true);
    this.auditService.getLogs({
      action: this.filterAction() || undefined,
      username: this.filterUsername() || undefined,
      startDate: this.filterStartDate() || undefined,
      endDate: this.filterEndDate() || undefined,
      page: this.currentPage(),
      size: this.pageSize()
    }).subscribe({
      next: (res) => {
        this.logs.set(res.content);
        this.totalElements.set(res.totalElements);
        this.totalPages.set(res.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.showToast('Error al cargar la bitácora de auditoría.');
      }
    });
  }

  onFilterChange(): void {
    this.currentPage.set(0);
    this.loadLogs();
  }

  clearFilters(): void {
    this.filterAction.set('');
    this.filterUsername.set('');
    this.filterStartDate.set('');
    this.filterEndDate.set('');
    this.currentPage.set(0);
    this.loadLogs();
  }

  prevPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.update(p => p - 1);
      this.loadLogs();
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update(p => p + 1);
      this.loadLogs();
    }
  }

  toggleExpand(id: number): void {
    this.expandedLogId.update(curr => curr === id ? null : id);
  }

  exportReservations(): void {
    this.isExporting.set(true);
    const start = this.filterStartDate() || undefined;
    const end = this.filterEndDate() || undefined;

    this.auditService.exportReservationsCsv(start, end).subscribe({
      next: (blob) => {
        this.isExporting.set(false);
        this.triggerDownload(blob, `hospentra_reservas_${new Date().toISOString().slice(0, 10)}.csv`);
        this.showToast('Libro de reservas exportado exitosamente.');
      },
      error: () => {
        this.isExporting.set(false);
        this.showToast('Error al exportar el libro de reservas.');
      }
    });
  }

  exportFinancialJournal(): void {
    this.isExporting.set(true);
    const start = this.filterStartDate() || undefined;
    const end = this.filterEndDate() || undefined;

    this.auditService.exportFinancialJournalCsv(start, end).subscribe({
      next: (blob) => {
        this.isExporting.set(false);
        this.triggerDownload(blob, `hospentra_diario_financiero_${new Date().toISOString().slice(0, 10)}.csv`);
        this.showToast('Diario financiero exportado exitosamente.');
      },
      error: () => {
        this.isExporting.set(false);
        this.showToast('Error al exportar el diario financiero.');
      }
    });
  }

  private triggerDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }

  showToast(message: string): void {
    this.toastMessage.set(message);
    setTimeout(() => {
      this.toastMessage.set(null);
    }, 4000);
  }

  getActionIcon(action: string): string {
    switch (action) {
      case 'CHECK_IN': return 'luggage';
      case 'CHECK_OUT': return 'key';
      case 'CANCEL_RESERVATION': return 'cancel';
      case 'CONFIRM_RESERVATION': return 'check_circle';
      case 'CREATE_STAFF': return 'person_add';
      case 'UPDATE_STAFF': return 'manage_accounts';
      case 'TOGGLE_USER_STATUS': return 'lock_reset';
      case 'CHANGE_PASSWORD': return 'password';
      case 'REQUEST_ACCOUNT_DELETION': return 'delete_forever';
      case 'CANCEL_ACCOUNT_DELETION': return 'restore_from_trash';
      case 'ASSIGN_THERAPIST': return 'self_improvement';
      default: return 'history_edu';
    }
  }

  formatAction(action: string): string {
    switch (action) {
      case 'CHECK_IN': return 'Check-In Huésped';
      case 'CHECK_OUT': return 'Check-Out Suite';
      case 'CANCEL_RESERVATION': return 'Cancelación Reserva';
      case 'CONFIRM_RESERVATION': return 'Confirmación Folio';
      case 'CREATE_STAFF': return 'Alta de Personal';
      case 'UPDATE_STAFF': return 'Modificación Personal';
      case 'TOGGLE_USER_STATUS': return 'Conmutación Acceso';
      case 'CHANGE_PASSWORD': return 'Cambio Contraseña OTP';
      case 'REQUEST_ACCOUNT_DELETION': return 'Baja 30 Días Gracia';
      case 'CANCEL_ACCOUNT_DELETION': return 'Cancelación de Baja';
      case 'ASSIGN_THERAPIST': return 'Asignación Spa';
      default: return action;
    }
  }
}
