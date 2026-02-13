import { Component, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-video-upload',
  templateUrl: './video-upload.component.html',
  styleUrls: ['./video-upload.component.css']
})
export class VideoUploadComponent implements OnDestroy {
  title: string = '';
  description: string = '';
  tags: string = '';
  scheduledAt: string = '';
  latitude: number | null = null;
  longitude: number | null = null;
  durationSeconds: number | null = null;
  
  selectedThumbnail: File | null = null;
  selectedVideo: File | null = null;

  errorMessage: string | null = null;
  successMessage: string | null = null;

  showMap: boolean = false;
  private map: L.Map | null = null;
  private selectionLayer: L.CircleMarker | null = null;

  constructor(private videoService: VideoService, private router: Router) {}

  onThumbnailSelected(event: any): void {
    this.selectedThumbnail = event.target.files[0] ?? null;
  }

  onVideoSelected(event: any): void {
    this.selectedVideo = event.target.files[0] ?? null;
    this.durationSeconds = null;
    if (!this.selectedVideo) {
      return;
    }

    this.loadDurationSeconds(this.selectedVideo)
      .then((duration) => {
        this.durationSeconds = duration;
      })
      .catch(() => {
        this.selectedVideo = null;
        this.errorMessage = 'Cannot read video duration. Please choose a valid MP4 file.';
      });
  }

  onSubmit(): void {
    if (!this.title || !this.description || !this.selectedThumbnail || !this.selectedVideo) {
      this.errorMessage = 'Title, Description, Thumbnail and Video are required.';
      return;
    }

    if (this.latitude === null || this.longitude === null) {
      this.errorMessage = 'Location coordinates are required.';
      return;
    }

    if (this.durationSeconds === null || this.durationSeconds <= 0) {
      this.errorMessage = 'Cannot determine video duration.';
      return;
    }

    if (this.scheduledAt) {
      const scheduledDate = new Date(this.scheduledAt);
      if (isNaN(scheduledDate.getTime())) {
        this.errorMessage = 'Invalid scheduled date format.';
        return;
      }
      if (scheduledDate.getTime() < Date.now()) {
        this.errorMessage = 'Scheduled date cannot be in the past.';
        return;
      }
    }

    const formData = new FormData();
    formData.append('title', this.title);
    formData.append('description', this.description);
    formData.append('tags', this.tags);
    if (this.scheduledAt) {
      formData.append('scheduledAt', this.scheduledAt);
    }
    formData.append('durationSeconds', Math.max(1, Math.floor(this.durationSeconds)).toString());
    formData.append('latitude', this.latitude.toString());
    formData.append('longitude', this.longitude.toString());
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

  openMap(): void {
    this.showMap = true;
    setTimeout(() => this.initMap(), 0);
  }

  closeMap(): void {
    this.showMap = false;
    this.destroyMap();
  }

  confirmLocation(): void {
    this.showMap = false;
    this.destroyMap();
  }

  clearLocation(): void {
    this.latitude = null;
    this.longitude = null;
    if (this.selectionLayer && this.map) {
      this.map.removeLayer(this.selectionLayer);
      this.selectionLayer = null;
    }
  }

  ngOnDestroy(): void {
    this.destroyMap();
  }

  private initMap(): void {
    const fallbackLat = 44.8176;
    const fallbackLng = 20.4633;
    const startLat = this.latitude ?? fallbackLat;
    const startLng = this.longitude ?? fallbackLng;

    if (!this.map) {
      this.map = L.map('upload-map', {
        center: [startLat, startLng],
        zoom: 12
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(this.map);

      this.map.on('click', (e: L.LeafletMouseEvent) => {
        this.setLocation(e.latlng.lat, e.latlng.lng);
      });
    } else {
      this.map.setView([startLat, startLng], 12);
    }

    if (this.latitude !== null && this.longitude !== null) {
      this.setLocation(this.latitude, this.longitude);
    }

    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private destroyMap(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
      this.selectionLayer = null;
    }
  }

  private setLocation(lat: number, lng: number): void {
    const roundedLat = Math.round(lat * 1e6) / 1e6;
    const roundedLng = Math.round(lng * 1e6) / 1e6;

    this.latitude = roundedLat;
    this.longitude = roundedLng;

    if (!this.map) {
      return;
    }

    if (this.selectionLayer) {
      this.selectionLayer.setLatLng([roundedLat, roundedLng]);
    } else {
      this.selectionLayer = L.circleMarker([roundedLat, roundedLng], {
        radius: 8,
        color: '#cc2a2a',
        fillColor: '#ff6b6b',
        fillOpacity: 0.85
      }).addTo(this.map);
    }
  }

  private loadDurationSeconds(file: File): Promise<number> {
    return new Promise((resolve, reject) => {
      const video = document.createElement('video');
      const url = URL.createObjectURL(file);

      const cleanup = () => {
        URL.revokeObjectURL(url);
      };

      video.preload = 'metadata';
      video.onloadedmetadata = () => {
        const duration = video.duration;
        cleanup();
        if (!isFinite(duration) || duration <= 0) {
          reject(new Error('Invalid duration'));
          return;
        }
        resolve(Math.ceil(duration));
      };
      video.onerror = () => {
        cleanup();
        reject(new Error('Metadata load failed'));
      };
      video.src = url;
    });
  }
}
