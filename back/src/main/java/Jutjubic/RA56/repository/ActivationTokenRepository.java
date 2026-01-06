package Jutjubic.RA56.repository;

import Jutjubic.RA56.domain.ActivationToken;
import Jutjubic.RA56.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
	Optional<ActivationToken> findByToken(String token);

	void deleteByUser(User user);
}
