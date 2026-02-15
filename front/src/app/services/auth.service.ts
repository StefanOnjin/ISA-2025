import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthResponse } from '../models/auth-response';
import { AuthTokenResponse } from '../models/auth-token-response';
import { LoginRequest } from '../models/login-request';
import { RegistrationRequest } from '../models/registration-request';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/auth';
  private readonly monitoringBaseUrl = 'http://localhost:8080/api/monitoring';
  private readonly tokenKey = 'ra56.jwt';

  constructor(private http: HttpClient) {}

  register(payload: RegistrationRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, payload);
  }

  login(payload: LoginRequest): Observable<AuthTokenResponse> {
    return this.http.post<AuthTokenResponse>(`${this.baseUrl}/login`, payload).pipe(
      tap((response) => {
        localStorage.setItem(this.tokenKey, response.accessToken);
        this.sendHeartbeat(response.accessToken);
      })
    );
  }

  private sendHeartbeat(token: string): void {
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

    this.http.post(`${this.monitoringBaseUrl}/heartbeat`, {}, { headers }).subscribe({
      error: () => {
        // Heartbeat is best-effort and must not affect login flow.
      }
    });
  }

  private sendLogoutSignal(token: string): void {
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

    this.http.post(`${this.monitoringBaseUrl}/logout`, {}, { headers }).subscribe({
      error: () => {
        // Logout signal is best-effort and must not block client logout.
      }
    });
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  logout(): void {
    const token = this.getToken();
    if (token) {
      this.sendLogoutSignal(token);
    }
    localStorage.removeItem(this.tokenKey);
  }
}
