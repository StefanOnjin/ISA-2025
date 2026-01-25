package Jutjubic.RA56.repository;

import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.VideoMapPoint;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Video v WHERE v.id = :id")
    Video findOneByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT new Jutjubic.RA56.dto.VideoMapPoint(
                v.id,
                v.title,
                v.latitude,
                v.longitude,
                v.thumbnailPath
            )
            FROM Video v
            WHERE v.latitude BETWEEN :minLat AND :maxLat
              AND v.longitude BETWEEN :minLng AND :maxLng
            """)
    List<VideoMapPoint> findMapPoints(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );
}
