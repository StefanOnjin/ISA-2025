export interface TranscodingStatus {
  videoId: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'NOT_FOUND';
  progress: number;
  ready: boolean;
  message: string;
}
