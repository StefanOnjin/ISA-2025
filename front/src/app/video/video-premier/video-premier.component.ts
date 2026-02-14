import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PremierDetail } from '../../models/premier-detail';
import { VideoService } from '../../services/video.service';

declare global {
  interface Window {
    Hls?: any;
    dashjs?: any;
  }
}

@Component({
  selector: 'app-video-premier',
  templateUrl: './video-premier.component.html',
  styleUrls: ['./video-premier.component.css']
})
export class VideoPremierComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('premierPlayer')
  set premierPlayerRef(ref: ElementRef<HTMLVideoElement> | undefined) {
    this.playerRef = ref;
    this.setupAdaptivePlayback();
  }
  playerRef?: ElementRef<HTMLVideoElement>;

  premier: PremierDetail | null = null;
  errorMessage: string | null = null;
  isLive = false;
  startsInLabel = '';
  private maxReachedTime = 0;
  private initialOffset = 0;
  private syncTimer: number | null = null;
  private hlsInstance: any = null;
  private dashInstance: any = null;
  private viewReady = false;

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
        this.setupAdaptivePlayback();
      },
      error: (error) => {
        this.errorMessage = error?.error?.message ?? 'Premier is not available.';
      }
    });
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.setupAdaptivePlayback();
  }

  ngOnDestroy(): void {
    if (this.syncTimer !== null) {
      window.clearInterval(this.syncTimer);
      this.syncTimer = null;
    }
    this.destroyPlayers();
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
      this.setupAdaptivePlayback();
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

  private setupAdaptivePlayback(): void {
    if (!this.viewReady || !this.isLive || !this.premier || !this.playerRef) {
      return;
    }

    const player = this.playerRef.nativeElement;
    const hlsUrl = this.premier.hlsUrl;
    const dashUrl = this.premier.dashUrl;
    const fallbackMp4Url = this.premier.videoUrl;
    const currentSrc = player.currentSrc || player.src;

    if (currentSrc) {
      return;
    }

    this.destroyPlayers();

    if (hlsUrl) {
      this.attachHls(player, hlsUrl, () => this.attachDashOrFallback(player, dashUrl, fallbackMp4Url));
      return;
    }

    this.attachDashOrFallback(player, dashUrl, fallbackMp4Url);
  }

  private attachDashOrFallback(player: HTMLVideoElement, dashUrl?: string, fallbackMp4Url?: string): void {
    if (dashUrl) {
      this.attachDash(player, dashUrl, () => this.attachNative(player, fallbackMp4Url));
      return;
    }
    this.attachNative(player, fallbackMp4Url);
  }

  private attachHls(player: HTMLVideoElement, hlsUrl: string, onFail: () => void): void {
    let failedOver = false;
    const failOver = () => {
      if (failedOver) {
        return;
      }
      failedOver = true;
      onFail();
    };

    if (player.canPlayType('application/vnd.apple.mpegurl')) {
      player.src = hlsUrl;
      return;
    }

    const init = () => {
      if (!window.Hls?.isSupported?.()) {
        onFail();
        return;
      }
      const hls = new window.Hls();
      hls.on(window.Hls.Events.ERROR, (_event: any, data: any) => {
        if (data?.fatal) {
          hls.destroy();
          this.hlsInstance = null;
          failOver();
        }
      });
      hls.loadSource(hlsUrl);
      hls.attachMedia(player);
      this.hlsInstance = hls;
    };

    if (window.Hls) {
      init();
      return;
    }

    this.loadScript('https://cdn.jsdelivr.net/npm/hls.js@1').then(init).catch(failOver);
  }

  private attachDash(player: HTMLVideoElement, dashUrl: string, onFail: () => void): void {
    let failedOver = false;
    const failOver = () => {
      if (failedOver) {
        return;
      }
      failedOver = true;
      onFail();
    };

    const init = () => {
      if (!window.dashjs?.MediaPlayer) {
        failOver();
        return;
      }
      const dashPlayer = window.dashjs.MediaPlayer().create();
      dashPlayer.on('error', () => {
        dashPlayer.reset();
        this.dashInstance = null;
        failOver();
      });
      dashPlayer.initialize(player, dashUrl, true);
      this.dashInstance = dashPlayer;
    };

    if (window.dashjs) {
      init();
      return;
    }

    this.loadScript('https://cdn.jsdelivr.net/npm/dashjs@4/dist/dash.all.min.js').then(init).catch(failOver);
  }

  private attachNative(player: HTMLVideoElement, src?: string): void {
    if (!src) {
      return;
    }
    player.src = src;
    player.load();
  }

  private destroyPlayers(): void {
    if (this.hlsInstance) {
      this.hlsInstance.destroy();
      this.hlsInstance = null;
    }
    if (this.dashInstance) {
      this.dashInstance.reset();
      this.dashInstance = null;
    }
  }

  private loadScript(src: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const existing = document.querySelector(`script[src="${src}"]`) as HTMLScriptElement | null;
      if (existing) {
        if (existing.getAttribute('data-loaded') === 'true') {
          resolve();
          return;
        }
        existing.addEventListener('load', () => resolve(), { once: true });
        existing.addEventListener('error', () => reject(new Error(`Failed to load script: ${src}`)), { once: true });
        return;
      }

      const script = document.createElement('script');
      script.src = src;
      script.async = true;
      script.addEventListener('load', () => {
        script.setAttribute('data-loaded', 'true');
        resolve();
      }, { once: true });
      script.addEventListener('error', () => reject(new Error(`Failed to load script: ${src}`)), { once: true });
      document.body.appendChild(script);
    });
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
