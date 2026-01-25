package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.VideoDetailResponse;
import Jutjubic.RA56.dto.VideoMapPoint;
import Jutjubic.RA56.dto.VideoMapResponse;
import Jutjubic.RA56.dto.VideoResponse;
import Jutjubic.RA56.repository.VideoLikeRepository;
import Jutjubic.RA56.repository.UserRepository;
import Jutjubic.RA56.repository.VideoRepository;
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

    public List<VideoMapResponse> getVideosForMap(double minLat, double maxLat, double minLng, double maxLng) {
        double safeMinLat = Math.min(minLat, maxLat);
        double safeMaxLat = Math.max(minLat, maxLat);
        double safeMinLng = Math.min(minLng, maxLng);
        double safeMaxLng = Math.max(minLng, maxLng);

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
                            thumbnailUrl
                    );
                })
                .collect(Collectors.toList());
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
