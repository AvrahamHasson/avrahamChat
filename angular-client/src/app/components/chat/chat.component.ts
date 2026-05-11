import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { combineLatest, map, tap } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { Message } from '../../models/Message';
import { User } from '../../models/User';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChatComponent {
  private chatService = inject(ChatService);
  private authService = inject(AuthService);
  private router = inject(Router);

  newMessage: string = '';

  readonly vm$ = combineLatest({
    user: this.authService.currentUser$,
    orderedUsers: this.chatService.contactOrder$,
    selected: this.chatService.selectedUser$,
    status: this.chatService.connectionStatus$,
    lastMessages: this.chatService.lastMessages$,
    messages: combineLatest([
      this.chatService.selectedUser$,
      this.chatService.messages$
    ]).pipe(
      map(([user, store]) => (user ? store[user.username] || [] : [])),
      tap(() => this.scrollToBottom())
    )
  });

  constructor() {
    this.chatService.refreshAllData();
  }

  selectUser(user: User) {
    this.chatService.selectUser(user);
  }

  sendMessage(currentUser: User, selectedUser: User) {
    const text = this.newMessage?.trim();
    if (!text) return;

    const msg: Message = {
      text: text,
      timestamp: new Date().toISOString(),
      sender: currentUser.username,
      status: 'sent'
    };

    this.chatService.addMessage(selectedUser.username, msg);
    this.newMessage = '';
  }

  onKeyDown(event: KeyboardEvent, currentUser: User | null, selectedUser: User | null) {
    if (event.key === 'Enter' && !event.shiftKey && currentUser && selectedUser) {
      event.preventDefault();
      this.sendMessage(currentUser, selectedUser);
    }
  }

  onLogout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private scrollToBottom() {
    setTimeout(() => {
      const container = document.querySelector('.messages-container');
      if (container) container.scrollTop = container.scrollHeight;
    }, 50);
  }
}