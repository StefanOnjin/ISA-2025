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
        this.videos = data;
      },
      error: (error) => {
        this.errorMessage = `Failed to load videos: ${error.message}`;
        console.error(error);
      }
    });
  }
}