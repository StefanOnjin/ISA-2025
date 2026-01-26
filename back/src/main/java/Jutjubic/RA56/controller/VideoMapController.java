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
            @RequestParam("minLat") String minLat,
            @RequestParam("maxLat") String maxLat,
            @RequestParam("minLng") String minLng,
            @RequestParam("maxLng") String maxLng,
            @RequestParam(value = "zoom", required = false) Integer zoom) {
        double parsedMinLat = parseDouble(minLat, "minLat");
        double parsedMaxLat = parseDouble(maxLat, "maxLat");
        double parsedMinLng = parseDouble(minLng, "minLng");
        double parsedMaxLng = parseDouble(maxLng, "maxLng");

        List<VideoMapResponse> videos = videoService.getVideosForMap(parsedMinLat, parsedMaxLat, parsedMinLng, parsedMaxLng, zoom);
        return ResponseEntity.ok(videos);
    }

    private double parseDouble(String value, String fieldName) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + fieldName + " value: " + value);
        }
    }
}
