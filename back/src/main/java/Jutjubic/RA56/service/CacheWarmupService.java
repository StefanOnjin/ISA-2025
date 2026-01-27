package Jutjubic.RA56.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CacheWarmupService {

    private final VideoService videoService;

    public CacheWarmupService(VideoService videoService) {
        this.videoService = videoService;
    }

    @Scheduled(cron = "* * 2 * * ?") // 2am
    public void warmUpVideoMapCache() {
        System.out.println("Starting cache warmup for video map at " + LocalDateTime.now());

        // Evropa - geografske koordinate
        warmupRegion(36.0, 71.0, -25.0, 40.0, "Europe");
        
        // Srbija - geografske koordinate
        warmupRegion(41.8, 46.2, 18.8, 23.0, "Serbia");

        System.out.println("Finished cache warmup for video map at " + LocalDateTime.now());
    }

    private void warmupRegion(double minLat, double maxLat, double minLng, double maxLng, String regionName) {
        double safeMinLat = Math.min(minLat, maxLat);
        double safeMaxLat = Math.max(minLat, maxLat);
        double safeMinLng = Math.min(minLng, maxLng);
        double safeMaxLng = Math.max(minLng, maxLng);

        int defaultZoom = 13;
        int defaultTileZoom = 13;

        int minX = lngToTileX(safeMinLng, defaultTileZoom);
        int maxX = lngToTileX(safeMaxLng, defaultTileZoom);
        int minY = latToTileY(safeMaxLat, defaultTileZoom);
        int maxY = latToTileY(safeMinLat, defaultTileZoom);

        System.out.println("Warming up cache for " + regionName + 
                " (lat: " + safeMinLat + "-" + safeMaxLat + 
                ", lng: " + safeMinLng + "-" + safeMaxLng + ")");

        videoService.getVideosForMapTiles(defaultTileZoom, minX, maxX, minY, maxY, defaultZoom, "all");
        videoService.getVideosForMapTiles(defaultTileZoom, minX, maxX, minY, maxY, defaultZoom, "30days");
        videoService.getVideosForMapTiles(defaultTileZoom, minX, maxX, minY, maxY, defaultZoom, "currentYear");
    }

    private int lngToTileX(double lng, int zoom) {
        return (int) Math.floor((lng + 180) / 360 * Math.pow(2, zoom));
    }

    private int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * Math.pow(2, zoom));
    }
}