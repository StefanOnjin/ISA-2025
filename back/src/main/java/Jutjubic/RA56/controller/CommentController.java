package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.CommentRequest;
import Jutjubic.RA56.dto.CommentResponse;
import Jutjubic.RA56.service.CommentService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {
	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@GetMapping("/api/videos/{videoId}/comments")
	public ResponseEntity<Page<CommentResponse>> getComments(
			@PathVariable Long videoId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ResponseEntity.ok(commentService.getComments(videoId, page, size));
	}

	@PostMapping("/api/videos/{videoId}/comments")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<CommentResponse> addComment(
			@PathVariable Long videoId,
			@Valid @RequestBody CommentRequest request,
			Principal principal) {
		CommentResponse response = commentService.addComment(videoId, principal.getName(), request.getText());
		return ResponseEntity.ok(response);
	}
}
