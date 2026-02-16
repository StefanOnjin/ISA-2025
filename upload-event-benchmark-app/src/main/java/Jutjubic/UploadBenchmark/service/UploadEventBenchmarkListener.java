package Jutjubic.UploadBenchmark.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UploadEventBenchmarkListener {
	private final UploadEventBenchmarkService benchmarkService;

	public UploadEventBenchmarkListener(UploadEventBenchmarkService benchmarkService) {
		this.benchmarkService = benchmarkService;
	}

	@RabbitListener(queues = "${app.upload-events.json.queue}")
	public void onJsonMessage(byte[] body) {
		benchmarkService.handleJson(body);
	}

	@RabbitListener(queues = "${app.upload-events.protobuf.queue}")
	public void onProtobufMessage(byte[] body) {
		benchmarkService.handleProtobuf(body);
	}
}
