import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-sign-on',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './sign-on.component.html',
  styleUrl: './sign-on.component.scss'
})
export class SignOnComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);

  loginForm = this.fb.group({
    username: ['', [
      Validators.required, 
      Validators.minLength(3),
      Validators.pattern('^[a-zA-Z\u0590-\u05FF ]+$')
    ]],
    address: ['', [
      Validators.required, 
      Validators.pattern('^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}:[0-9]{1,5}$')
    ]]
  });

  onSubmit() {
      if (!this.loginForm.valid) {return;}
  
      const user = this.loginForm.getRawValue() as { username: string; address: string };
      this.authService.setUser(user).subscribe({
        next: () => {
          this.router.navigate(['/chat']);
        },
        error: (err) => {
          const serverError = err.error?.error;
          const connectionError = err.status === 0 ? 'Server is unreachable' : 'Connection failed';
          alert(serverError || connectionError);
          console.error('Login error details:', err);
        }
      });
    
  }
}