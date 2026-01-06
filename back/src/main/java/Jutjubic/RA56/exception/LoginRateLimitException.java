package Jutjubic.RA56.exception;

public class LoginRateLimitException extends RuntimeException {
	public LoginRateLimitException(String message) {
		super(message);
	}
}
