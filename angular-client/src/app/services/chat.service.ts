import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, forkJoin, timer } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { retry, delay, tap } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { Chat } from '../models/Chat';
import { Message } from '../models/Message';
import { User } from '../models/User';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = 'http://localhost:8081';
  private wsUrl = 'ws://localhost:8081/chat-ws';

  private socket$?: WebSocketSubject<any>;

  private connectionStatusSource = new BehaviorSubject<boolean>(true);
  connectionStatus$ = this.connectionStatusSource.asObservable();

  private contactOrderSource = new BehaviorSubject<User[]>([]);
  contactOrder$ = this.contactOrderSource.asObservable();

  private messagesSource = new BehaviorSubject<{ [username: string]: Message[] }>({});
  messages$ = this.messagesSource.asObservable();

  private selectedUserSource = new BehaviorSubject<User | null>(null);
  selectedUser$ = this.selectedUserSource.asObservable();

  constructor() {
    this.authService.currentUser$.subscribe(user => {
      if (user) {
        this.connectWebSocket(user.username);
        this.refreshAllData();
      } else {
        this.closeConnection();
      }
    });
  }

  refreshAllData() {
    const user = this.authService.currentUserValue;
    if (!user) return;

    forkJoin({
      allUsers: this.http.get<User[]>(`${this.apiUrl}/users`),
      rooms: this.http.get<Chat[]>(`${this.apiUrl}/chats`, { params: { username: user.username } })
    }).subscribe({
      next: (data) => {
        this.processRooms(data.rooms, user.username);
        this.processContacts(data.allUsers, data.rooms, user.username);
        this.connectionStatusSource.next(true);
      },
      error: () => this.connectionStatusSource.next(false)
    });
  }

  selectUser(user: User | null) {
    this.selectedUserSource.next(user);
  }

  addMessage(contactName: string, message: Message) {
    if (this.socket$) {
      this.socket$.next({
        targetUsername: contactName,
        text: message.text,
        sender: message.sender
      });
    }
    this.addMessageLocally(contactName, message);
  }

  getMessages(username: string): Message[] {
    return this.messagesSource.value[username] || [];
  }

  private connectWebSocket(username: string) {
    this.closeConnection();
    this.socket$ = webSocket(`${this.wsUrl}/${username}`);

    this.socket$.pipe(
      retry({
        delay: () => {
          this.connectionStatusSource.next(false);
          return timer(5000);
        }
      })
    ).subscribe({
      next: (msg) => {
        this.connectionStatusSource.next(true);
        this.handleIncomingMessage(msg, username);
      },
      error: () => this.connectionStatusSource.next(false)
    });
  }

  private handleIncomingMessage(msg: any, currentUsername: string) {
    if (!msg.sender || !msg.text) return;
    
    const chatPartner = msg.sender === currentUsername ? msg.targetUsername : msg.sender;
    
    if (!chatPartner) return;

    const message: Message = {
      text: msg.text,
      sender: msg.sender,
      timestamp: msg.timestamp || new Date().toISOString(),
      status: 'received'
    };

    if (!this.messagesSource.value[chatPartner]) {
      this.refreshAllData();
    } else {
      this.addMessageLocally(chatPartner, message);
    }
  }

  private processRooms(rooms: Chat[], currentUsername: string) {
    const newStore: { [username: string]: Message[] } = {};
    rooms.forEach(room => {
      const partner = room.participants.find(p => p.username !== currentUsername);
      if (partner) newStore[partner.username] = room.messages;
    });
    this.messagesSource.next(newStore);
  }

  private processContacts(allUsers: User[], rooms: Chat[], currentUsername: string) {
    const sorted = allUsers
      .filter(u => u.username !== currentUsername)
      .sort((a, b) => {
        const timeA = this.getRoomTime(rooms, a.username);
        const timeB = this.getRoomTime(rooms, b.username);
        return timeB - timeA;
      });
    this.contactOrderSource.next(sorted);
  }

  private getRoomTime(rooms: Chat[], username: string): number {
    const room = rooms.find(r => r.participants.some(p => p.username === username));
    return room ? new Date(room.lastMessageTime).getTime() : 0;
  }

  private addMessageLocally(contactName: string, message: Message) {
    const currentStore = { ...this.messagesSource.value };
    const chat = [...(currentStore[contactName] || [])];
    
    if (!chat.some(m => m.timestamp === message.timestamp && m.text === message.text)) {
      currentStore[contactName] = [...chat, message];
      this.messagesSource.next(currentStore);
      this.moveContactToTop(contactName);
    }
  }

  private moveContactToTop(contactName: string) {
    const list = [...this.contactOrderSource.value];
    const index = list.findIndex(u => u.username === contactName);
    if (index > -1) {
      const [contact] = list.splice(index, 1);
      list.unshift(contact);
      this.contactOrderSource.next(list);
    }
  }

  private closeConnection() {
    if (this.socket$) {
      this.socket$.complete();
      this.socket$ = undefined;
    }
    this.messagesSource.next({});
    this.contactOrderSource.next([]);
  }
}