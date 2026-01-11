package Jutjubic.RA56.dto;

import java.time.LocalDateTime;

public record VideoDetailResponse(
        Long id,
        String title,
        String description,
        String tags,
        LocalDateTime createdAt,
        String location,
        String ownerUsername,
        String thumbnailUrl,
        String videoUrl,
        long likesCount,
        boolean likedByUser
) {
}
