import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ChatService } from '../../services/chat.service';
import { User } from '../../models/User';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserListComponent {
  private chatService = inject(ChatService);
  private router = inject(Router);

  users$ = this.chatService.contactOrder$;

  constructor() {
    this.chatService.refreshAllData();
  }

  startChat(user: User) {
    this.chatService.selectUser(user);
    this.router.navigate(['/chat']);
  }
}