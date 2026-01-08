package Jutjubic.RA56.service;

import Jutjubic.RA56.exception.CommentRateLimitException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommentRateLimiterServiceTests {
	@Test
	void limitsToSixtyPerHourPerUser() {
		CommentRateLimiterService service = new CommentRateLimiterService();
		String userKey = "test@example.com";

		for (int i = 0; i < 60; i++) {
			service.checkCommentAllowed(userKey);
		}

		Assertions.assertThrows(CommentRateLimitException.class, () -> service.checkCommentAllowed(userKey));
	}
}
