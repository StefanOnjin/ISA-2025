import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { VideoService } from '../../services/video.service';

@Component({
  selector: 'app-video-upload',
  templateUrl: './video-upload.component.html',
  styleUrls: ['./video-upload.component.css']
})
export class VideoUploadComponent {
  title: string = '';
  description: string = '';
  tags: string = '';
  location: string = '';
  
  selectedThumbnail: File | null = null;
  selectedVideo: File | null = null;

  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(private videoService: VideoService, private router: Router) {}

  onThumbnailSelected(event: any): void {
    this.selectedThumbnail = event.target.files[0] ?? null;
  }

  onVideoSelected(event: any): void {
    this.selectedVideo = event.target.files[0] ?? null;
  }

  onSubmit(): void {
    if (!this.title || !this.description || !this.selectedThumbnail || !this.selectedVideo) {
      this.errorMessage = 'Title, Description, Thumbnail and Video are required.';
      return;
    }

    const formData = new FormData();
    formData.append('title', this.title);
    formData.append('description', this.description);
    formData.append('tags', this.tags);
    formData.append('location', this.location);
    formData.append('thumbnail', this.selectedThumbnail);
    formData.append('video', this.selectedVideo);

    this.errorMessage = null;
    this.successMessage = null;

    this.videoService.upload(formData).subscribe({
      next: (response) => {
        this.successMessage = 'Video uploaded successfully! Redirecting...';
        setTimeout(() => {
          this.router.navigate(['/home']);
        }, 2000);
      },
      error: (error) => {
        this.errorMessage = `Upload failed: ${error.error?.message || error.message}`;
      }
    });
  }
}