package Jutjubic.RA56.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class TokenUtils {
	@Value("${jwt.app-name:ra56-api}")
	private String appName;

	@Value("${jwt.secret:ra56-secret-key-for-jwt-token-has-to-be-64-bytes-long-1234567890}")
	private String secret;

	@Value("${jwt.expires-in:1800000}")
	private int expiresIn;

	@Value("${jwt.auth-header:Authorization}")
	private String authHeader;

	private SecretKey getSigningKey() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateToken(String username) {
		Date now = new Date();
		return Jwts.builder()
				.setIssuer(appName)
				.setSubject(username)
				.setIssuedAt(now)
				.setExpiration(new Date(now.getTime() + expiresIn))
				.signWith(getSigningKey(), SignatureAlgorithm.HS512)
				.compact();
	}

	public int getExpiresIn() {
		return expiresIn;
	}

	public String getToken(HttpServletRequest request) {
		String authHeaderValue = request.getHeader(authHeader);
		if (authHeaderValue != null && authHeaderValue.startsWith("Bearer ")) {
			return authHeaderValue.substring(7);
		}
		return null;
	}

	public String getUsernameFromToken(String token) {
		try {
			return getAllClaimsFromToken(token).getSubject();
		} catch (ExpiredJwtException ex) {
			throw ex;
		} catch (Exception ex) {
			return null;
		}
	}

	public Date getExpirationDateFromToken(String token) {
		try {
			return getAllClaimsFromToken(token).getExpiration();
		} catch (ExpiredJwtException ex) {
			throw ex;
		} catch (Exception ex) {
			return null;
		}
	}

	public boolean validateToken(String token, UserDetails userDetails) {
		String username = getUsernameFromToken(token);
		Date expiration = getExpirationDateFromToken(token);
		return username != null
				&& username.equalsIgnoreCase(userDetails.getUsername())
				&& expiration != null
				&& expiration.after(new Date());
	}

	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}
}
