import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, tap } from 'rxjs';
import { User } from '../models/User';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient); 
  
  private apiUrl = 'http://localhost:8081';
  
  private userSource = new BehaviorSubject<User | null>(this.loadFromStorage());
  currentUser$ = this.userSource.asObservable();

  get currentUserValue(): User | null {
    return this.userSource.value;
  }

  getCurrentUserValue(): User | null {
    return this.userSource.value;
  }

  setUser(user: User) {
    return this.http.post(`${this.apiUrl}/sign-on`, user).pipe(
      tap(() => {
        localStorage.setItem('chat_user', JSON.stringify(user));
        this.userSource.next(user);
      })
    );
  }

  logout() {
    localStorage.removeItem('chat_user');
    this.userSource.next(null);
  }

  private loadFromStorage(): User | null {
    const saved = localStorage.getItem('chat_user');
    try {
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  }
}