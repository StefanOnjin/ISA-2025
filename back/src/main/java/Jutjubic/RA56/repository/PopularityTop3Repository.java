package Jutjubic.RA56.repository;

import Jutjubic.RA56.domain.PopularityTop3;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PopularityTop3Repository extends JpaRepository<PopularityTop3, Long> {
	Optional<PopularityTop3> findTopByOrderByCreatedAtDesc();
}
