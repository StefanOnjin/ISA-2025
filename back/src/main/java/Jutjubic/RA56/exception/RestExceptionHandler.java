package Jutjubic.RA56.exception;

import Jutjubic.RA56.dto.ErrorResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
		ex.getBindingResult().getGlobalErrors()
				.forEach(error -> errors.putIfAbsent("confirmPassword", error.getDefaultMessage()));
		return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed", errors));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), null));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage(), null));
	}

	@ExceptionHandler(LoginRateLimitException.class)
	public ResponseEntity<ErrorResponse> handleRateLimit(LoginRateLimitException ex) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(new ErrorResponse(ex.getMessage(), null));
	}

	@ExceptionHandler(CommentRateLimitException.class)
	public ResponseEntity<ErrorResponse> handleCommentRateLimit(CommentRateLimitException ex) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.body(new ErrorResponse(ex.getMessage(), null));
	}

	@ExceptionHandler(ActivationTokenException.class)
	public ResponseEntity<ErrorResponse> handleActivation(ActivationTokenException ex) {
		return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), null));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ErrorResponse("Invalid email or password", null));
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ErrorResponse("Account is not activated", null));
	}
}
