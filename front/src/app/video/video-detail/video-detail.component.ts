import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Comment } from '../../models/comment';
import { CommentPage } from '../../models/comment-page';
import { CommentService } from '../../services/comment.service';
import { VideoService } from '../../services/video.service';

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

  constructor(
    private route: ActivatedRoute,
    public authService: AuthService,
    private commentService: CommentService,
    private videoService: VideoService
  ) { }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.videoId = +id;
      this.videoService.getVideoById(+id).subscribe({
        next: (data) => {
          this.video = data;
          this.likesCount = data.likesCount ?? 0;
          this.likedByUser = !!data.likedByUser;
        },
        error: (error) => {
          this.errorMessage = `Failed to load video: ${error.message}`;
          console.error(error);
        }
      });
      this.loadComments(0);
    }
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.setupAdaptivePlayback();
  }

  ngOnDestroy(): void {
    this.destroyPlayers();
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
    if (!this.viewReady || !this.video || !this.playerRef) {
      return;
    }

    const player = this.playerRef.nativeElement;
    const hlsUrl = this.video?.hlsUrl as string | undefined;
    const fallbackMp4Url = this.video?.videoUrl as string | undefined;

    this.destroyPlayers();

    if (hlsUrl) {
      this.attachHls(player, hlsUrl, () => this.attachNative(player, fallbackMp4Url));
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
}
