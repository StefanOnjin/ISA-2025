package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.VideoResponse;
import Jutjubic.RA56.dto.LikeResponse;
import Jutjubic.RA56.service.LikeService;
import Jutjubic.RA56.service.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import jakarta.servlet.http.HttpServletRequest;


import java.util.List;

import Jutjubic.RA56.dto.VideoDetailResponse;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    private final LikeService likeService;

    public VideoController(VideoService videoService, LikeService likeService) {
        this.videoService = videoService;
        this.likeService = likeService;
    }

    @GetMapping
    public ResponseEntity<List<VideoResponse>> getAllVideos() {
        List<VideoResponse> videos = videoService.getAllVideos();
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoDetailResponse> getVideoById(@PathVariable Long id, Principal principal) {
        String userEmail = principal != null ? principal.getName() : null;
        VideoDetailResponse video = videoService.getVideoById(id, userEmail);
        return ResponseEntity.ok(video);
    }

    @GetMapping("/play/{fileName:.+}")
    public ResponseEntity<Resource> playVideo(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = videoService.getVideoStream(fileName);

        String contentType = "video/mp4";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            @RequestParam("video") MultipartFile video,
            Principal principal) {

        VideoResponse videoResponse = videoService.createVideo(title, description, tags, latitude, longitude, thumbnail, video, principal.getName());
        return ResponseEntity.ok(videoResponse);
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> likeVideo(@PathVariable Long id, Principal principal) {
        LikeResponse response = likeService.likeVideo(id, principal.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> unlikeVideo(@PathVariable Long id, Principal principal) {
        LikeResponse response = likeService.unlikeVideo(id, principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/thumbnail/{fileName:.+}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = videoService.getThumbnailResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // fallback
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
