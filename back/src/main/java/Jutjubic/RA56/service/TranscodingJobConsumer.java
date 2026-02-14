package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.TranscodingJob;
import Jutjubic.RA56.domain.TranscodingJobStatus;
import Jutjubic.RA56.dto.TranscodingJobMessage;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class TranscodingJobConsumer {

	private static final Logger logger = LoggerFactory.getLogger(TranscodingJobConsumer.class);

	private final TranscodingWorkerService transcodingWorkerService;
	private final FileStorageService fileStorageService;
	private final TranscodingJobService transcodingJobService;
	private final TranscodingJobProducer transcodingJobProducer;

	public TranscodingJobConsumer(
			TranscodingWorkerService transcodingWorkerService,
			FileStorageService fileStorageService,
			TranscodingJobService transcodingJobService,
			TranscodingJobProducer transcodingJobProducer) {
		this.transcodingWorkerService = transcodingWorkerService;
		this.fileStorageService = fileStorageService;
		this.transcodingJobService = transcodingJobService;
		this.transcodingJobProducer = transcodingJobProducer;
	}

	@RabbitListener(
			queues = "${app.transcoding.queue}",
			containerFactory = "transcodingListenerContainerFactory",
			concurrency = "2-4"
	)
	public void consume(TranscodingJobMessage payload, Message message, Channel channel) throws IOException {
		long deliveryTag = message.getMessageProperties().getDeliveryTag();
		if (payload == null || payload.jobId() == null || payload.videoFileName() == null) {
			channel.basicAck(deliveryTag, false);
			return;
		}

		TranscodingJob job = transcodingJobService.startProcessing(payload.jobId());
		if (job == null || job.getStatus() == TranscodingJobStatus.COMPLETED || job.getStatus() == TranscodingJobStatus.FAILED) {
			channel.basicAck(deliveryTag, false);
			return;
		}

		try {
			Path sourcePath = fileStorageService.resolveVideoPath(payload.videoFileName());
			long duration = Math.max(1L, payload.durationSeconds());
			transcodingWorkerService.transcodeToHls(
					payload.videoFileName(),
					sourcePath,
					duration,
					progress -> transcodingJobService.updateProgress(payload.jobId(), progress)
			);
			transcodingJobService.markCompleted(payload.jobId());
			channel.basicAck(deliveryTag, false);
		} catch (Exception ex) {
			boolean shouldRetry = transcodingJobService.markFailureAndCheckRetry(payload.jobId(), ex);
			logger.error("Transcoding failed for jobId={} videoId={}", payload.jobId(), payload.videoId(), ex);
			channel.basicAck(deliveryTag, false);
			if (shouldRetry) {
				safeDelay(1500);
				transcodingJobProducer.publish(payload);
			}
		}
	}

	private void safeDelay(long millis) {
		try {
			TimeUnit.MILLISECONDS.sleep(millis);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
