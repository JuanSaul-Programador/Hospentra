import { Component, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private cooldownInterval: any = null;
  private verificationCompleted = false;

  readonly step = signal<'FORM' | 'OTP'>('FORM');
  readonly registeredEmail = signal<string>('');
  readonly registeredName = signal<string>('');
  readonly otpDigits = signal<string[]>(['', '', '', '', '', '']);

  readonly isSubmitting = signal(false);
  readonly isVerifying = signal(false);
  readonly isResending = signal(false);
  readonly resendCooldown = signal(0);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly registerForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern(/^9\d{8}$/)]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  ngOnDestroy(): void {
    if (this.cooldownInterval) {
      clearInterval(this.cooldownInterval);
    }
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const formValue = { ...this.registerForm.value };
    if (formValue.phone) {
      formValue.phone = `+51 ${formValue.phone}`;
    }

    this.authService.register(formValue).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.registeredEmail.set(res.email || formValue.email);
        this.registeredName.set(formValue.firstName);
        if (res.verificationRequired) {
          this.step.set('OTP');
          this.startCooldown(60);
          this.successMessage.set('Código de seguridad enviado a su correo.');
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        if (err.status === 409) {
          this.errorMessage.set('El nombre de usuario o el correo electrónico ya se encuentran registrados.');
        } else if (err.error?.detail) {
          this.errorMessage.set(err.error.detail);
        } else {
          this.errorMessage.set('No se pudo completar el registro. Intente nuevamente.');
        }
      }
    });
  }

  onDigitInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const val = input.value.replace(/\D/g, '').slice(-1);
    const updated = [...this.otpDigits()];
    updated[index] = val;
    this.otpDigits.set(updated);
    input.value = val;

    if (val && index < 5) {
      const nextInput = document.getElementById(`otp-input-${index + 1}`) as HTMLInputElement;
      if (nextInput) nextInput.focus();
    }

    const fullCode = updated.join('');
    if (fullCode.length === 6 && !updated.includes('')) {
      this.verifyOtpCode(fullCode);
    }
  }

  onKeyDown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace' && !this.otpDigits()[index] && index > 0) {
      const prevInput = document.getElementById(`otp-input-${index - 1}`) as HTMLInputElement;
      if (prevInput) {
        prevInput.focus();
      }
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pasted = event.clipboardData?.getData('text') || '';
    const digits = pasted.replace(/\D/g, '').slice(0, 6).split('');
    if (digits.length > 0) {
      const updated = ['', '', '', '', '', ''];
      digits.forEach((d, i) => {
        if (i < 6) updated[i] = d;
      });
      this.otpDigits.set(updated);
      const focusIndex = Math.min(digits.length, 5);
      const targetInput = document.getElementById(`otp-input-${focusIndex}`) as HTMLInputElement;
      if (targetInput) targetInput.focus();

      if (digits.length === 6) {
        this.verifyOtpCode(digits.join(''));
      }
    }
  }

  submitOtp(): void {
    const code = this.otpDigits().join('');
    if (code.length !== 6) {
      this.errorMessage.set('Por favor ingrese el código completo de 6 dígitos.');
      return;
    }
    this.verifyOtpCode(code);
  }

  private verifyOtpCode(code: string): void {
    if (this.isVerifying() || this.verificationCompleted) {
      return;
    }
    this.isVerifying.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.verifyEmail(this.registeredEmail(), code).subscribe({
      next: () => {
        this.verificationCompleted = true;
        this.isVerifying.set(false);
        this.successMessage.set('¡Cuenta verificada exitosamente! Accediendo a Hospentra...');
        setTimeout(() => {
          this.router.navigate(['/']);
        }, 1000);
      },
      error: (err) => {
        if (this.verificationCompleted) {
          return;
        }
        this.isVerifying.set(false);
        if (err.error?.detail) {
          this.errorMessage.set(err.error.detail);
        } else {
          this.errorMessage.set('El código de verificación es inválido o ha expirado.');
        }
      }
    });
  }

  resendOtp(): void {
    if (this.resendCooldown() > 0 || this.isResending()) return;

    this.isResending.set(true);
    this.errorMessage.set(null);

    this.authService.resendVerification(this.registeredEmail()).subscribe({
      next: () => {
        this.isResending.set(false);
        this.startCooldown(60);
        this.successMessage.set('Hemos reenviado un nuevo código de seguridad a su correo.');
      },
      error: (err) => {
        this.isResending.set(false);
        if (err.error?.detail) {
          this.errorMessage.set(err.error.detail);
        } else {
          this.errorMessage.set('No se pudo reenviar el código. Intente en unos minutos.');
        }
      }
    });
  }

  backToForm(): void {
    this.verificationCompleted = false;
    this.step.set('FORM');
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  private startCooldown(seconds: number): void {
    this.resendCooldown.set(seconds);
    if (this.cooldownInterval) clearInterval(this.cooldownInterval);
    this.cooldownInterval = setInterval(() => {
      const current = this.resendCooldown();
      if (current <= 1) {
        clearInterval(this.cooldownInterval);
        this.resendCooldown.set(0);
      } else {
        this.resendCooldown.set(current - 1);
      }
    }, 1000);
  }
}
