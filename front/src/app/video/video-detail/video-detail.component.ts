import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { Comment } from '../../models/comment';
import { CommentPage } from '../../models/comment-page';
import { CommentService } from '../../services/comment.service';
import { VideoService } from '../../services/video.service';
import { TranscodingStatus } from '../../models/transcoding-status';
import { WatchPartyRoom } from '../../models/watch-party-room';
import { WatchPartyService } from '../../services/watch-party.service';

declare global {
  interface Window {
    Hls?: any;
  }
}

@Component({
  selector: 'app-video-detail',
  templateUrl: './video-detail.component.html',
  styleUrls: ['./video-detail.component.css']
})
export class VideoDetailComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('videoPlayer')
  set videoPlayerRef(ref: ElementRef<HTMLVideoElement> | undefined) {
    this.playerRef = ref;
    this.setupAdaptivePlayback();
  }
  playerRef?: ElementRef<HTMLVideoElement>;

  video: any;
  videoId: number | null = null;
  errorMessage: string | null = null;
  guestNotice: string | null = null;
  comments: Comment[] = [];
  commentsError: string | null = null;
  commentText = '';
  commentPage: CommentPage | null = null;
  readonly commentPageSize = 10;
  likesCount = 0;
  likedByUser = false;
  likeError: string | null = null;
  private hlsInstance: any = null;
  private viewReady = false;
  transcodingStatus: TranscodingStatus | null = null;
  transcodingProgress = 0;
  transcodingMessage = 'Preparing transcoding...';
  isTranscodingReady = false;
  watchPartyRoom: WatchPartyRoom | null = null;
  watchPartyConnected = false;
  watchPartyError: string | null = null;
  watchPartyStatus: string | null = null;
  private transcodingPollId: ReturnType<typeof setInterval> | null = null;
  private routeSub?: Subscription;
  private watchPartyRoomSub?: Subscription;
  private watchPartyConnectedSub?: Subscription;
  private watchPartyErrorSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    public authService: AuthService,
    private commentService: CommentService,
    private videoService: VideoService,
    private watchPartyService: WatchPartyService
  ) { }

  ngOnInit(): void {
    this.watchPartyRoomSub = this.watchPartyService.activeRoom$.subscribe((room) => {
      this.watchPartyRoom = room;
    });
    this.watchPartyConnectedSub = this.watchPartyService.connected$.subscribe((connected) => {
      this.watchPartyConnected = connected;
    });
    this.watchPartyErrorSub = this.watchPartyService.errors$.subscribe((error) => {
      this.watchPartyError = error;
    });

    this.routeSub = this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (!id) {
        return;
      }

      const parsedId = Number(id);
      if (!Number.isFinite(parsedId) || parsedId <= 0) {
        this.errorMessage = 'Invalid video id.';
        return;
      }

      this.loadVideo(parsedId);
    });
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.setupAdaptivePlayback();
  }

  ngOnDestroy(): void {
    this.destroyPlayers();
    this.stopTranscodingPolling();
    this.routeSub?.unsubscribe();
    this.watchPartyRoomSub?.unsubscribe();
    this.watchPartyConnectedSub?.unsubscribe();
    this.watchPartyErrorSub?.unsubscribe();
  }

  loadComments(page: number): void {
    if (this.videoId === null) {
      return;
    }
    this.commentService.getComments(this.videoId, page, this.commentPageSize).subscribe({
      next: (data) => {
        this.commentPage = data;
        this.comments = data.content;
      },
      error: (error) => {
        this.commentsError = `Failed to load comments: ${error.message}`;
        console.error(error);
      }
    });
  }

  handleGuestAction(): void {
    if (!this.authService.isAuthenticated()) {
      this.guestNotice = 'Sign in to like or comment.';
      return;
    }

    this.guestNotice = null;
  }

  likeVideo(): void {
    if (!this.authService.isAuthenticated() || this.videoId === null) {
      this.handleGuestAction();
      return;
    }

    this.likeError = null;
    this.videoService.likeVideo(this.videoId).subscribe({
      next: (response) => {
        this.likesCount = response.likesCount ?? this.likesCount;
        this.likedByUser = true;
      },
      error: (error) => {
        this.likeError = error?.error?.message ?? 'Failed to like video.';
        console.error(error);
      }
    });
  }

  unlikeVideo(): void {
    if (!this.authService.isAuthenticated() || this.videoId === null) {
      this.handleGuestAction();
      return;
    }

    this.likeError = null;
    this.videoService.unlikeVideo(this.videoId).subscribe({
      next: (response) => {
        this.likesCount = response.likesCount ?? this.likesCount;
        this.likedByUser = false;
      },
      error: (error) => {
        this.likeError = error?.error?.message ?? 'Failed to unlike video.';
        console.error(error);
      }
    });
  }

  submitComment(): void {
    if (!this.authService.isAuthenticated()) {
      this.handleGuestAction();
      return;
    }
    if (!this.commentText.trim() || this.videoId === null) {
      return;
    }

    this.commentService.addComment(this.videoId, this.commentText.trim()).subscribe({
      next: () => {
        this.commentText = '';
        this.loadComments(0);
      },
      error: (error) => {
        if (error?.status === 429) {
          this.commentsError = 'Oladi malo, vidimo se za sat vremena.';
        } else {
          this.commentsError = `Failed to post comment: ${error.message}`;
        }
        console.error(error);
      }
    });
  }

  goToPreviousPage(): void {
    if (!this.commentPage || this.commentPage.number <= 0) {
      return;
    }
    this.loadComments(this.commentPage.number - 1);
  }

  goToNextPage(): void {
    if (!this.commentPage || this.commentPage.number >= this.commentPage.totalPages - 1) {
      return;
    }
    this.loadComments(this.commentPage.number + 1);
  }

  private setupAdaptivePlayback(): void {
    if (!this.viewReady || !this.video || !this.playerRef || !this.isTranscodingReady) {
      return;
    }

    const player = this.playerRef.nativeElement;
    const hlsUrl = this.video?.hlsUrl as string | undefined;

    this.destroyPlayers();

    if (hlsUrl) {
      this.attachHls(player, hlsUrl, () => {
        this.errorMessage = 'Adaptive stream is not available yet. Refresh in a few seconds.';
      });
      return;
    }
    this.errorMessage = 'Adaptive stream URL is missing.';
  }

  syncWatchPartyVideo(): void {
    this.watchPartyStatus = null;
    this.watchPartyError = null;
    if (!this.videoId || !this.watchPartyRoom?.owner) {
      this.watchPartyError = 'Only room owner can sync video.';
      return;
    }

    try {
      this.watchPartyService.publishVideoSelected(this.videoId);
      this.watchPartyStatus = `Synced video to room ${this.watchPartyRoom.roomCode}.`;
    } catch (error) {
      this.watchPartyError = error instanceof Error ? error.message : 'Failed to sync watch party video.';
    }
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

  private destroyPlayers(): void {
    if (this.hlsInstance) {
      this.hlsInstance.destroy();
      this.hlsInstance = null;
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

  private startTranscodingPolling(): void {
    if (!this.videoId) {
      return;
    }

    this.stopTranscodingPolling();

    const load = () => {
      this.videoService.getTranscodingStatus(this.videoId as number).subscribe({
        next: (status) => {
          this.transcodingStatus = status;
          this.transcodingProgress = Math.max(0, Math.min(100, status.progress ?? 0));
          this.transcodingMessage = status.message ?? 'Transcoding in progress.';
          this.isTranscodingReady = status.ready;

          if (status.ready) {
            this.stopTranscodingPolling();
            this.setupAdaptivePlayback();
            return;
          }

          if (status.status === 'FAILED') {
            this.stopTranscodingPolling();
            this.errorMessage = status.message || 'Transcoding failed.';
          }
        },
        error: () => {
          this.transcodingMessage = 'Unable to load transcoding status.';
        }
      });
    };

    load();
    this.transcodingPollId = setInterval(load, 2000);
  }

  private stopTranscodingPolling(): void {
    if (this.transcodingPollId) {
      clearInterval(this.transcodingPollId);
      this.transcodingPollId = null;
    }
  }

  private loadVideo(id: number): void {
    this.videoId = id;
    this.video = null;
    this.errorMessage = null;
    this.commentsError = null;
    this.likeError = null;
    this.watchPartyStatus = null;
    this.stopTranscodingPolling();
    this.isTranscodingReady = false;
    this.transcodingProgress = 0;
    this.transcodingMessage = 'Preparing transcoding...';
    this.destroyPlayers();

    const player = this.playerRef?.nativeElement;
    if (player) {
      player.pause();
      player.removeAttribute('src');
      player.load();
    }

    this.videoService.getVideoById(id).subscribe({
      next: (data) => {
        this.video = data;
        this.likesCount = data.likesCount ?? 0;
        this.likedByUser = !!data.likedByUser;
        this.startTranscodingPolling();
      },
      error: (error) => {
        this.errorMessage = `Failed to load video: ${error.message}`;
        console.error(error);
      }
    });

    this.loadComments(0);
  }
}
