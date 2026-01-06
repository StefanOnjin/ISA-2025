import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { RegistrationRequest } from '../models/registration-request';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  successMessage = '';
  errorMessage = '';
  fieldErrors: Record<string, string> = {};

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(8)]],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    address: ['', [Validators.required]]
  });

  constructor(private fb: FormBuilder, private authService: AuthService) {}

  submit(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.fieldErrors = {};

    if (this.form.invalid) {
      return;
    }

    const payload = this.form.value as RegistrationRequest;
    this.authService.register(payload).subscribe({
      next: (response) => {
        this.successMessage = response.message || 'Registration completed.';
        this.form.reset();
      },
      error: (err) => {
        this.fieldErrors = err?.error?.errors ?? {};
        if (Object.keys(this.fieldErrors).length > 0) {
          this.errorMessage = '';
        } else {
          this.errorMessage = err?.error?.message ?? 'Registration failed.';
        }
      }
    });
  }
}
