import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PremierDetail } from '../../models/premier-detail';
import { VideoService } from '../../services/video.service';

@Component({
  selector: 'app-video-premier',
  templateUrl: './video-premier.component.html',
  styleUrls: ['./video-premier.component.css']
})
export class VideoPremierComponent implements OnInit, OnDestroy {
  @ViewChild('premierPlayer') playerRef?: ElementRef<HTMLVideoElement>;

  premier: PremierDetail | null = null;
  errorMessage: string | null = null;
  isLive = false;
  startsInLabel = '';
  private maxReachedTime = 0;
  private initialOffset = 0;
  private syncTimer: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private videoService: VideoService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.errorMessage = 'Premier id is missing.';
      return;
    }
    const id = Number(idParam);
    this.videoService.getPremierById(id).subscribe({
      next: (data) => {
        this.premier = data;
        this.initialOffset = data.streamOffsetSeconds ?? 0;
        this.refreshLiveState();
        this.startSyncLoop();
      },
      error: (error) => {
        this.errorMessage = error?.error?.message ?? 'Premier is not available.';
      }
    });
  }

  ngOnDestroy(): void {
    if (this.syncTimer !== null) {
      window.clearInterval(this.syncTimer);
      this.syncTimer = null;
    }
  }

  onMetadataLoaded(): void {
    const player = this.playerRef?.nativeElement;
    if (!player || !this.premier) {
      return;
    }
    const duration = player.duration;
    if (!isFinite(duration) || duration <= 0) {
      return;
    }
    const target = Math.max(0, Math.min(this.getExpectedOffsetSeconds(), Math.max(0, duration - 0.5)));
    player.currentTime = target;
    this.maxReachedTime = target;
    void player.play().catch(() => {});
  }

  onTimeUpdate(): void {
    const player = this.playerRef?.nativeElement;
    if (!player || !this.isLive) {
      return;
    }

    const expected = this.getExpectedOffsetSeconds();
    if (Math.abs(player.currentTime - expected) > 2.0) {
      player.currentTime = expected;
    }

    if (player.currentTime > this.maxReachedTime) {
      this.maxReachedTime = player.currentTime;
    }
  }

  onSeeking(): void {
    const player = this.playerRef?.nativeElement;
    if (!player) {
      return;
    }
    if (player.currentTime < this.maxReachedTime - 0.8) {
      player.currentTime = this.maxReachedTime;
    }
  }

  backToPremieres(): void {
    this.router.navigate(['/premieres']);
  }

  private startSyncLoop(): void {
    if (this.syncTimer !== null) {
      window.clearInterval(this.syncTimer);
    }

    this.syncTimer = window.setInterval(() => {
      this.refreshLiveState();
      const player = this.playerRef?.nativeElement;
      if (!player || !this.isLive) {
        return;
      }

      const expected = this.getExpectedOffsetSeconds();
      if (Math.abs(player.currentTime - expected) > 2.0) {
        player.currentTime = expected;
      }

      if (player.paused) {
        void player.play().catch(() => {});
      }
    }, 1000);
  }

  private refreshLiveState(): void {
    if (!this.premier) {
      this.isLive = false;
      this.startsInLabel = '';
      return;
    }

    const scheduledTimestamp = new Date(this.premier.scheduledAt).getTime();
    const now = Date.now();
    const startsInMs = scheduledTimestamp - now;
    this.isLive = startsInMs <= 0;
    this.startsInLabel = this.formatStartsIn(startsInMs);
  }

  private getExpectedOffsetSeconds(): number {
    if (!this.premier) {
      return 0;
    }
    const scheduledTimestamp = new Date(this.premier.scheduledAt).getTime();
    const now = Date.now();
    const expected = Math.floor((now - scheduledTimestamp) / 1000);
    return Math.max(this.initialOffset, Math.max(0, expected));
  }

  private formatStartsIn(startsInMs: number): string {
    if (startsInMs <= 0) {
      return 'Premiere is live';
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
