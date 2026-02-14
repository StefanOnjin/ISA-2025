package Jutjubic.RA56.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

@Service
public class TranscodingWorkerService {

	private final String ffmpegBinary;
	private final Path streamRoot;

	public TranscodingWorkerService(@Value("${app.ffmpeg.binary:ffmpeg}") String ffmpegBinary) {
		this.ffmpegBinary = ffmpegBinary;
		this.streamRoot = Paths.get("storage/streams").toAbsolutePath().normalize();
	}

	public void transcodeToHls(String videoFileName, Path sourcePath, long durationSeconds, IntConsumer onProgress) {
		Path hlsDir = streamRoot.resolve(videoFileName).resolve("hls").normalize();
		Path manifestPath = hlsDir.resolve("master.m3u8");

		try {
			Files.createDirectories(hlsDir);
		} catch (IOException ex) {
			throw new RuntimeException("Failed creating transcoding directory.", ex);
		}

		List<String> command = buildCommand(sourcePath, manifestPath);
		runCommand(command, hlsDir, Math.max(1L, durationSeconds), onProgress);
	}

	private List<String> buildCommand(Path inputPath, Path manifestPath) {
		Path hlsDir = manifestPath.getParent();
		List<String> command = new ArrayList<>();
		command.add(ffmpegBinary);
		command.add("-y");
		command.add("-i");
		command.add(inputPath.toString());
		command.add("-map");
		command.add("0:v:0");
		command.add("-map");
		command.add("0:a?");
		command.add("-c:v");
		command.add("libx264");
		command.add("-c:a");
		command.add("aac");
		command.add("-preset");
		command.add("veryfast");
		command.add("-progress");
		command.add("pipe:1");
		command.add("-nostats");
		command.add("-f");
		command.add("hls");
		command.add("-hls_time");
		command.add("4");
		command.add("-hls_playlist_type");
		command.add("vod");
		command.add("-hls_segment_filename");
		command.add(hlsDir.resolve("segment_%03d.ts").toString());
		command.add(manifestPath.toString());
		return command;
	}

	private void runCommand(List<String> command, Path workingDirectory, long durationSeconds, IntConsumer onProgress) {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(workingDirectory.toFile());
		processBuilder.redirectErrorStream(true);

		try {
			Process process = processBuilder.start();
			readProgress(process, durationSeconds, onProgress);
			boolean completed = process.waitFor(10, TimeUnit.MINUTES);
			if (!completed) {
				process.destroyForcibly();
				throw new RuntimeException("FFmpeg timed out.");
			}
			int exitCode = process.exitValue();
			if (exitCode != 0) {
				throw new RuntimeException("FFmpeg exited with code " + exitCode + ".");
			}
			onProgress.accept(100);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to start FFmpeg process. Ensure ffmpeg is installed and available.", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("FFmpeg process was interrupted.", ex);
		}
	}

	private void readProgress(Process process, long durationSeconds, IntConsumer onProgress) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("out_time_ms=")) {
					continue;
				}
				long outTimeUs;
				try {
					outTimeUs = Long.parseLong(line.substring("out_time_ms=".length()));
				} catch (NumberFormatException ignored) {
					continue;
				}
				if (outTimeUs <= 0) {
					continue;
				}
				long outSeconds = outTimeUs / 1_000_000L;
				int progress = (int) Math.min(99L, (outSeconds * 100L) / durationSeconds);
				onProgress.accept(progress);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Unable to read FFmpeg progress output.", ex);
		}
	}
}
