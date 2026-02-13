import { Component, OnInit } from '@angular/core';
import { VideoService } from '../../services/video.service';

@Component({
  selector: 'app-video-list',
  templateUrl: './video-list.component.html',
  styleUrls: ['./video-list.component.css']
})
export class VideoListComponent implements OnInit {
  
  videos: any[] = [];
  errorMessage: string | null = null;

  constructor(private videoService: VideoService) { }

  ngOnInit(): void {
    this.videoService.getAllVideos().subscribe({
      next: (data) => {
        const now = Date.now();
        this.videos = data.filter((video) => this.isVideoAvailableOnHome(video, now));
      },
      error: (error) => {
        this.errorMessage = `Failed to load videos: ${error.message}`;
        console.error(error);
      }
    });
  }

  private isVideoAvailableOnHome(video: any, nowTimestamp: number): boolean {
    if (!video?.premiereEnabled) {
      return true;
    }

    const scheduledAt = video?.scheduledAt ? new Date(video.scheduledAt) : null;
    const durationSeconds = Number(video?.durationSeconds ?? 0);

    if (!scheduledAt || isNaN(scheduledAt.getTime()) || durationSeconds <= 0) {
      return false;
    }

    const endTimestamp = scheduledAt.getTime() + durationSeconds * 1000;
    return endTimestamp <= nowTimestamp;
  }

  getVideoLink(video: any): string[] {
    if (!video?.id) {
      return ['/home'];
    }

    if (!video?.premiereEnabled) {
      return ['/videos', String(video.id)];
    }

    const scheduledAt = video.scheduledAt ? new Date(video.scheduledAt) : null;
    const durationSeconds = Number(video.durationSeconds ?? 0);

    if (!scheduledAt || isNaN(scheduledAt.getTime()) || durationSeconds <= 0) {
      return ['/videos', String(video.id)];
    }

    const endTimestamp = scheduledAt.getTime() + durationSeconds * 1000;
    if (Date.now() < endTimestamp) {
      return ['/premiers', String(video.id)];
    }

    return ['/videos', String(video.id)];
  }
}
