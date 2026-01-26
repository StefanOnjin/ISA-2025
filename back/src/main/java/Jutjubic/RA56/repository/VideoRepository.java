package Jutjubic.RA56.repository;

import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.VideoMapClusterRow;
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

    @Query(value = """
            SELECT id,
                   title,
                   latitude,
                   longitude,
                   thumbnailPath,
                   videoCount
            FROM (
                SELECT v.id AS id,
                       v.title AS title,
                       AVG(v.longitude) OVER (PARTITION BY tile_x, tile_y) AS longitude,
                       AVG(v.latitude) OVER (PARTITION BY tile_x, tile_y) AS latitude,
                       v.thumbnail_path AS thumbnailPath,
                       COUNT(*) OVER (PARTITION BY tile_x, tile_y) AS videoCount,
                       ROW_NUMBER() OVER (
                           PARTITION BY tile_x, tile_y
                           ORDER BY COALESCE(v.views, 0) DESC, v.id
                       ) AS rn
                FROM (
                    SELECT v.*,
                           FLOOR(((v.longitude + 180.0) / 360.0) * POWER(2, :tileZoom)) AS tile_x,
                           FLOOR((1 - LN(TAN(RADIANS(v.latitude)) + 1 / COS(RADIANS(v.latitude))) / PI()) / 2 * POWER(2, :tileZoom)) AS tile_y
                    FROM videos v
                    WHERE v.latitude BETWEEN :minLat AND :maxLat
                      AND v.longitude BETWEEN :minLng AND :maxLng
                ) v
            ) t
            WHERE rn = 1
            """, nativeQuery = true)
    List<VideoMapClusterRow> findMapTilePoints(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("tileZoom") int tileZoom
    );
}
