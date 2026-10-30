import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RoomImage, RoomType } from '../../../../../core/models/room.model';
import { RoomService } from '../../../../../core/services/room.service';

@Component({
  selector: 'app-suite-gallery-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './suite-gallery-modal.component.html',
  styleUrl: './suite-gallery-modal.component.scss'
})
export class SuiteGalleryModalComponent implements OnChanges {
  private readonly roomService = inject(RoomService);

  @Input() roomType: RoomType | null = null;
  @Input() isOpen: boolean = false;

  @Output() closeModal = new EventEmitter<void>();
  @Output() galleryUpdated = new EventEmitter<void>();

  readonly images = signal<RoomImage[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isUploading = signal<boolean>(false);
  readonly uploadAsCover = signal<boolean>(false);
  readonly selectedFile = signal<File | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly feedback = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['isOpen'] || changes['roomType']) && this.isOpen && this.roomType) {
      this.loadImages();
      this.resetUploadState();
    }
  }

  loadImages(): void {
    if (!this.roomType) return;
    this.isLoading.set(true);
    this.roomService.getRoomImages(this.roomType.id).subscribe({
      next: (data) => {
        this.images.set(data || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.feedback.set({ type: 'error', text: 'Error al sincronizar la galería de la suite.' });
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (!file.type.startsWith('image/')) {
        this.feedback.set({ type: 'error', text: 'Solo se permiten archivos de imagen (JPG, PNG, WebP).' });
        return;
      }
      if (file.size > 10 * 1024 * 1024) {
        this.feedback.set({ type: 'error', text: 'La imagen excede el límite de 10 MB.' });
        return;
      }
      this.selectedFile.set(file);
      const reader = new FileReader();
      reader.onload = () => this.previewUrl.set(reader.result as string);
      reader.readAsDataURL(file);
    }
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      if (!file.type.startsWith('image/')) {
        this.feedback.set({ type: 'error', text: 'Solo se admiten archivos de imagen.' });
        return;
      }
      this.selectedFile.set(file);
      const reader = new FileReader();
      reader.onload = () => this.previewUrl.set(reader.result as string);
      reader.readAsDataURL(file);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  cancelSelectedFile(): void {
    this.resetUploadState();
  }

  uploadImage(): void {
    const file = this.selectedFile();
    if (!file || !this.roomType) return;

    this.isUploading.set(true);
    this.feedback.set(null);

    this.roomService.uploadRoomImage(this.roomType.id, file, this.uploadAsCover()).subscribe({
      next: () => {
        this.isUploading.set(false);
        this.feedback.set({ type: 'success', text: 'Fotografía subida y procesada correctamente.' });
        this.resetUploadState();
        this.loadImages();
        this.galleryUpdated.emit();
      },
      error: (err) => {
        this.isUploading.set(false);
        const msg = err.error?.detail || 'Error al guardar la fotografía en el servidor.';
        this.feedback.set({ type: 'error', text: msg });
      }
    });
  }

  setCover(imageId: number): void {
    this.feedback.set(null);
    this.roomService.setCoverImage(imageId).subscribe({
      next: () => {
        this.feedback.set({ type: 'success', text: 'Foto de portada principal actualizada.' });
        this.loadImages();
        this.galleryUpdated.emit();
      },
      error: () => {
        this.feedback.set({ type: 'error', text: 'No se pudo cambiar la foto de portada.' });
      }
    });
  }

  deleteImage(imageId: number): void {
    if (!confirm('¿Confirma que desea eliminar permanentemente esta fotografía de la suite?')) {
      return;
    }
    this.feedback.set(null);
    this.roomService.deleteRoomImage(imageId).subscribe({
      next: () => {
        this.feedback.set({ type: 'success', text: 'Fotografía eliminada.' });
        this.loadImages();
        this.galleryUpdated.emit();
      },
      error: () => {
        this.feedback.set({ type: 'error', text: 'No se pudo eliminar la imagen.' });
      }
    });
  }

  resolveUrl(url: string): string {
    return this.roomService.resolveImageUrl(url);
  }

  get coverImage(): RoomImage | undefined {
    return this.images().find((img) => img.isCover);
  }

  get referentialImages(): RoomImage[] {
    return this.images().filter((img) => !img.isCover);
  }

  private resetUploadState(): void {
    this.selectedFile.set(null);
    this.previewUrl.set(null);
    this.uploadAsCover.set(false);
  }
}
