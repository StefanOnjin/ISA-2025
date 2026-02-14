package Jutjubic.RA56.service;

import Jutjubic.RA56.domain.TranscodingJob;
import Jutjubic.RA56.domain.TranscodingJobStatus;
import Jutjubic.RA56.domain.Video;
import Jutjubic.RA56.dto.TranscodingStatusResponse;
import Jutjubic.RA56.repository.TranscodingJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranscodingJobService {

	private final TranscodingJobRepository transcodingJobRepository;
	private final int maxRetries;

	public TranscodingJobService(
			TranscodingJobRepository transcodingJobRepository,
			@Value("${app.transcoding.max-retries:3}") int maxRetries) {
		this.transcodingJobRepository = transcodingJobRepository;
		this.maxRetries = Math.max(1, maxRetries);
	}

	@Transactional
	public TranscodingJob createPendingJob(Video video) {
		TranscodingJob job = new TranscodingJob();
		job.setVideo(video);
		job.setStatus(TranscodingJobStatus.PENDING);
		job.setProgress(0);
		job.setAttempt(0);
		job.setLastError(null);
		return transcodingJobRepository.save(job);
	}

	@Transactional
	public TranscodingJob startProcessing(Long jobId) {
		TranscodingJob job = transcodingJobRepository.findByIdForUpdate(jobId)
				.orElseThrow(() -> new IllegalArgumentException("Transcoding job not found."));

		if (job.getStatus() == TranscodingJobStatus.COMPLETED || job.getStatus() == TranscodingJobStatus.PROCESSING) {
			return null;
		}
		if (job.getStatus() == TranscodingJobStatus.FAILED && job.getAttempt() >= maxRetries) {
			return null;
		}

		job.setStatus(TranscodingJobStatus.PROCESSING);
		job.setProgress(Math.max(1, safeProgress(job.getProgress())));
		job.setLastError(null);
		return transcodingJobRepository.save(job);
	}

	@Transactional
	public void updateProgress(Long jobId, int progress) {
		TranscodingJob job = transcodingJobRepository.findById(jobId)
				.orElseThrow(() -> new IllegalArgumentException("Transcoding job not found."));
		if (job.getStatus() != TranscodingJobStatus.PROCESSING) {
			return;
		}
		int normalized = Math.max(1, Math.min(99, progress));
		if (normalized > safeProgress(job.getProgress())) {
			job.setProgress(normalized);
			transcodingJobRepository.save(job);
		}
	}

	@Transactional
	public void markCompleted(Long jobId) {
		TranscodingJob job = transcodingJobRepository.findById(jobId)
				.orElseThrow(() -> new IllegalArgumentException("Transcoding job not found."));
		job.setStatus(TranscodingJobStatus.COMPLETED);
		job.setProgress(100);
		job.setLastError(null);
		transcodingJobRepository.save(job);
	}

	@Transactional
	public boolean markFailureAndCheckRetry(Long jobId, Exception ex) {
		TranscodingJob job = transcodingJobRepository.findById(jobId)
				.orElseThrow(() -> new IllegalArgumentException("Transcoding job not found."));

		int nextAttempt = safeAttempt(job.getAttempt()) + 1;
		job.setAttempt(nextAttempt);
		job.setLastError(safeError(ex));

		if (nextAttempt >= maxRetries) {
			job.setStatus(TranscodingJobStatus.FAILED);
			job.setProgress(Math.max(0, Math.min(99, safeProgress(job.getProgress()))));
			transcodingJobRepository.save(job);
			return false;
		}

		job.setStatus(TranscodingJobStatus.PENDING);
		job.setProgress(Math.max(0, Math.min(95, safeProgress(job.getProgress()))));
		transcodingJobRepository.save(job);
		return true;
	}

	@Transactional(readOnly = true)
	public TranscodingStatusResponse getVideoStatus(Long videoId) {
		TranscodingJob job = transcodingJobRepository.findByVideoId(videoId)
				.orElse(null);
		if (job == null) {
			return new TranscodingStatusResponse(videoId, "NOT_FOUND", 0, false, "Transcoding job not found.");
		}

		TranscodingJobStatus status = job.getStatus();
		boolean ready = status == TranscodingJobStatus.COMPLETED;
		String message = switch (status) {
			case PENDING -> "Queued for transcoding.";
			case PROCESSING -> "Transcoding is in progress.";
			case COMPLETED -> "Transcoding completed.";
			case FAILED -> job.getLastError() == null ? "Transcoding failed." : job.getLastError();
		};

		return new TranscodingStatusResponse(
				videoId,
				status.name(),
				Math.max(0, Math.min(100, safeProgress(job.getProgress()))),
				ready,
				message
		);
	}

	@Transactional(readOnly = true)
	public TranscodingJobStatus getStatusByVideoPath(String videoPath) {
		return transcodingJobRepository.findByVideoVideoPath(videoPath)
				.map(TranscodingJob::getStatus)
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public TranscodingJob findByVideoId(Long videoId) {
		return transcodingJobRepository.findByVideoId(videoId).orElse(null);
	}

	@Transactional(readOnly = true)
	public TranscodingJob findByVideoPath(String videoPath) {
		return transcodingJobRepository.findByVideoVideoPath(videoPath).orElse(null);
	}

	private int safeProgress(Integer progress) {
		return progress == null ? 0 : progress;
	}

	private int safeAttempt(Integer attempt) {
		return attempt == null ? 0 : attempt;
	}

	private String safeError(Exception ex) {
		if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
			return "Transcoding failed.";
		}
		return ex.getMessage();
	}
}
