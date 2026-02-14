export interface PremierDetail {
  id: number;
  title: string;
  description: string;
  thumbnailUrl: string;
  videoUrl: string;
  hlsUrl: string;
  dashUrl: string;
  scheduledAt: string;
  durationSeconds: number;
  streamOffsetSeconds: number;
}
