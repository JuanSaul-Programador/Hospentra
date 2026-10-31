import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TapeChartComponent } from './tape-chart.component';
import { Room } from '../../../../../core/models/room.model';
import { Reservation } from '../../../../../core/models/reservation.model';
import { MaintenanceBlock } from '../../../../../core/models/maintenance.model';

describe('TapeChartComponent', () => {
  let component: TapeChartComponent;
  let fixture: ComponentFixture<TapeChartComponent>;

  const mockRooms: Room[] = [
    {
      id: 1,
      roomNumber: '101',
      roomType: { id: 1, name: 'Deluxe Suite', capacity: 2, pricePerNight: 200 },
      status: 'AVAILABLE',
      cleaningPriority: 'NORMAL',
      housekeeperName: '',
      housekeepingNotes: '',
      lastCleanedAt: '2026-10-31T10:00:00'
    }
  ];

  const mockReservations: Reservation[] = [
    {
      id: 10,
      room: mockRooms[0],
      guest: { id: 1, firstName: 'Carlos', lastName: 'Gómez', email: 'carlos@example.com', phone: '123' },
      checkInDate: new Date().toISOString(),
      checkOutDate: new Date(Date.now() + 86400000 * 2).toISOString(),
      status: 'CONFIRMED',
      totalPrice: 400,
      createdAt: new Date().toISOString()
    }
  ];

  const mockMaintenance: MaintenanceBlock[] = [
    {
      id: 5,
      roomId: 1,
      roomNumber: '101',
      roomTypeName: 'Deluxe Suite',
      blockType: 'OUT_OF_ORDER',
      startDate: new Date(Date.now() + 86400000 * 3).toISOString().split('T')[0],
      endDate: new Date(Date.now() + 86400000 * 6).toISOString().split('T')[0],
      reason: 'Falla climatización',
      contractorName: 'ClimaTech',
      status: 'ACTIVE',
      createdBy: 'admin',
      createdAt: new Date().toISOString(),
      durationDays: 3
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TapeChartComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TapeChartComponent);
    component = fixture.componentInstance;
    component.rooms = mockRooms;
    component.reservations = mockReservations;
    component.maintenanceBlocks = mockMaintenance;
    fixture.detectChanges();
  });

  it('should create TapeChartComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should render 14 days in visibleDays', () => {
    expect(component.visibleDays().length).toBe(14);
  });

  it('should include maintenance blocks in roomRows', () => {
    const rows = component.roomRows();
    expect(rows.length).toBe(1);
    expect(rows[0].maintenanceBlocks.length).toBe(1);
    expect(rows[0].maintenanceBlocks[0].block.id).toBe(5);
    expect(rows[0].maintenanceBlocks[0].typeClass).toBe('maintenance-ooo');
  });

  it('should navigate 7 days forward and backward', () => {
    const initialDate = new Date(component.startDate());
    component.nextPeriod();
    const advancedDate = new Date(component.startDate());
    expect(advancedDate.getTime() - initialDate.getTime()).toBe(7 * 24 * 60 * 60 * 1000);

    component.prevPeriod();
    const revertedDate = new Date(component.startDate());
    expect(revertedDate.getTime()).toBe(initialDate.getTime());
  });

  it('should emit openMaintenance when onManageMaintenance is called', () => {
    let emitted = false;
    component.openMaintenance.subscribe(() => emitted = true);
    component.onManageMaintenance();
    expect(emitted).toBe(true);
  });
});
