package Jutjubic.RA56.dto;

import java.time.LocalDateTime;

public record VideoResponse(
        Long id,
        String title,
        String description,
        String tags,
        LocalDateTime createdAt,
        Double latitude,
        Double longitude,
        String ownerUsername,
        String thumbnailUrl,
        long likesCount
) {
}
