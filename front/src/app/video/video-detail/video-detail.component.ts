import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Comment } from '../../models/comment';
import { CommentPage } from '../../models/comment-page';
import { CommentService } from '../../services/comment.service';
import { VideoService } from '../../services/video.service';

@Component({
  selector: 'app-video-detail',
  templateUrl: './video-detail.component.html',
  styleUrls: ['./video-detail.component.css']
})
export class VideoDetailComponent implements OnInit {

  video: any;
  videoId: number | null = null;
  errorMessage: string | null = null;
  guestNotice: string | null = null;
  comments: Comment[] = [];
  commentsError: string | null = null;
  commentText = '';
  commentPage: CommentPage | null = null;
  readonly commentPageSize = 10;

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
        },
        error: (error) => {
          this.errorMessage = `Failed to load video: ${error.message}`;
          console.error(error);
        }
      });
      this.loadComments(0);
    }
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
      this.guestNotice = 'Prijavite se da biste mogli da lajkujete ili komentarisete.';
      return;
    }

    this.guestNotice = null;
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
}
