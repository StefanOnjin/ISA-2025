package Jutjubic.RA56.repository;

import Jutjubic.RA56.domain.TranscodingJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranscodingJobRepository extends JpaRepository<TranscodingJob, Long> {

	Optional<TranscodingJob> findByVideoId(Long videoId);

	Optional<TranscodingJob> findByVideoVideoPath(String videoPath);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT j FROM TranscodingJob j WHERE j.id = :id")
	Optional<TranscodingJob> findByIdForUpdate(@Param("id") Long id);
}
