import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GuestConciergeWidgetComponent } from './guest-concierge-widget.component';
import { ConciergeChatService } from '../../../core/services/concierge-chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { of } from 'rxjs';
import { signal } from '@angular/core';

describe('GuestConciergeWidgetComponent', () => {
  let component: GuestConciergeWidgetComponent;
  let fixture: ComponentFixture<GuestConciergeWidgetComponent>;

  const mockChatService = {
    messages: signal([]),
    unreadCount: signal(2),
    isWidgetOpen: signal(false),
    isSending: signal(false),
    hasActiveStay: signal(true),
    conciergeStatus: signal({ activeStay: true, suiteNumber: '101', reservationId: 5, guestName: 'Guest' }),
    loadStatus: vi.fn(() => of({ activeStay: true, suiteNumber: '101', reservationId: 5, guestName: 'Guest' })),
    loadUnreadCount: vi.fn(),
    toggleWidget: vi.fn(() => mockChatService.isWidgetOpen.set(!mockChatService.isWidgetOpen())),
    closeWidget: vi.fn(() => mockChatService.isWidgetOpen.set(false)),
    markMyConversationAsRead: vi.fn(() => of(void 0)),
    sendMessage: vi.fn(() => of({ id: 10, message: 'Hola' }))
  };

  const mockAuthService = {
    currentUser: () => ({ id: 5, username: 'guest.test', role: 'ROLE_CUSTOMER' })
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GuestConciergeWidgetComponent],
      providers: [
        { provide: ConciergeChatService, useValue: mockChatService },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GuestConciergeWidgetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and verify customer role', () => {
    expect(component).toBeTruthy();
    expect(component.isCustomer()).toBe(true);
    expect(mockChatService.loadStatus).toHaveBeenCalled();
  });

  it('should toggle chat and call markMyConversationAsRead when opened', () => {
    component.toggleChat();
    expect(mockChatService.toggleWidget).toHaveBeenCalled();
    expect(mockChatService.markMyConversationAsRead).toHaveBeenCalled();
  });

  it('should close chat', () => {
    component.closeChat();
    expect(mockChatService.closeWidget).toHaveBeenCalled();
  });

  it('should send a message with suite info and reset input', () => {
    component.messageInput.set('Necesito toallas extras');
    component.sendMessage();

    expect(mockChatService.sendMessage).toHaveBeenCalledWith({
      message: 'Necesito toallas extras',
      suiteNumber: '101',
      reservationId: 5
    });
    expect(component.messageInput()).toBe('');
  });

  it('should handle quick prompt selection', () => {
    component.sendQuickPrompt('Late Check-Out');
    expect(mockChatService.sendMessage).toHaveBeenCalledWith({
      message: 'Late Check-Out',
      suiteNumber: '101',
      reservationId: 5
    });
  });
});
