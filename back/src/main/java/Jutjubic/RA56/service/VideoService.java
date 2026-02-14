package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.PremierDetailResponse;
import Jutjubic.RA56.dto.PremiereVideoResponse;
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
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final VideoLikeRepository likeRepository;
    private final CacheManager cacheManager;
    private final AdaptiveStreamingService adaptiveStreamingService;
    
    @Value("${app.base-url}") 
    private String baseUrl; 

    public VideoService(VideoRepository videoRepository,
            FileStorageService fileStorageService,
            UserRepository userRepository,
            VideoLikeRepository likeRepository,
            CacheManager cacheManager,
            AdaptiveStreamingService adaptiveStreamingService) {
        this.videoRepository = videoRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.cacheManager = cacheManager;
        this.adaptiveStreamingService = adaptiveStreamingService;
    }

    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(video -> {
                    String thumbnailUrl = this.baseUrl + "/api/videos/thumbnail/" + video.getThumbnailPath(); 
                    long likesCount = likeRepository.countByVideoId(video.getId());
                    LocalDateTime scheduledAt = video.getScheduledAt() == null ? video.getCreatedAt() : video.getScheduledAt();
                    return new VideoResponse(
                            video.getId(),
                            video.getTitle(),
                            video.getDescription(),
                            video.getTags(),
                            video.getCreatedAt(),
                            scheduledAt,
                            resolveDurationSeconds(video),
                            isPremiereEnabled(video),
                            video.getLatitude(),
                            video.getLongitude(),
                            video.getOwner().getUsername(),
                            thumbnailUrl,
                            likesCount
                    );
                })
                .collect(Collectors.toList());
    }

    public List<PremiereVideoResponse> getPremieres() {
        LocalDateTime now = LocalDateTime.now();
        return videoRepository.findAll(Sort.by(Sort.Direction.ASC, "scheduledAt")).stream()
                .filter(this::isPremiereEnabled)
                .filter(video -> !isPremiereExpired(video, now))
                .map(video -> new PremiereVideoResponse(
                        video.getId(),
                        video.getTitle(),
                        video.getDescription(),
                        this.baseUrl + "/api/videos/thumbnail/" + video.getThumbnailPath(),
                        video.getScheduledAt() == null ? video.getCreatedAt() : video.getScheduledAt(),
                        resolveDurationSeconds(video)
                ))
                .collect(Collectors.toList());
    }

    private static final int DETAIL_ZOOM_MIN = 14;
    private static final int MEDIUM_ZOOM_MIN = 12;

    @Cacheable(value = "video-map-tiles", key = "#tileZoom + ':' + #minX + ':' + #maxX + ':' + #minY + ':' + #maxY + ':' + #zoom + ':' + #period")
    public List<VideoMapResponse> getVideosForMapTiles(int tileZoom, int minX, int maxX, int minY, int maxY, int zoom, String period) {
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
        
        LocalDateTime startDate = null; 
        
        switch(period) {
        	case "30days":
        		startDate = LocalDateTime.now().minusDays(30); 
        		break; 
        	case "currentYear":
        		startDate = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0); 
        		break; 
        	case "all": 
        	default: 
        		break; 
        }

        if (resolvedZoom >= DETAIL_ZOOM_MIN) {
        	List<VideoMapPoint> points; 
        	
        	if (startDate == null) {
        		points = videoRepository.findMapPoints(
        				safeMinLat, safeMaxLat, safeMinLng, safeMaxLng
        		);
        	} else {
        		points = videoRepository.findMapPointsFilteredByDate(
        				safeMinLat, safeMaxLat, safeMinLng, safeMaxLng, startDate 
        		);
        	}
        	

            return points.stream()
                    .filter(point -> Objects.nonNull(point.latitude()) && Objects.nonNull(point.longitude()))
                    .map(point -> {
                        String thumbnailUrl = this.baseUrl + "/api/videos/thumbnail/" + point.thumbnailPath();
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
                resolvedTileZoom,
                startDate
        );

        return points.stream()
                .filter(point -> Objects.nonNull(point.getLatitude()) && Objects.nonNull(point.getLongitude()))
                .map(point -> {
                    String thumbnailUrl = this.baseUrl + "/api/videos/thumbnail/" + point.getThumbnailPath();
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

    private int lngToTileX(double lng, int zoom) {
        return (int) Math.floor((lng + 180) / 360 * Math.pow(2, zoom));
    }

    private int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * Math.pow(2, zoom));
    }

    @Transactional
    public VideoDetailResponse getVideoById(Long id, String userEmail) {
        Video video = videoRepository.findOneByIdForUpdate(id);
        if (video == null) {
            throw new RuntimeException("Video not found with id: " + id);
        }

        LocalDateTime now = LocalDateTime.now();
        if (isPremiereEnabled(video) && !isPremiereExpired(video, now)) {
            throw new IllegalStateException("Video is available only through premiere until the broadcast ends.");
        }

        long currentViews = video.getViews() == null ? 0L : video.getViews();
        video.setViews(currentViews + 1);
        videoRepository.save(video);

        String thumbnailUrl = this.baseUrl + "/api/videos/thumbnail/" + video.getThumbnailPath();
        
        String videoUrl = this.baseUrl + "/api/videos/play/" + video.getVideoPath();
        String hlsUrl = this.baseUrl + "/api/videos/hls/" + video.getVideoPath() + "/master.m3u8";
        String dashUrl = this.baseUrl + "/api/videos/dash/" + video.getVideoPath() + "/manifest.mpd";

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
                hlsUrl,
                dashUrl,
                likesCount,
                likedByUser
        );
    }

    @Transactional
    public PremierDetailResponse getPremierById(Long id) {
        Video video = videoRepository.findOneByIdForUpdate(id);
        if (video == null) {
            throw new RuntimeException("Video not found with id: " + id);
        }

        LocalDateTime scheduledAt = video.getScheduledAt() == null ? video.getCreatedAt() : video.getScheduledAt();
        long durationSeconds = resolveDurationSeconds(video);
        LocalDateTime now = LocalDateTime.now();

        if (!isPremiereEnabled(video)) {
            throw new IllegalStateException("Video is not a premiere.");
        }

        if (now.isBefore(scheduledAt)) {
            throw new IllegalStateException("Premiere has not started yet.");
        }

        if (isPremiereExpired(video, now)) {
            throw new IllegalStateException("Premiere has ended.");
        }

        long currentViews = video.getViews() == null ? 0L : video.getViews();
        video.setViews(currentViews + 1);
        videoRepository.save(video);

        long streamOffsetSeconds = computeStreamOffsetSeconds(scheduledAt, now);
        String thumbnailUrl = this.baseUrl + "/api/videos/thumbnail/" + video.getThumbnailPath();
        String videoUrl = this.baseUrl + "/api/videos/play/" + video.getVideoPath();
        String hlsUrl = this.baseUrl + "/api/videos/hls/" + video.getVideoPath() + "/master.m3u8";
        String dashUrl = this.baseUrl + "/api/videos/dash/" + video.getVideoPath() + "/manifest.mpd";

        return new PremierDetailResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                thumbnailUrl,
                videoUrl,
                hlsUrl,
                dashUrl,
                scheduledAt,
                durationSeconds,
                streamOffsetSeconds
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoResponse createVideo(String title, String description, String tags, String scheduledAtValue, Long durationSeconds, Double latitude, Double longitude, MultipartFile thumbnailFile, MultipartFile videoFile, String username) {
        if (latitude == null || longitude == null) {
            throw new RuntimeException("Location coordinates are required.");
        }
        if (durationSeconds == null || durationSeconds <= 0) {
            throw new IllegalArgumentException("Video duration must be greater than zero.");
        }

        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        String thumbnailFileName = null;
        String videoFileName = null;

        try {
            thumbnailFileName = fileStorageService.storeThumbnail(thumbnailFile);
            videoFileName = fileStorageService.storeVideo(videoFile);
            Path sourceVideoPath = fileStorageService.resolveVideoPath(videoFileName);
            adaptiveStreamingService.ensureAdaptiveStreamsAsync(videoFileName, sourceVideoPath);
            LocalDateTime now = LocalDateTime.now();
            boolean hasScheduledAt = scheduledAtValue != null && !scheduledAtValue.isBlank();
            LocalDateTime scheduledAt = parseScheduledAtOrNow(scheduledAtValue, now, durationSeconds);
            if (hasScheduledAt && scheduledAt.isBefore(now)) {
                throw new IllegalArgumentException("Scheduled date cannot be in the past.");
            }

            Video video = new Video(
                    title,
                    description,
                    tags,
                    thumbnailFileName,
                    videoFileName,
                    now,
                    scheduledAt,
                    durationSeconds,
                    hasScheduledAt,
                    latitude,
                    longitude,
                    user
            );

            Video savedVideo = videoRepository.save(video);

            evictVideoMapCacheFor(savedVideo);
            
            String thumbnailUrl = this.baseUrl + "/api/videos/thumbnail/" + savedVideo.getThumbnailPath();

            return new VideoResponse(
                savedVideo.getId(),
                savedVideo.getTitle(),
                savedVideo.getDescription(),
                savedVideo.getTags(),
                savedVideo.getCreatedAt(),
                savedVideo.getScheduledAt(),
                resolveDurationSeconds(savedVideo),
                isPremiereEnabled(savedVideo),
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

    private void evictVideoMapCacheFor(Video video) {
        Cache videoMapCache = cacheManager.getCache("video-map-tiles");
        if (videoMapCache != null) {
            if (video.getLatitude() == null || video.getLongitude() == null) {
                videoMapCache.clear();
                return;
            }

            Object nativeCache = videoMapCache.getNativeCache();
            if (nativeCache instanceof javax.cache.Cache<?, ?> jcache) {
                for (javax.cache.Cache.Entry<?, ?> entry : jcache) {
                    Object keyObj = entry.getKey();
                    if (!(keyObj instanceof String key)) {
                        continue;
                    }
                    if (shouldEvictMapKeyForVideo(key, video.getLatitude(), video.getLongitude())) {
                        videoMapCache.evict(key);
                    }
                }
                return;
            }

            videoMapCache.clear();
        }
    }

    private boolean shouldEvictMapKeyForVideo(String key, double latitude, double longitude) {
        String[] parts = key.split(":");
        if (parts.length < 7) {
            return false;
        }
        try {
            int tileZoom = Integer.parseInt(parts[0]);
            int minX = Integer.parseInt(parts[1]);
            int maxX = Integer.parseInt(parts[2]);
            int minY = Integer.parseInt(parts[3]);
            int maxY = Integer.parseInt(parts[4]);
            int tileX = lngToTileX(longitude, tileZoom);
            int tileY = latToTileY(latitude, tileZoom);
            return tileX >= Math.min(minX, maxX)
                    && tileX <= Math.max(minX, maxX)
                    && tileY >= Math.min(minY, maxY)
                    && tileY <= Math.max(minY, maxY);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    @Cacheable(value = "thumbnails", unless = "#result == null")
    public Resource getThumbnailResource(String fileName) {
        return fileStorageService.loadFileAsResource(fileName);
    }

    public Resource getVideoStream(String fileName) {
        ensureVideoAvailableNow(fileName);
        return fileStorageService.loadVideoAsResource(fileName);
    }

    public Resource getHlsResource(String fileName, String resourcePath) {
        ensureVideoAvailableNow(fileName);
        Path resource = adaptiveStreamingService.resolveHlsResource(fileName, resourcePath);
        return asResource(resource);
    }

    public Resource getDashResource(String fileName, String resourcePath) {
        ensureVideoAvailableNow(fileName);
        Path resource = adaptiveStreamingService.resolveDashResource(fileName, resourcePath);
        return asResource(resource);
    }

    private void ensureVideoAvailableNow(String fileName) {
        videoRepository.findByVideoPath(fileName).ifPresent(video -> {
            LocalDateTime scheduledAt = video.getScheduledAt() == null ? video.getCreatedAt() : video.getScheduledAt();
            if (LocalDateTime.now().isBefore(scheduledAt)) {
                throw new IllegalStateException("Video is not available before the scheduled time.");
            }
        });
    }

    private Resource asResource(Path path) {
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new RuntimeException("Adaptive stream resource not found.");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Invalid adaptive stream resource path.", ex);
        }
    }

    private LocalDateTime parseScheduledAtOrNow(String scheduledAtValue, LocalDateTime now, long durationSeconds) {
        if (scheduledAtValue == null || scheduledAtValue.isBlank()) {
            return now.minusSeconds(Math.max(1L, durationSeconds));
        }

        try {
            return LocalDateTime.parse(scheduledAtValue);
        } catch (DateTimeParseException ignored) {
            try {
                DateTimeFormatter fallback = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                return LocalDateTime.parse(scheduledAtValue, fallback);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid scheduledAt format. Expected ISO date-time.");
            }
        }
    }

    private long resolveDurationSeconds(Video video) {
        if (video.getDurationSeconds() == null || video.getDurationSeconds() <= 0) {
            return 1L;
        }
        return video.getDurationSeconds();
    }

    private boolean isPremiereEnabled(Video video) {
        if (!Boolean.TRUE.equals(video.getPremiereEnabled())) {
            return false;
        }
        if (video.getScheduledAt() == null || video.getCreatedAt() == null) {
            return false;
        }
        // Guard for legacy rows where unscheduled uploads were represented as past scheduled times.
        return !video.getScheduledAt().isBefore(video.getCreatedAt());
    }

    private long computeStreamOffsetSeconds(LocalDateTime scheduledAt, LocalDateTime now) {
        if (now.isBefore(scheduledAt)) {
            return 0L;
        }
        return Math.max(0L, Duration.between(scheduledAt, now).getSeconds());
    }

    private boolean isPremiereExpired(Video video, LocalDateTime now) {
        LocalDateTime scheduledAt = video.getScheduledAt() == null ? video.getCreatedAt() : video.getScheduledAt();
        long durationSeconds = resolveDurationSeconds(video);
        LocalDateTime endAt = scheduledAt.plusSeconds(durationSeconds);
        return !now.isBefore(endAt);
    }
}
