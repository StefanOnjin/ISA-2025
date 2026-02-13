package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.PremierDetailResponse;
import Jutjubic.RA56.dto.PremiereVideoResponse;
import Jutjubic.RA56.service.VideoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/premiers")
public class PremierController {
    private final VideoService videoService;

    public PremierController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public ResponseEntity<List<PremiereVideoResponse>> getPremieres() {
        return ResponseEntity.ok(videoService.getPremieres());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<PremierDetailResponse> getPremier(@PathVariable Long id) {
        return ResponseEntity.ok(videoService.getPremierById(id));
    }
}
