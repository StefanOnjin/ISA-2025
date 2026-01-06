package Jutjubic.RA56.security;

import Jutjubic.RA56.domain.User;
import Jutjubic.RA56.repository.UserRepository;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());

		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getEmail())
				.password(user.getPassword())
				.authorities(Collections.singletonList(authority))
				.accountLocked(false)
				.disabled(!user.isEnabled())
				.build();
	}
}
