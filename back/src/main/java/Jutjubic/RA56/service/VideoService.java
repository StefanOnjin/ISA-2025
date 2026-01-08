package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.VideoDetailResponse;
import Jutjubic.RA56.dto.VideoResponse;
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
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public VideoService(VideoRepository videoRepository, FileStorageService fileStorageService, UserRepository userRepository) {
        this.videoRepository = videoRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(video -> {
                    String thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/videos/thumbnail/")
                            .path(video.getThumbnailPath())
                            .toUriString();
                    return new VideoResponse(
                            video.getId(),
                            video.getTitle(),
                            video.getDescription(),
                            video.getTags(),
                            video.getCreatedAt(),
                            video.getLocation(),
                            video.getOwner().getUsername(),
                            thumbnailUrl
                    );
                })
                .collect(Collectors.toList());
    }

    public VideoDetailResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
        
        String thumbnailUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/videos/thumbnail/")
                .path(video.getThumbnailPath())
                .toUriString();
        
        String videoUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/videos/play/")
                .path(video.getVideoPath())
                .toUriString();

        return new VideoDetailResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getTags(),
                video.getCreatedAt(),
                video.getLocation(),
                video.getOwner().getUsername(),
                thumbnailUrl,
                videoUrl
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoResponse createVideo(String title, String description, String tags, String location, MultipartFile thumbnailFile, MultipartFile videoFile, String username) {
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
                    location,
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
                savedVideo.getLocation(),
                savedVideo.getOwner().getUsername(),
                thumbnailUrl
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
