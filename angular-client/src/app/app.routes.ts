import { Routes } from '@angular/router';
import { LandingPageComponent } from './components/landing-page/landing-page.component';
import { SignOnComponent } from './components/sign-on/sign-on.component';
import { ChatComponent } from './components/chat/chat.component';
import { UserListComponent } from './components/user-list/user-list.component';

import { authGuard } from './guards/auth.guard';
import { loginGuard } from './guards/login.guard';

export const routes: Routes = [
  { path: '', component: LandingPageComponent, canActivate: [loginGuard] },
  { path: 'login', component: SignOnComponent, canActivate: [loginGuard] },
  { path: 'sign-on', redirectTo: 'login' }, 
  { path: 'chat', component: ChatComponent, canActivate: [authGuard] },
  { path: 'users', component: UserListComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' } 
];