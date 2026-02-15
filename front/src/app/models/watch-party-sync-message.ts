export interface WatchPartySyncMessage {
  type: string;
  roomCode: string;
  videoId: number;
  senderUsername: string;
  sentAt: string;
}
