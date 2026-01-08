import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { VideoService } from '../../services/video.service';

@Component({
  selector: 'app-video-detail',
  templateUrl: './video-detail.component.html',
  styleUrls: ['./video-detail.component.css']
})
export class VideoDetailComponent implements OnInit {

  video: any;
  errorMessage: string | null = null;
  guestNotice: string | null = null;

  constructor(
    private route: ActivatedRoute,
    public authService: AuthService,
    private videoService: VideoService
  ) { }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.videoService.getVideoById(+id).subscribe({
        next: (data) => {
          this.video = data;
        },
        error: (error) => {
          this.errorMessage = `Failed to load video: ${error.message}`;
          console.error(error);
        }
      });
    }
  }

  handleGuestAction(): void {
    if (!this.authService.isAuthenticated()) {
      this.guestNotice = 'Prijavite se da biste mogli da lajkujete ili komentarisete.';
      return;
    }

    this.guestNotice = null;
  }
}
