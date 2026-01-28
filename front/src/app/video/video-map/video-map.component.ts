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
  private tileCache = new Map<string, Map<number, VideoMapItem>>();
  private readonly maxCachedTiles = 500;
  private readonly detailZoomMin = 14;
  private readonly mediumZoomMin = 12;
  private readonly debugTiles = new URLSearchParams(window.location.search).has('debugTiles');
  private selectedPeriod = "all"; 

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

  public onPeriodChange(event: Event) : void {
    const select = event.target as HTMLSelectElement;
    this.selectedPeriod = select.value;
    // Period change invalidates cached tiles because cache keys are period-aware.
    this.lastBoundsKey = null;
    this.tileCache.clear();
    this.scheduleLoad();
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
    const tileZoom = this.getTileZoom(zoom);
    const minX = this.lngToTileX(bounds.getWest(), tileZoom);
    const maxX = this.lngToTileX(bounds.getEast(), tileZoom);
    const minY = this.latToTileY(bounds.getNorth(), tileZoom);
    const maxY = this.latToTileY(bounds.getSouth(), tileZoom);
    const modeKey = zoom >= this.detailZoomMin ? 'detail' : 'cluster';
    const boundsKey = `${tileZoom}:${minX}:${maxX}:${minY}:${maxY}:${modeKey}:${this.selectedPeriod}`;
    if (this.lastBoundsKey === boundsKey) {
      this.renderVisibleTiles(minX, maxX, minY, maxY, tileZoom, modeKey, zoom);
      return;
    }
    this.lastBoundsKey = boundsKey;

    if (this.hasAllTiles(minX, maxX, minY, maxY, tileZoom, modeKey)) {
      this.renderVisibleTiles(minX, maxX, minY, maxY, tileZoom, modeKey, zoom);
      return;
    }

    const missing = this.collectMissingTiles(minX, maxX, minY, maxY, tileZoom, modeKey);
    const chunks = this.chunkMissingTiles(missing, 8);
    if (chunks.length === 0) {
      this.renderVisibleTiles(minX, maxX, minY, maxY, tileZoom, modeKey, zoom);
      return;
    }

    chunks.forEach(chunk => {
      this.videoService.getMapVideos(
        tileZoom,
        chunk.minX,
        chunk.maxX,
        chunk.minY,
        chunk.maxY,
        zoom,
        this.selectedPeriod
      ).subscribe({
        next: (items) => {
          this.markTilesLoaded(chunk.minX, chunk.maxX, chunk.minY, chunk.maxY, tileZoom, modeKey);
          this.storeTileItems(items, tileZoom, modeKey);
          this.renderVisibleTiles(minX, maxX, minY, maxY, tileZoom, modeKey, zoom);
        },
        error: () => {
          this.renderVisibleTiles(minX, maxX, minY, maxY, tileZoom, modeKey, zoom);
        }
      });
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

  private getTileKey(tileZoom: number, x: number, y: number, modeKey: string): string {
    return `${tileZoom}:${x}:${y}:${modeKey}:${this.selectedPeriod}`;
  }

  private storeTileItems(items: VideoMapItem[], tileZoom: number, modeKey: string): void {
    items.forEach(item => {
      const x = this.lngToTileX(item.longitude, tileZoom);
      const y = this.latToTileY(item.latitude, tileZoom);
      const key = this.getTileKey(tileZoom, x, y, modeKey);
      const existing = this.touchTile(key) ?? new Map<number, VideoMapItem>();
      existing.set(item.id, item);
      this.tileCache.set(key, existing);
      this.evictIfNeeded();
    });
  }

  private renderVisibleTiles(minX: number, maxX: number, minY: number, maxY: number, tileZoom: number, modeKey: string, zoom: number): void {
    const aggregated = new Map<number, VideoMapItem>();
    for (let x = minX; x <= maxX; x += 1) {
      for (let y = minY; y <= maxY; y += 1) {
        const key = this.getTileKey(tileZoom, x, y, modeKey);
        const items = this.touchTile(key);
        if (!items) {
          continue;
        }
        items.forEach(item => {
          aggregated.set(item.id, item);
        });
      }
    }
    this.renderMarkers(Array.from(aggregated.values()), zoom);
  }

  private hasAllTiles(minX: number, maxX: number, minY: number, maxY: number, tileZoom: number, modeKey: string): boolean {
    for (let x = minX; x <= maxX; x += 1) {
      for (let y = minY; y <= maxY; y += 1) {
        const key = this.getTileKey(tileZoom, x, y, modeKey);
        if (!this.tileCache.has(key)) {
          return false;
        }
      }
    }
    return true;
  }

  private collectMissingTiles(
    minX: number,
    maxX: number,
    minY: number,
    maxY: number,
    tileZoom: number,
    modeKey: string
  ): Array<{ x: number; y: number }> {
    const missing: Array<{ x: number; y: number }> = [];
    for (let x = minX; x <= maxX; x += 1) {
      for (let y = minY; y <= maxY; y += 1) {
        const key = this.getTileKey(tileZoom, x, y, modeKey);
        if (!this.tileCache.has(key)) {
          missing.push({ x, y });
        }
      }
    }
    return missing;
  }

  private chunkMissingTiles(
    tiles: Array<{ x: number; y: number }>,
    chunkSize: number
  ): Array<{ minX: number; maxX: number; minY: number; maxY: number }> {
    if (tiles.length === 0) {
      return [];
    }

    const chunkMap = new Map<string, { minX: number; maxX: number; minY: number; maxY: number }>();
    tiles.forEach(tile => {
      const chunkX = Math.floor(tile.x / chunkSize);
      const chunkY = Math.floor(tile.y / chunkSize);
      const key = `${chunkX}:${chunkY}`;
      const existing = chunkMap.get(key);
      if (!existing) {
        const startX = chunkX * chunkSize;
        const startY = chunkY * chunkSize;
        chunkMap.set(key, {
          minX: Math.max(startX, tile.x),
          maxX: Math.min(startX + chunkSize - 1, tile.x),
          minY: Math.max(startY, tile.y),
          maxY: Math.min(startY + chunkSize - 1, tile.y)
        });
        return;
      }
      existing.minX = Math.min(existing.minX, tile.x);
      existing.maxX = Math.max(existing.maxX, tile.x);
      existing.minY = Math.min(existing.minY, tile.y);
      existing.maxY = Math.max(existing.maxY, tile.y);
    });

    return Array.from(chunkMap.values());
  }

  private markTilesLoaded(minX: number, maxX: number, minY: number, maxY: number, tileZoom: number, modeKey: string): void {
    for (let x = minX; x <= maxX; x += 1) {
      for (let y = minY; y <= maxY; y += 1) {
        const key = this.getTileKey(tileZoom, x, y, modeKey);
        if (!this.tileCache.has(key)) {
          this.tileCache.set(key, new Map<number, VideoMapItem>());
        }
        this.touchTile(key);
        this.evictIfNeeded();
      }
    }
  }

  private touchTile(key: string): Map<number, VideoMapItem> | undefined {
    const existing = this.tileCache.get(key);
    if (!existing) {
      return undefined;
    }
    this.tileCache.delete(key);
    this.tileCache.set(key, existing);
    return existing;
  }

  private evictIfNeeded(): void {
    while (this.tileCache.size > this.maxCachedTiles) {
      const oldestKey = this.tileCache.keys().next().value as string | undefined;
      if (!oldestKey) {
        return;
      }
      this.tileCache.delete(oldestKey);
    }
  }
}
