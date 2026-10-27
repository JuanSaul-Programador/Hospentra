import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Therapist } from '../../../../../core/models/therapist.model';

@Component({
  selector: 'app-therapist-directory-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './therapist-directory-modal.component.html',
  styleUrl: './therapist-directory-modal.component.scss'
})
export class TherapistDirectoryModalComponent {
  @Input() isOpen: boolean = false;
  @Input() therapists: Therapist[] = [];
  @Input() isSaving: boolean = false;

  @Output() close = new EventEmitter<void>();
  @Output() toggleStatus = new EventEmitter<Therapist>();
  @Output() delete = new EventEmitter<number>();
  @Output() create = new EventEmitter<{ name: string; specialty: string; phone?: string }>();

  readonly isFormModalOpen = signal<boolean>(false);
  readonly formName = signal<string>('');
  readonly formSpecialty = signal<string>('');
  readonly formPhone = signal<string>('');

  openFormModal(): void {
    this.formName.set('');
    this.formSpecialty.set('');
    this.formPhone.set('');
    this.isFormModalOpen.set(true);
  }

  closeFormModal(): void {
    this.isFormModalOpen.set(false);
  }

  submitForm(): void {
    if (!this.formName().trim() || !this.formSpecialty().trim()) {
      return;
    }
    this.create.emit({
      name: this.formName().trim(),
      specialty: this.formSpecialty().trim(),
      phone: this.formPhone().trim() || undefined
    });
  }

  onToggleStatus(therapist: Therapist): void {
    this.toggleStatus.emit(therapist);
  }

  onDelete(id: number): void {
    this.delete.emit(id);
  }

  onClose(): void {
    this.close.emit();
  }
}
