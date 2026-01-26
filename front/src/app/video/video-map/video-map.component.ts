import { Component, OnDestroy, OnInit } from '@angular/core';
import * as L from 'leaflet';
import { VideoService } from '../../services/video.service';

interface VideoMapItem {
  id: number;
  title: string;
  latitude: number;
  longitude: number;
  thumbnailUrl: string;
  count?: number;
}

@Component({
  selector: 'app-video-map',
  templateUrl: './video-map.component.html',
  styleUrls: ['./video-map.component.css']
})
export class VideoMapComponent implements OnInit, OnDestroy {
  private map: L.Map | null = null;
  private markersLayer: L.LayerGroup | null = null;
  private debugLayer: L.LayerGroup | null = null;
  private loadTimer: number | null = null;
  private lastBoundsKey: string | null = null;
  private loading = false;
  private pendingRequest: { bounds: L.LatLngBounds; zoom: number } | null = null;
  private readonly detailZoomMin = 14;
  private readonly mediumZoomMin = 12;
  private readonly debugTiles = new URLSearchParams(window.location.search).has('debugTiles');

  constructor(private videoService: VideoService) {}

  ngOnInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
      this.markersLayer = null;
      this.debugLayer = null;
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

    this.markersLayer = L.layerGroup();
    this.map.addLayer(this.markersLayer);
    this.debugLayer = L.layerGroup();
    if (this.debugTiles) {
      this.map.addLayer(this.debugLayer);
    }

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
      const bounds = this.map!.getBounds();
      const zoom = this.map!.getZoom();
      this.updateDebugGrid(bounds, zoom);
      this.loadMapPoints(bounds, zoom);
    }, 150);
  }

  private loadMapPoints(bounds: L.LatLngBounds, zoom: number): void {
    if (this.loading) {
      this.pendingRequest = { bounds, zoom };
      return;
    }

    const boundsKey = `${bounds.getSouthWest().lat.toFixed(4)}:${bounds.getSouthWest().lng.toFixed(4)}:${bounds.getNorthEast().lat.toFixed(4)}:${bounds.getNorthEast().lng.toFixed(4)}:${zoom}`;
    if (this.lastBoundsKey === boundsKey) {
      return;
    }
    this.lastBoundsKey = boundsKey;
    this.loading = true;

    this.videoService.getMapVideos(
      bounds.getSouthWest().lat,
      bounds.getNorthEast().lat,
      bounds.getSouthWest().lng,
      bounds.getNorthEast().lng,
      zoom
    ).subscribe({
      next: (items) => {
        this.renderMarkers(items, zoom);
      },
      error: () => {
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
        if (this.pendingRequest) {
          const nextRequest = this.pendingRequest;
          this.pendingRequest = null;
          this.loadMapPoints(nextRequest.bounds, nextRequest.zoom);
        }
      }
    });
  }

  private renderMarkers(items: VideoMapItem[], zoom: number): void {
    if (!this.markersLayer) {
      return;
    }

    this.markersLayer.clearLayers();

    items.forEach(item => {
      const countBadge = item.count && item.count > 1
        ? `<div class="map-count">${item.count}</div>`
        : '';
      const markerHtml = `
        <div class="map-pin">
          <div class="map-card">
            <img src="${item.thumbnailUrl}" alt="${item.title}" />
            <div class="map-card-title">${item.title}</div>
          </div>
          <div class="map-pin-dot"></div>
          ${countBadge}
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

      this.markersLayer!.addLayer(marker);
    });
  }

  private updateDebugGrid(bounds: L.LatLngBounds, zoom: number): void {
    if (!this.debugTiles || !this.debugLayer) {
      return;
    }

    this.debugLayer.clearLayers();
    const tileZoom = this.getTileZoom(zoom);
    const minX = this.lngToTileX(bounds.getWest(), tileZoom);
    const maxX = this.lngToTileX(bounds.getEast(), tileZoom);
    const minY = this.latToTileY(bounds.getNorth(), tileZoom);
    const maxY = this.latToTileY(bounds.getSouth(), tileZoom);

    for (let x = minX; x <= maxX; x += 1) {
      for (let y = minY; y <= maxY; y += 1) {
        const west = this.tileXToLng(x, tileZoom);
        const east = this.tileXToLng(x + 1, tileZoom);
        const north = this.tileYToLat(y, tileZoom);
        const south = this.tileYToLat(y + 1, tileZoom);
        const rect = L.rectangle([[south, west], [north, east]], {
          color: '#2563eb',
          weight: 1,
          fillOpacity: 0.05
        });
        this.debugLayer.addLayer(rect);
      }
    }
  }

  private getTileZoom(zoom: number): number {
    if (zoom >= this.detailZoomMin) {
      return zoom;
    }
    if (zoom >= this.mediumZoomMin) {
      return Math.min(19, zoom + 1);
    }
    return Math.min(19, Math.max(0, zoom + 1));
  }

  private lngToTileX(lng: number, zoom: number): number {
    const scale = Math.pow(2, zoom);
    return Math.floor(((lng + 180) / 360) * scale);
  }

  private latToTileY(lat: number, zoom: number): number {
    const scale = Math.pow(2, zoom);
    const rad = (lat * Math.PI) / 180;
    const merc = Math.log(Math.tan(rad) + 1 / Math.cos(rad));
    return Math.floor((1 - merc / Math.PI) / 2 * scale);
  }

  private tileXToLng(x: number, zoom: number): number {
    return (x / Math.pow(2, zoom)) * 360 - 180;
  }

  private tileYToLat(y: number, zoom: number): number {
    const n = Math.PI - (2 * Math.PI * y) / Math.pow(2, zoom);
    return (180 / Math.PI) * Math.atan(Math.sinh(n));
  }
}
