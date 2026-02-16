package Jutjubic.RA56.controller;

import Jutjubic.RA56.dto.PopularityTop3Response;
import Jutjubic.RA56.service.PopularityEtlService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/popularity")
public class PopularityController {
	private final PopularityEtlService popularityEtlService;

	public PopularityController(PopularityEtlService popularityEtlService) {
		this.popularityEtlService = popularityEtlService;
	}

	@GetMapping("/latest")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<PopularityTop3Response> getLatest() {
		return popularityEtlService.getLatestTop3()
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}
}
