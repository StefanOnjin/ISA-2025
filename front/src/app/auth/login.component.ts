import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { LoginRequest } from '../models/login-request';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  successMessage = '';
  errorMessage = '';
  fieldErrors: Record<string, string> = {};

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  submit(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.fieldErrors = {};

    if (this.form.invalid) {
      return;
    }

    const payload = this.form.value as LoginRequest;
    this.authService.login(payload).subscribe({
      next: () => {
        this.successMessage = 'Login successful.';
        this.form.reset();
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Login failed.';
        this.fieldErrors = err?.error?.errors ?? {};
      }
    });
  }
}
