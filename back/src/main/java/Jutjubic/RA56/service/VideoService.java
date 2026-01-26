package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.VideoDetailResponse;
import Jutjubic.RA56.dto.VideoMapClusterRow;
import Jutjubic.RA56.dto.VideoMapPoint;
import Jutjubic.RA56.dto.VideoMapResponse;
import Jutjubic.RA56.dto.VideoResponse;
import Jutjubic.RA56.repository.VideoLikeRepository;
import Jutjubic.RA56.repository.UserRepository;
import Jutjubic.RA56.repository.VideoRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final VideoLikeRepository likeRepository;

    public VideoService(VideoRepository videoRepository,
            FileStorageService fileStorageService,
            UserRepository userRepository,
            VideoLikeRepository likeRepository) {
        this.videoRepository = videoRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
    }

    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(video -> {
                    String thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/videos/thumbnail/")
                            .path(video.getThumbnailPath())
                            .toUriString();
                    long likesCount = likeRepository.countByVideoId(video.getId());
                    return new VideoResponse(
                            video.getId(),
                            video.getTitle(),
                            video.getDescription(),
                            video.getTags(),
                            video.getCreatedAt(),
                            video.getLatitude(),
                            video.getLongitude(),
                            video.getOwner().getUsername(),
                            thumbnailUrl,
                            likesCount
                    );
                })
                .collect(Collectors.toList());
    }

    private static final int DETAIL_ZOOM_MIN = 14;
    private static final int MEDIUM_ZOOM_MIN = 12;

    @Cacheable(value = "video-map-tiles", key = "#tileZoom + ':' + #minX + ':' + #maxX + ':' + #minY + ':' + #maxY + ':' + #zoom")
    public List<VideoMapResponse> getVideosForMapTiles(int tileZoom, int minX, int maxX, int minY, int maxY, int zoom) {
        int resolvedZoom = clampZoom(zoom);
        int resolvedTileZoom = clampZoom(tileZoom);

        int safeMinX = Math.min(minX, maxX);
        int safeMaxX = Math.max(minX, maxX);
        int safeMinY = Math.min(minY, maxY);
        int safeMaxY = Math.max(minY, maxY);

        safeMinX = clampTileCoord(safeMinX, resolvedTileZoom);
        safeMaxX = clampTileCoord(safeMaxX, resolvedTileZoom);
        safeMinY = clampTileCoord(safeMinY, resolvedTileZoom);
        safeMaxY = clampTileCoord(safeMaxY, resolvedTileZoom);

        double minLng = tileXToLng(safeMinX, resolvedTileZoom);
        double maxLng = tileXToLng(safeMaxX + 1, resolvedTileZoom);
        double maxLat = tileYToLat(safeMinY, resolvedTileZoom);
        double minLat = tileYToLat(safeMaxY + 1, resolvedTileZoom);

        double safeMinLat = Math.min(minLat, maxLat);
        double safeMaxLat = Math.max(minLat, maxLat);
        double safeMinLng = Math.min(minLng, maxLng);
        double safeMaxLng = Math.max(minLng, maxLng);

        if (resolvedZoom >= DETAIL_ZOOM_MIN) {
            List<VideoMapPoint> points = videoRepository.findMapPoints(safeMinLat, safeMaxLat, safeMinLng, safeMaxLng);

            return points.stream()
                    .filter(point -> Objects.nonNull(point.latitude()) && Objects.nonNull(point.longitude()))
                    .map(point -> {
                        String thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/api/videos/thumbnail/")
                                .path(point.thumbnailPath())
                                .toUriString();
                        return new VideoMapResponse(
                                point.id(),
                                point.title(),
                                point.latitude(),
                                point.longitude(),
                                thumbnailUrl,
                                1L
                        );
                    })
                    .collect(Collectors.toList());
        }

        List<VideoMapClusterRow> points = videoRepository.findMapTilePoints(
                safeMinLat,
                safeMaxLat,
                safeMinLng,
                safeMaxLng,
                resolvedTileZoom
        );

        return points.stream()
                .filter(point -> Objects.nonNull(point.getLatitude()) && Objects.nonNull(point.getLongitude()))
                .map(point -> {
                    String thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/videos/thumbnail/")
                            .path(point.getThumbnailPath())
                            .toUriString();
                    return new VideoMapResponse(
                            point.getId(),
                            point.getTitle(),
                            point.getLatitude(),
                            point.getLongitude(),
                            thumbnailUrl,
                            point.getVideoCount() == null ? 1L : point.getVideoCount()
                    );
                })
                .collect(Collectors.toList());
    }

    private int clampZoom(int zoom) {
        if (zoom < 0) {
            return 0;
        }
        if (zoom > 19) {
            return 19;
        }
        return zoom;
    }

    private int clampTileCoord(int value, int zoom) {
        if (zoom <= 0) {
            return 0;
        }
        int max = (1 << zoom) - 1;
        if (value < 0) {
            return 0;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private double tileXToLng(int x, int zoom) {
        return (x / Math.pow(2, zoom)) * 360 - 180;
    }

    private double tileYToLat(int y, int zoom) {
        double n = Math.PI - (2 * Math.PI * y) / Math.pow(2, zoom);
        return (180 / Math.PI) * Math.atan(Math.sinh(n));
    }

    @Transactional
    public VideoDetailResponse getVideoById(Long id, String userEmail) {
        Video video = videoRepository.findOneByIdForUpdate(id);
        if (video == null) {
            throw new RuntimeException("Video not found with id: " + id);
        }

        long currentViews = video.getViews() == null ? 0L : video.getViews();
        video.setViews(currentViews + 1);
        videoRepository.save(video);

        String thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/videos/thumbnail/")
                .path(video.getThumbnailPath())
                .toUriString();
        
        String videoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/videos/play/")
                .path(video.getVideoPath())
                .toUriString();

        long likesCount = likeRepository.countByVideoId(video.getId());
        boolean likedByUser = false;

        if (userEmail != null && !userEmail.isBlank()) {
            User user = userRepository.findByEmailIgnoreCase(userEmail).orElse(null);
            if (user != null) {
                likedByUser = likeRepository.existsByVideoIdAndUserId(video.getId(), user.getId());
            }
        }

        return new VideoDetailResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getTags(),
                video.getViews(),
                video.getCreatedAt(),
                video.getLatitude(),
                video.getLongitude(),
                video.getOwner().getUsername(),
                thumbnailUrl,
                videoUrl,
                likesCount,
                likedByUser
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "video-map-tiles", allEntries = true)
    public VideoResponse createVideo(String title, String description, String tags, Double latitude, Double longitude, MultipartFile thumbnailFile, MultipartFile videoFile, String username) {
        if (latitude == null || longitude == null) {
            throw new RuntimeException("Location coordinates are required.");
        }

        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        String thumbnailFileName = null;
        String videoFileName = null;

        try {
            thumbnailFileName = fileStorageService.storeThumbnail(thumbnailFile);
            videoFileName = fileStorageService.storeVideo(videoFile);

            Video video = new Video(
                    title,
                    description,
                    tags,
                    thumbnailFileName,
                    videoFileName,
                    LocalDateTime.now(),
                    latitude,
                    longitude,
                    user
            );

            Video savedVideo = videoRepository.save(video);
            
            String thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/videos/thumbnail/")
                .path(savedVideo.getThumbnailPath())
                .toUriString();

            return new VideoResponse(
                savedVideo.getId(),
                savedVideo.getTitle(),
                savedVideo.getDescription(),
                savedVideo.getTags(),
                savedVideo.getCreatedAt(),
                savedVideo.getLatitude(),
                savedVideo.getLongitude(),
                savedVideo.getOwner().getUsername(),
                thumbnailUrl,
                0L
            );

        } catch (Exception e) {
            if (thumbnailFileName != null) {
                fileStorageService.deleteFile(thumbnailFileName, false);
            }
            if (videoFileName != null) {
                fileStorageService.deleteFile(videoFileName, true);
            }
            throw new RuntimeException("Failed to create video. " + e.getMessage(), e);
        }
    }

    @Cacheable(value = "thumbnails", unless = "#result == null")
    public Resource getThumbnailResource(String fileName) {
        return fileStorageService.loadFileAsResource(fileName);
    }

    public Resource getVideoStream(String fileName) {
        return fileStorageService.loadVideoAsResource(fileName);
    }
}
