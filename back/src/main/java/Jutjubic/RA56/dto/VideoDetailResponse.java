package Jutjubic.RA56.dto;

import java.time.LocalDateTime;

public record VideoDetailResponse(
        Long id,
        String title,
        String description,
        String tags,
        Long views,
        LocalDateTime createdAt,
        Double latitude,
        Double longitude,
        String ownerUsername,
        String thumbnailUrl,
        String videoUrl,
        long likesCount,
        boolean likedByUser
) {
}
