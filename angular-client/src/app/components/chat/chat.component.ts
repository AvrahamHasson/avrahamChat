import { Component, OnInit, inject, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router'; // הוספת Router לייבוא
import { Subscription } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/User';
import { Message } from '../../models/Message';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements OnInit, OnDestroy {
  private chatService = inject(ChatService);
  private authService = inject(AuthService);
  private router = inject(Router); 
  private cdr = inject(ChangeDetectorRef); 
  private subscriptions = new Subscription();

  currentUser: User | null = null;
  orderedUsers: User[] = [];
  selectedUser: User | null = null;
  newMessage: string = '';
  messages: Message[] = [];
  isConnected: boolean = true;

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUserValue();

    this.subscriptions.add(
      this.authService.currentUser$.subscribe(user => {
        this.currentUser = user;
        this.cdr.detectChanges(); 
      })
    );

    this.subscriptions.add(
      this.chatService.connectionStatus$.subscribe(status => {
        this.isConnected = status;
        this.cdr.detectChanges();
      })
    );

    this.subscriptions.add(
      this.chatService.selectedUser$.subscribe(user => {
        this.selectedUser = user;
        if (user) {
          this.messages = this.chatService.getMessages(user.username);
          this.scrollToBottom();
        }
        this.cdr.detectChanges();
      })
    );

    this.subscriptions.add(
      this.chatService.contactOrder$.subscribe(users => {
        this.orderedUsers = users || [];
        this.cdr.detectChanges();
      })
    );

    this.subscriptions.add(
      this.chatService.messages$.subscribe(store => {
        if (this.selectedUser) {
          this.messages = store[this.selectedUser.username] || [];
          this.scrollToBottom();
        }
        this.cdr.detectChanges();
      })
    );

    this.chatService.refreshAllData();
  }

  selectUser(user: User) {
    this.chatService.selectUser(user);
  }

  sendMessage() {
    const text = this.newMessage?.trim();
    if (!text || !this.currentUser || !this.selectedUser) {
      return;
    }

    const msg: Message = {
      text: text,
      timestamp: new Date().toISOString(),
      sender: this.currentUser.username,
      status: 'sent'
    };
  
    this.chatService.addMessage(this.selectedUser.username, msg);
    this.newMessage = '';
    this.scrollToBottom();
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  getLastMessage(username: string): string {
    const msgs = this.chatService.getMessages(username);
    return msgs.length > 0 ? msgs[msgs.length - 1].text : '';
  }

  onLogout() {
    this.authService.logout();
    this.router.navigate(['/login']); // ניווט חזרה לדף ההתחברות
  }

  private scrollToBottom() {
    setTimeout(() => {
      const container = document.querySelector('.messages-container');
      if (container) container.scrollTop = container.scrollHeight;
    }, 50);
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }
}