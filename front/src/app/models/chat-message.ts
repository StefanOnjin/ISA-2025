export interface ChatMessage {
  type: 'CHAT' | 'SYSTEM';
  senderUsername: string;
  text: string;
  sentAt: string;
}
