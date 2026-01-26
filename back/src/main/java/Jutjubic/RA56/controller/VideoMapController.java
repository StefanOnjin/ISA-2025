package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.VideoMapResponse;
import Jutjubic.RA56.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/video-map")
public class VideoMapController {
    private final VideoService videoService;

    public VideoMapController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/points")
    public ResponseEntity<List<VideoMapResponse>> getVideosForMap(
            @RequestParam("tileZoom") String tileZoom,
            @RequestParam("minX") String minX,
            @RequestParam("maxX") String maxX,
            @RequestParam("minY") String minY,
            @RequestParam("maxY") String maxY,
            @RequestParam("zoom") String zoom) {
        int parsedTileZoom = parseInt(tileZoom, "tileZoom");
        int parsedMinX = parseInt(minX, "minX");
        int parsedMaxX = parseInt(maxX, "maxX");
        int parsedMinY = parseInt(minY, "minY");
        int parsedMaxY = parseInt(maxY, "maxY");
        int parsedZoom = parseInt(zoom, "zoom");

        List<VideoMapResponse> videos = videoService.getVideosForMapTiles(
                parsedTileZoom,
                parsedMinX,
                parsedMaxX,
                parsedMinY,
                parsedMaxY,
                parsedZoom
        );
        return ResponseEntity.ok(videos);
    }

    private int parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + fieldName + " value: " + value);
        }
    }
}
