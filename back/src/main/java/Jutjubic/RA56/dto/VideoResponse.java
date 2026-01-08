package Jutjubic.RA56.dto;

import java.time.LocalDateTime;

public record VideoResponse(
        Long id,
        String title,
        String description,
        String tags,
        LocalDateTime createdAt,
        String location,
        String ownerUsername,
        String thumbnailUrl
) {
}
