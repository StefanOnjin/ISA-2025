package Jutjubic.RA56.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
		@NotBlank(message = "Message cannot be empty.")
		@Size(max = 500, message = "Message cannot exceed 500 characters.")
		String text
) {
}
