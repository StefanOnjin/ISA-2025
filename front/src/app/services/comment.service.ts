import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CommentPage } from '../models/comment-page';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class CommentService {
  private readonly baseUrl = 'http://localhost:8080/api/videos';

  constructor(private http: HttpClient, private authService: AuthService) {}

  getComments(videoId: number, page: number, size: number): Observable<CommentPage> {
    return this.http.get<CommentPage>(`${this.baseUrl}/${videoId}/comments?page=${page}&size=${size}`);
  }

  addComment(videoId: number, text: string): Observable<any> {
    const token = this.authService.getToken();
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.post(`${this.baseUrl}/${videoId}/comments`, { text }, { headers });
  }
}
