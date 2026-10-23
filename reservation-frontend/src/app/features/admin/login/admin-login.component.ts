import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.scss'
})
export class AdminLoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loginForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal<boolean>(false);

  ngOnInit(): void {
    this.authService.logout();
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(prev => !prev);
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const credentials = {
      username: (this.loginForm.value.username || '').trim(),
      password: this.loginForm.value.password || ''
    };

    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        if (response.role === 'ROLE_ADMIN' || response.role === 'ROLE_RECEPTIONIST') {
          this.router.navigate(['/admin/dashboard']);
        } else {
          this.authService.logout();
          this.errorMessage.set('Acceso denegado: Esta cuenta de huésped no tiene permisos para el panel de gestión.');
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 401 || err.status === 403) {
          this.errorMessage.set('Credenciales inválidas. Verifique su usuario y contraseña corporativa.');
        } else {
          this.errorMessage.set('Error al conectar con el servidor. Intente nuevamente.');
        }
      }
    });
  }
}
