import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { ChatMessage } from '../models/chat-message';
import { ChatMessageRequest } from '../models/chat-message-request';

@Injectable({
  providedIn: 'root'
})
export class PremierChatService {
  private readonly wsUrl = 'ws://localhost:8080/ws';

  private client: Client | null = null;
  private messageSubscription: StompSubscription | null = null;
  private errorSubscription: StompSubscription | null = null;
  private currentPremierId: number | null = null;

  private readonly messageSubject = new Subject<ChatMessage>();
  readonly messages$ = this.messageSubject.asObservable();

  private readonly connectedSubject = new BehaviorSubject<boolean>(false);
  readonly connected$ = this.connectedSubject.asObservable();

  private readonly errorSubject = new Subject<string>();
  readonly errors$ = this.errorSubject.asObservable();

  connect(premierId: number, token: string | null): void {
    if (this.client?.connected && this.currentPremierId === premierId) {
      return;
    }

    if (this.currentPremierId !== null && this.currentPremierId !== premierId) {
      this.disconnect();
    }

    if (this.client && this.currentPremierId === premierId) {
      return;
    }

    this.currentPremierId = premierId;

    const connectHeaders: Record<string, string> = {};
    if (token) {
      connectHeaders['Authorization'] = `Bearer ${token}`;
    }

    const client = new Client({
      brokerURL: this.wsUrl,
      connectHeaders,
      reconnectDelay: 3000,
      debug: () => undefined,
      onConnect: () => {
        this.connectedSubject.next(true);
        this.messageSubscription = client.subscribe(
          `/topic/premiers/${premierId}/chat`,
          (message: IMessage) => this.handleIncomingMessage(message)
        );

        this.errorSubscription = client.subscribe('/user/queue/errors', (message: IMessage) => {
          const parsed = this.safeParse(message.body) as { message?: string } | null;
          const text = parsed?.message ?? 'Chat error.';
          this.errorSubject.next(text);
        });

        if (token) {
          client.publish({
            destination: `/app/premiers/${premierId}/chat.join`,
            body: '{}'
          });
        }
      },
      onStompError: (frame) => {
        this.errorSubject.next(frame.headers['message'] ?? 'STOMP connection error.');
      },
      onWebSocketClose: () => {
        this.connectedSubject.next(false);
      }
    });

    this.client = client;
    this.client.activate();
  }

  send(premierId: number, text: string): void {
    if (!this.client?.connected || this.currentPremierId !== premierId) {
      throw new Error('Chat connection is not active.');
    }

    const payload: ChatMessageRequest = { text };
    this.client.publish({
      destination: `/app/premiers/${premierId}/chat.send`,
      body: JSON.stringify(payload)
    });
  }

  disconnect(): void {
    this.messageSubscription?.unsubscribe();
    this.messageSubscription = null;

    this.errorSubscription?.unsubscribe();
    this.errorSubscription = null;

    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }

    this.currentPremierId = null;
    this.connectedSubject.next(false);
  }

  private handleIncomingMessage(message: IMessage): void {
    const parsed = this.safeParse(message.body) as ChatMessage | null;
    if (!parsed || !parsed.text || !parsed.senderUsername || !parsed.type || !parsed.sentAt) {
      return;
    }
    this.messageSubject.next(parsed);
  }

  private safeParse(payload: string): unknown | null {
    try {
      return JSON.parse(payload);
    } catch {
      return null;
    }
  }
}
