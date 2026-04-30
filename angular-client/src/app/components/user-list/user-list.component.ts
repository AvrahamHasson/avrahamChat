import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/User';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-list.component.html',
  styleUrl: './user-list.component.scss'
})
export class UserListComponent implements OnInit, OnDestroy {
  private chatService = inject(ChatService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef); 

  users: User[] = [];
  currentUser: User | null = null;
  private subscriptions = new Subscription();

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUserValue();

    this.subscriptions.add(
      this.chatService.contactOrder$.subscribe(data => {
        this.users = data;
        this.cdr.detectChanges(); 
      })
    );

    this.chatService.refreshAllData();
  }

  startChat(user: User) {
    this.chatService.selectUser(user);
    this.router.navigate(['/chat']);
  }

  ngOnDestroy() {
    this.subscriptions.unsubscribe();
  }
}