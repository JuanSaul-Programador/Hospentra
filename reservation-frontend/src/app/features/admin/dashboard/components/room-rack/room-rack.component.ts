import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Room } from '../../../../../core/models/room.model';

@Component({
  selector: 'app-room-rack',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './room-rack.component.html',
  styleUrl: './room-rack.component.scss'
})
export class RoomRackComponent {
  @Input() rooms: Room[] = [];
  @Input() isActionProcessing: boolean = false;
  @Output() markClean = new EventEmitter<number>();

  onMarkClean(roomId: number): void {
    this.markClean.emit(roomId);
  }
}
