import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MaintenanceModalComponent } from './maintenance-modal.component';
import { SimpleChange } from '@angular/core';
import { MaintenanceBlock } from '../../../../../core/models/maintenance.model';
import { Room } from '../../../../../core/models/room.model';

describe('MaintenanceModalComponent', () => {
  let component: MaintenanceModalComponent;
  let fixture: ComponentFixture<MaintenanceModalComponent>;

  const mockRooms: Room[] = [
    {
      id: 10,
      roomNumber: '101',
      roomType: { id: 1, name: 'Presidential Suite', capacity: 2, pricePerNight: 350 },
      status: 'AVAILABLE',
      cleaningPriority: 'NORMAL',
      housekeeperName: '',
      housekeepingNotes: '',
      lastCleanedAt: '2026-10-31T10:00:00'
    }
  ];

  const mockBlocks: MaintenanceBlock[] = [
    {
      id: 1,
      roomId: 10,
      roomNumber: '101',
      roomTypeName: 'Presidential Suite',
      blockType: 'OUT_OF_ORDER',
      startDate: '2026-11-01',
      endDate: '2026-11-04',
      reason: 'Fuga en jacuzzi',
      contractorName: 'Hidráulica S.A.',
      status: 'ACTIVE',
      createdBy: 'admin',
      createdAt: '2026-10-31T12:00:00',
      durationDays: 3
    },
    {
      id: 2,
      roomId: 10,
      roomNumber: '101',
      blockType: 'OUT_OF_SERVICE',
      startDate: '2026-10-10',
      endDate: '2026-10-12',
      reason: 'Pintura',
      status: 'RESOLVED',
      createdAt: '2026-10-10T12:00:00',
      durationDays: 2
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MaintenanceModalComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(MaintenanceModalComponent);
    component = fixture.componentInstance;
    component.rooms = mockRooms;
    component.blocks = mockBlocks;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should count active blocks and filter correctly', () => {
    expect(component.activeBlocksCount()).toBe(1);

    component.statusFilter.set('ACTIVE');
    expect(component.filteredBlocks().length).toBe(1);

    component.statusFilter.set('RESOLVED');
    expect(component.filteredBlocks().length).toBe(1);

    component.statusFilter.set('ALL');
    expect(component.filteredBlocks().length).toBe(2);
  });

  it('should reset form when isOpen becomes true', () => {
    component.isOpen = true;
    component.ngOnChanges({
      isOpen: new SimpleChange(false, true, true)
    });

    expect(component.formRoomId()).toBe(10);
    expect(component.formBlockType()).toBe('OUT_OF_ORDER');
    expect(component.formReason()).toBe('');
  });

  it('should calculate nights correctly', () => {
    component.formStartDate.set('2026-11-01');
    component.formEndDate.set('2026-11-05');
    expect(component.calculatedNights()).toBe(4);
  });

  it('should validate empty reason and stop submission', () => {
    let emitted = false;
    component.createBlock.subscribe(() => emitted = true);

    component.formRoomId.set(10);
    component.formStartDate.set('2026-11-01');
    component.formEndDate.set('2026-11-04');
    component.formReason.set('');

    component.submitCreate();

    expect(component.formValidationError()).toBe('El motivo técnico del bloqueo es obligatorio.');
    expect(emitted).toBe(false);
  });

  it('should validate invalid inverted dates and stop submission', () => {
    let emitted = false;
    component.createBlock.subscribe(() => emitted = true);

    component.formRoomId.set(10);
    component.formStartDate.set('2026-11-05');
    component.formEndDate.set('2026-11-01');
    component.formReason.set('Avería');

    component.submitCreate();

    expect(component.formValidationError()).toBe('La fecha de fin debe ser estrictamente posterior a la fecha de inicio.');
    expect(emitted).toBe(false);
  });

  it('should emit createBlock when form is valid', () => {
    let emittedPayload: any = null;
    component.createBlock.subscribe(val => emittedPayload = val);

    component.formRoomId.set(10);
    component.formBlockType.set('OUT_OF_ORDER');
    component.formStartDate.set('2026-11-01');
    component.formEndDate.set('2026-11-05');
    component.formReason.set('Fuga en tina');
    component.formContractor.set('Fontanería Express');

    component.submitCreate();

    expect(component.formValidationError()).toBeNull();
    expect(emittedPayload).toEqual({
      roomId: 10,
      blockType: 'OUT_OF_ORDER',
      startDate: '2026-11-01',
      endDate: '2026-11-05',
      reason: 'Fuga en tina',
      contractor: 'Fontanería Express'
    });
  });

  it('should emit resolveBlock', () => {
    let emittedId: number | null = null;
    component.resolveBlock.subscribe(id => emittedId = id);

    component.onResolve(42);
    expect(emittedId).toBe(42);
  });

  it('should emit cancelBlock', () => {
    let emittedId: number | null = null;
    component.cancelBlock.subscribe(id => emittedId = id);

    component.onCancel(42);
    expect(emittedId).toBe(42);
  });

  it('should emit close', () => {
    let closed = false;
    component.close.subscribe(() => closed = true);

    component.onClose();
    expect(closed).toBe(true);
  });
});
