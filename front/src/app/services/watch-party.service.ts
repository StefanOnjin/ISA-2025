import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject, tap } from 'rxjs';
import { WatchPartyCreateResponse } from '../models/watch-party-create-response';
import { WatchPartyRoom } from '../models/watch-party-room';
import { WatchPartySyncMessage } from '../models/watch-party-sync-message';

@Injectable({
  providedIn: 'root'
})
export class WatchPartyService {
  private readonly baseUrl = 'http://localhost:8080/api/watch-party';
  private readonly wsUrl = 'ws://localhost:8080/ws';
  private readonly tokenKey = 'ra56.jwt';

  private client: Client | null = null;
  private roomSubscription: StompSubscription | null = null;
  private errorSubscription: StompSubscription | null = null;
  private currentRoomCode: string | null = null;

  private readonly activeRoomSubject = new BehaviorSubject<WatchPartyRoom | null>(null);
  readonly activeRoom$ = this.activeRoomSubject.asObservable();

  private readonly syncSubject = new Subject<WatchPartySyncMessage>();
  readonly syncEvents$ = this.syncSubject.asObservable();

  private readonly connectedSubject = new BehaviorSubject<boolean>(false);
  readonly connected$ = this.connectedSubject.asObservable();

  private readonly errorSubject = new Subject<string>();
  readonly errors$ = this.errorSubject.asObservable();

  constructor(private http: HttpClient) {}

  createRoom(): Observable<WatchPartyCreateResponse> {
    return this.http.post<WatchPartyCreateResponse>(`${this.baseUrl}/rooms`, {}, this.withAuth()).pipe(
      tap((response) => this.connectToRoom(response.roomCode))
    );
  }

  joinRoom(roomCode: string): Observable<WatchPartyRoom> {
    return this.http.post<WatchPartyRoom>(
      `${this.baseUrl}/rooms/${encodeURIComponent(roomCode)}/join`,
      {},
      this.withAuth()
    ).pipe(tap((room) => this.connectToRoom(room.roomCode)));
  }

  getRoom(roomCode: string): Observable<WatchPartyRoom> {
    return this.http.get<WatchPartyRoom>(
      `${this.baseUrl}/rooms/${encodeURIComponent(roomCode)}`,
      this.withAuth()
    ).pipe(tap((room) => this.activeRoomSubject.next(room)));
  }

  publishVideoSelected(videoId: number): void {
    const roomCode = this.currentRoomCode;
    if (!this.client?.connected || !roomCode) {
      throw new Error('Watch party connection is not active.');
    }
    this.client.publish({
      destination: `/app/watch-party/${roomCode}/play`,
      body: JSON.stringify({ videoId })
    });
  }

  disconnect(): void {
    this.roomSubscription?.unsubscribe();
    this.roomSubscription = null;

    this.errorSubscription?.unsubscribe();
    this.errorSubscription = null;

    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }

    this.currentRoomCode = null;
    this.connectedSubject.next(false);
    this.activeRoomSubject.next(null);
  }

  private connectToRoom(roomCode: string): void {
    const normalizedRoomCode = roomCode.trim().toUpperCase();
    const token = localStorage.getItem(this.tokenKey);
    if (!token) {
      throw new Error('You must be logged in to use Watch Party.');
    }

    if (this.client?.connected && this.currentRoomCode === normalizedRoomCode) {
      return;
    }

    if (this.currentRoomCode && this.currentRoomCode !== normalizedRoomCode) {
      this.disconnect();
    }

    this.currentRoomCode = normalizedRoomCode;

    const client = new Client({
      brokerURL: this.wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 3000,
      debug: () => undefined,
      onConnect: () => {
        this.connectedSubject.next(true);
        this.roomSubscription = client.subscribe(
          `/topic/watch-party/${normalizedRoomCode}`,
          (message: IMessage) => this.handleSyncMessage(message)
        );
        this.errorSubscription = client.subscribe('/user/queue/errors', (message: IMessage) => {
          const payload = this.safeParse(message.body) as { message?: string } | null;
          this.errorSubject.next(payload?.message ?? 'Watch party error.');
        });
      },
      onStompError: (frame) => {
        this.errorSubject.next(frame.headers['message'] ?? 'Watch party connection error.');
      },
      onWebSocketClose: () => {
        this.connectedSubject.next(false);
      }
    });

    this.client = client;
    this.client.activate();
  }

  private handleSyncMessage(message: IMessage): void {
    const payload = this.safeParse(message.body) as WatchPartySyncMessage | null;
    if (!payload || !payload.videoId || !payload.roomCode) {
      return;
    }
    this.syncSubject.next(payload);
  }

  private withAuth(): { headers: HttpHeaders } {
    const token = localStorage.getItem(this.tokenKey);
    if (!token) {
      throw new Error('You must be logged in.');
    }
    return {
      headers: new HttpHeaders({
        Authorization: `Bearer ${token}`
      })
    };
  }

  private safeParse(raw: string): unknown | null {
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }
}
