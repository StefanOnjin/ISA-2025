import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PopularityTop3 } from '../models/popularity-top3';

@Injectable({
  providedIn: 'root'
})
export class PopularityService {
  private readonly baseUrl = 'http://localhost:8080/api/popularity';
  private readonly tokenKey = 'ra56.jwt';

  constructor(private http: HttpClient) {}

  getLatestTop3(): Observable<PopularityTop3> {
    const token = localStorage.getItem(this.tokenKey);
    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
    return this.http.get<PopularityTop3>(`${this.baseUrl}/latest`, { headers });
  }
}
