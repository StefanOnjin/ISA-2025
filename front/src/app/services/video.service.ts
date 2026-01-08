import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class VideoService {
  private readonly baseUrl = 'http://localhost:8080/api/videos';
  private readonly tokenKey = 'ra56.jwt';

  constructor(private http: HttpClient) {}

  getAllVideos(): Observable<any[]> {
    return this.http.get<any[]>(this.baseUrl);
  }

  getVideoById(id: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}`);
  }

  upload(formData: FormData): Observable<any> {
    const token = localStorage.getItem(this.tokenKey);
    
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.post(`${this.baseUrl}/upload`, formData, { headers: headers });
  }
}
