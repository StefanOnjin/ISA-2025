import { Component, OnDestroy, OnInit } from '@angular/core';
import * as L from 'leaflet';
import 'leaflet.markercluster';
import { VideoService } from '../../services/video.service';

interface VideoMapItem {
  id: number;
  title: string;
  latitude: number;
  longitude: number;
  thumbnailUrl: string;
}

@Component({
  selector: 'app-video-map',
  templateUrl: './video-map.component.html',
  styleUrls: ['./video-map.component.css']
})
export class VideoMapComponent implements OnInit, OnDestroy {
  private map: L.Map | null = null;
  private clusterGroup: L.MarkerClusterGroup | null = null;
  private loadTimer: number | null = null;
  private lastBoundsKey: string | null = null;
  private loading = false;
  private pendingBounds: L.LatLngBounds | null = null;

  constructor(private videoService: VideoService) {}

  ngOnInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
      this.clusterGroup = null;
    }
    if (this.loadTimer !== null) {
      window.clearTimeout(this.loadTimer);
      this.loadTimer = null;
    }
  }

  private initMap(): void {
    const startLat = 45.2671;
    const startLng = 19.8335;

    this.map = L.map('video-map', {
      center: [startLat, startLng],
      zoom: 8
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.clusterGroup = L.markerClusterGroup({
      iconCreateFunction: (cluster) => {
        const count = cluster.getChildCount();
        return L.divIcon({
          html: `<div class="cluster-badge">${count}</div>`,
          className: 'cluster-icon',
          iconSize: L.point(44, 44)
        });
      },
      showCoverageOnHover: false,
      spiderfyOnMaxZoom: false,
      animate: false
    });
    this.map.addLayer(this.clusterGroup);

    this.map.on('moveend', () => this.scheduleLoad());
    this.map.on('zoomend', () => this.scheduleLoad());

    this.scheduleLoad();
  }

  private scheduleLoad(): void {
    if (!this.map) {
      return;
    }
    if (this.loadTimer !== null) {
      window.clearTimeout(this.loadTimer);
    }
    this.loadTimer = window.setTimeout(() => {
      this.loadTimer = null;
      this.loadMapPoints(this.map!.getBounds());
    }, 150);
  }

  private loadMapPoints(bounds: L.LatLngBounds): void {
    if (this.loading) {
      this.pendingBounds = bounds;
      return;
    }

    const boundsKey = `${bounds.getSouthWest().lat.toFixed(4)}:${bounds.getSouthWest().lng.toFixed(4)}:${bounds.getNorthEast().lat.toFixed(4)}:${bounds.getNorthEast().lng.toFixed(4)}`;
    if (this.lastBoundsKey === boundsKey) {
      return;
    }
    this.lastBoundsKey = boundsKey;
    this.loading = true;

    this.videoService.getMapVideos(
      bounds.getSouthWest().lat,
      bounds.getNorthEast().lat,
      bounds.getSouthWest().lng,
      bounds.getNorthEast().lng
    ).subscribe({
      next: (items) => {
        this.renderMarkers(items);
      },
      error: () => {
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
        if (this.pendingBounds) {
          const nextBounds = this.pendingBounds;
          this.pendingBounds = null;
          this.loadMapPoints(nextBounds);
        }
      }
    });
  }

  private renderMarkers(items: VideoMapItem[]): void {
    if (!this.clusterGroup) {
      return;
    }

    this.clusterGroup.clearLayers();

    items.forEach(item => {
      const markerHtml = `
        <div class="map-pin">
          <div class="map-card">
            <img src="${item.thumbnailUrl}" alt="${item.title}" />
            <div class="map-card-title">${item.title}</div>
          </div>
          <div class="map-pin-dot"></div>
        </div>
      `;

      const marker = L.marker([item.latitude, item.longitude], {
        icon: L.divIcon({
          className: 'map-marker',
          html: markerHtml,
          iconSize: [140, 130],
          iconAnchor: [70, 124]
        })
      });

      marker.on('click', () => {
        window.location.href = `/videos/${item.id}`;
      });

      this.clusterGroup!.addLayer(marker);
    });
  }
}
