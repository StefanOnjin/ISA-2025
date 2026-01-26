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
    const token = localStorage.getItem(this.tokenKey);
    const options = token
      ? { headers: new HttpHeaders({ 'Authorization': `Bearer ${token}` }) }
      : {};

    return this.http.get<any>(`${this.baseUrl}/${id}`, options);
  }

  likeVideo(id: number): Observable<any> {
    const token = localStorage.getItem(this.tokenKey);
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.post(`${this.baseUrl}/${id}/like`, {}, { headers: headers });
  }

  unlikeVideo(id: number): Observable<any> {
    const token = localStorage.getItem(this.tokenKey);
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.delete(`${this.baseUrl}/${id}/like`, { headers: headers });
  }

  upload(formData: FormData): Observable<any> {
    const token = localStorage.getItem(this.tokenKey);
    
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.post(`${this.baseUrl}/upload`, formData, { headers: headers });
  }

  getMapVideos(minLat: number, maxLat: number, minLng: number, maxLng: number, zoom: number): Observable<any[]> {
    return this.http.get<any[]>(
      `http://localhost:8080/api/video-map/points?minLat=${minLat}&maxLat=${maxLat}&minLng=${minLng}&maxLng=${maxLng}&zoom=${zoom}`
    );
  }
}
