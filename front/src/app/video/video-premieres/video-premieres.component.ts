import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PremiereVideo } from '../../models/premiere-video';
import { VideoService } from '../../services/video.service';

interface PremiereCard extends PremiereVideo {
  isLive: boolean;
  countdownLabel: string;
}

@Component({
  selector: 'app-video-premieres',
  templateUrl: './video-premieres.component.html',
  styleUrls: ['./video-premieres.component.css']
})
export class VideoPremieresComponent implements OnInit, OnDestroy {
  premieres: PremiereCard[] = [];
  errorMessage: string | null = null;
  private tickTimer: number | null = null;
  private reloadTimer: number | null = null;

  constructor(private videoService: VideoService, private router: Router) {}

  ngOnInit(): void {
    this.loadPremieres();
    this.tickTimer = window.setInterval(() => this.refreshCardStates(), 1000);
    this.reloadTimer = window.setInterval(() => this.loadPremieres(), 30000);
  }

  ngOnDestroy(): void {
    if (this.tickTimer !== null) {
      window.clearInterval(this.tickTimer);
      this.tickTimer = null;
    }
    if (this.reloadTimer !== null) {
      window.clearInterval(this.reloadTimer);
      this.reloadTimer = null;
    }
  }

  openPremiere(card: PremiereCard): void {
    if (!card.isLive) {
      return;
    }
    this.router.navigate(['/premiers', card.id]);
  }

  private loadPremieres(): void {
    this.videoService.getPremieres().subscribe({
      next: (items) => {
        this.errorMessage = null;
        this.premieres = items
          .map((item) => this.mapToCard(item))
          .filter((item) => this.isWithinPremiereWindow(item));
      },
      error: (error) => {
        this.errorMessage = `Failed to load premieres: ${error.message}`;
      }
    });
  }

  private refreshCardStates(): void {
    const now = Date.now();
    this.premieres = this.premieres.filter((item) => this.isWithinPremiereWindow(item));
    for (const item of this.premieres) {
      const scheduled = new Date(item.scheduledAt).getTime();
      const durationMs = Math.max(1, item.durationSeconds) * 1000;
      const startsInMs = scheduled - now;
      item.isLive = startsInMs <= 0 && now <= scheduled + durationMs;
      item.countdownLabel = item.isLive ? 'Premiere is live' : this.formatStartsIn(startsInMs);
    }
  }

  trackByPremiereId(_: number, item: PremiereCard): number {
    return item.id;
  }

  private mapToCard(item: PremiereVideo): PremiereCard {
    const now = Date.now();
    const scheduled = new Date(item.scheduledAt).getTime();
    const durationMs = Math.max(1, item.durationSeconds) * 1000;
    const startsInMs = scheduled - now;
    const isLive = startsInMs <= 0 && now <= scheduled + durationMs;
    return {
      ...item,
      isLive,
      countdownLabel: isLive ? 'Premiere is live' : this.formatStartsIn(startsInMs)
    };
  }

  private isWithinPremiereWindow(item: PremiereVideo): boolean {
    const now = Date.now();
    const scheduled = new Date(item.scheduledAt).getTime();
    const durationMs = Math.max(1, item.durationSeconds) * 1000;
    return now <= scheduled + durationMs;
  }

  private formatStartsIn(startsInMs: number): string {
    if (startsInMs <= 0) {
      return 'Starting soon';
    }
    const totalSeconds = Math.ceil(startsInMs / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    if (hours > 0) {
      return `Starts in ${hours}h ${minutes}m`;
    }
    if (minutes > 0) {
      return `Starts in ${minutes}m ${seconds}s`;
    }
    return `Starts in ${seconds}s`;
  }
}
