package Jutjubic.UploadBenchmark.controller;

import Jutjubic.UploadBenchmark.service.UploadEventBenchmarkService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/benchmark")
public class BenchmarkReportController {
	private final UploadEventBenchmarkService benchmarkService;

	public BenchmarkReportController(UploadEventBenchmarkService benchmarkService) {
		this.benchmarkService = benchmarkService;
	}

	@GetMapping(value = "/report", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> getReport() {
		return ResponseEntity.ok(benchmarkService.buildReport());
	}
}
