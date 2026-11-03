import { Component, OnInit, inject, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConciergeChatService } from '../../../core/services/concierge-chat.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-guest-concierge-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './guest-concierge-widget.component.html',
  styleUrl: './guest-concierge-widget.component.scss'
})
export class GuestConciergeWidgetComponent implements OnInit, AfterViewChecked {
  readonly chatService = inject(ConciergeChatService);
  readonly authService = inject(AuthService);

  @ViewChild('chatScroll') private chatScrollContainer?: ElementRef<HTMLDivElement>;

  readonly messageInput = signal<string>('');
  private shouldScrollToBottom = false;

  readonly quickPrompts = [
    'Toallas y amenidades extras para mi suite',
    'Consultar disponibilidad de Late Check-Out',
    'Horarios y reservas para el Spa Biofílico',
    'Solicitar servicio de hielo o copas en suite'
  ];

  ngOnInit(): void {
    if (this.isCustomer()) {
      this.chatService.loadStatus().subscribe();
    }
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  isCustomer(): boolean {
    return this.authService.currentUser()?.role === 'ROLE_CUSTOMER';
  }

  toggleChat(): void {
    this.chatService.toggleWidget();
    if (this.chatService.isWidgetOpen()) {
      this.shouldScrollToBottom = true;
      this.chatService.markMyConversationAsRead().subscribe();
    }
  }

  closeChat(): void {
    this.chatService.closeWidget();
  }

  sendQuickPrompt(promptText: string): void {
    this.messageInput.set(promptText);
    this.sendMessage();
  }

  sendMessage(): void {
    const text = this.messageInput().trim();
    if (!text || this.chatService.isSending()) return;

    const status = this.chatService.conciergeStatus();
    this.chatService.sendMessage({
      message: text,
      suiteNumber: status?.suiteNumber,
      reservationId: status?.reservationId
    }).subscribe({
      next: () => {
        this.messageInput.set('');
        this.shouldScrollToBottom = true;
      },
      error: () => {}
    });
  }

  private scrollToBottom(): void {
    if (this.chatScrollContainer) {
      const el = this.chatScrollContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }
}
