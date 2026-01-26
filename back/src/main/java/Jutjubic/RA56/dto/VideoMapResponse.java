package Jutjubic.RA56.dto;

public record VideoMapResponse(
        Long id,
        String title,
        Double latitude,
        Double longitude,
        String thumbnailUrl,
        Long count
) {
}
