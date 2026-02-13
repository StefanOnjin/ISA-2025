package Jutjubic.RA56.security;

import Jutjubic.RA56.security.auth.RestAuthenticationEntryPoint;
import Jutjubic.RA56.security.auth.TokenAuthenticationFilter;
import Jutjubic.RA56.util.TokenUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
	private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
	private final TokenUtils tokenUtils;
	private final UserDetailsService userDetailsService;

	public SecurityConfig(RestAuthenticationEntryPoint restAuthenticationEntryPoint,
			TokenUtils tokenUtils,
			UserDetailsService userDetailsService) {
		this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
		this.tokenUtils = tokenUtils;
		this.userDetailsService = userDetailsService;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint))
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.authorizeHttpRequests(auth -> auth
					.requestMatchers(HttpMethod.GET, "/api/videos/**").permitAll()
					.requestMatchers(HttpMethod.HEAD, "/api/videos/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/video-map/**").permitAll()
					.requestMatchers(HttpMethod.HEAD, "/api/video-map/**").permitAll()
					.requestMatchers("/api/video-map/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/premiers/**").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/premiers").permitAll()
					.requestMatchers(HttpMethod.HEAD, "/api/premiers/**").permitAll()
					.requestMatchers(HttpMethod.HEAD, "/api/premiers").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
					.requestMatchers(HttpMethod.HEAD, "/api/users/**").permitAll()
					.requestMatchers("/auth/**", "/h2-console/**", "/error").permitAll()
					.anyRequest().authenticated());

		http.formLogin(form -> form.disable());
		http.httpBasic(basic -> basic.disable());

		http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
		http.addFilterBefore(new TokenAuthenticationFilter(tokenUtils, userDetailsService), BasicAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("http://localhost:4200");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		config.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
