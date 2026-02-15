package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.repository.VideoRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ThumbnailCompressionService {

    private final VideoRepository videoRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.thumbnail-compression.older-than-days:30}")
    private long olderThanDays;

    @Value("${app.thumbnail-compression.batch-size:100}")
    private int batchSize;

    @Value("${app.thumbnail-compression.quality:0.75}")
    private double compressionQuality;

    public ThumbnailCompressionService(VideoRepository videoRepository, FileStorageService fileStorageService) {
        this.videoRepository = videoRepository;
        this.fileStorageService = fileStorageService;
    }

    @Scheduled(cron = "${app.thumbnail-compression.cron:0 15 3 * * ?}")
    public void compressOldThumbnails() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);

        while (true) {
            List<Video> candidates = videoRepository.findOldUncompressedThumbnails(
                    cutoff,
                    PageRequest.of(0, batchSize)
            );
            if (candidates.isEmpty()) {
                return;
            }

            int compressedInBatch = 0;
            for (Video video : candidates) {
                if (compressOne(video)) {
                    compressedInBatch++;
                }
            }

            if (compressedInBatch == 0) {
                return;
            }
        }
    }

    private boolean compressOne(Video video) {
        String originalThumbnail = video.getThumbnailPath();
        Path sourcePath = fileStorageService.resolveThumbnailPath(originalThumbnail);
        if (!Files.exists(sourcePath)) {
            System.err.println("Thumbnail compression skipped - source file missing: " + sourcePath);
            return false;
        }

        String compressedFileName = UUID.randomUUID() + "_cmp.jpg";
        Path compressedPath = fileStorageService.resolveThumbnailPath(compressedFileName);

        try {
            Thumbnails.of(sourcePath.toFile())
                    .scale(1.0)
                    .outputFormat("jpg")
                    .outputQuality(compressionQuality)
                    .toFile(compressedPath.toFile());

            video.setThumbnailPath(compressedFileName);
            video.setThumbnailCompressed(true);
            video.setThumbnailCompressedAt(LocalDateTime.now());
            videoRepository.save(video);
            return true;
        } catch (Exception ex) {
            System.err.println("Failed to compress thumbnail for video id " + video.getId() + ": " + ex.getMessage());
            return false;
        }
    }
}
