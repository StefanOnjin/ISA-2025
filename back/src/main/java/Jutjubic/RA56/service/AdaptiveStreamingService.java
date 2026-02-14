package Jutjubic.RA56.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AdaptiveStreamingService {
	private static final Logger logger = LoggerFactory.getLogger(AdaptiveStreamingService.class);

	private final Path streamRoot;
	private final String ffmpegBinary;

	public AdaptiveStreamingService(@Value("${app.ffmpeg.binary:ffmpeg}") String ffmpegBinary) {
		this.streamRoot = Paths.get("storage/streams").toAbsolutePath().normalize();
		this.ffmpegBinary = ffmpegBinary;
		try {
			Files.createDirectories(streamRoot);
		} catch (IOException ex) {
			throw new RuntimeException("Failed to initialize adaptive stream storage directory.", ex);
		}
	}

	public void ensureAdaptiveStreams(String videoFileName, Path sourceVideoPath) {
		Path hlsDir = getHlsDirectory(videoFileName);
		Path dashDir = getDashDirectory(videoFileName);
		Path hlsManifest = getHlsManifestPath(videoFileName);
		Path dashManifest = getDashManifestPath(videoFileName);
		boolean hlsReady = Files.exists(hlsManifest) && hasHlsSegments(hlsDir);
		boolean dashReady = Files.exists(dashManifest) && hasDashSegments(dashDir);
		if (hlsReady && dashReady) {
			return;
		}

		try {
			Files.createDirectories(hlsDir);
			Files.createDirectories(dashDir);
		} catch (IOException ex) {
			throw new RuntimeException("Failed creating adaptive stream directories for video: " + videoFileName, ex);
		}

		if (!hlsReady) {
			runCommand(buildHlsCommand(sourceVideoPath, hlsManifest), hlsManifest.getParent());
		}
		if (!dashReady) {
			runCommand(buildDashCommand(sourceVideoPath, dashManifest), dashManifest.getParent());
		}
	}

	@Async
	public void ensureAdaptiveStreamsAsync(String videoFileName, Path sourceVideoPath) {
		try {
			ensureAdaptiveStreams(videoFileName, sourceVideoPath);
		} catch (Exception ex) {
			logger.error("Failed generating adaptive streams for {}", videoFileName, ex);
		}
	}

	public Path resolveHlsResource(String videoFileName, String resourcePath) {
		return resolveResource(getHlsDirectory(videoFileName), resourcePath);
	}

	public Path resolveDashResource(String videoFileName, String resourcePath) {
		return resolveResource(getDashDirectory(videoFileName), resourcePath);
	}

	public Path getHlsManifestPath(String videoFileName) {
		return getHlsDirectory(videoFileName).resolve("master.m3u8");
	}

	public Path getDashManifestPath(String videoFileName) {
		return getDashDirectory(videoFileName).resolve("manifest.mpd");
	}

	private Path getVideoDir(String videoFileName) {
		return streamRoot.resolve(videoFileName).normalize();
	}

	private Path getHlsDirectory(String videoFileName) {
		return getVideoDir(videoFileName).resolve("hls").normalize();
	}

	private Path getDashDirectory(String videoFileName) {
		return getVideoDir(videoFileName).resolve("dash").normalize();
	}

	private Path resolveResource(Path root, String relativePath) {
		Path resolved = root.resolve(relativePath).normalize();
		if (!resolved.startsWith(root)) {
			throw new IllegalArgumentException("Invalid adaptive stream resource path.");
		}
		return resolved;
	}

	private boolean hasHlsSegments(Path hlsDir) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(hlsDir, "*.ts")) {
			return stream.iterator().hasNext();
		} catch (IOException ex) {
			return false;
		}
	}

	private boolean hasDashSegments(Path dashDir) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dashDir, "*.m4s")) {
			return stream.iterator().hasNext();
		} catch (IOException ex) {
			return false;
		}
	}

	private List<String> buildHlsCommand(Path inputPath, Path manifestPath) {
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

	private List<String> buildDashCommand(Path inputPath, Path manifestPath) {
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
		command.add("-f");
		command.add("dash");
		command.add("-seg_duration");
		command.add("4");
		command.add("-use_timeline");
		command.add("1");
		command.add("-use_template");
		command.add("1");
		command.add("-init_seg_name");
		command.add("init-$RepresentationID$.m4s");
		command.add("-media_seg_name");
		command.add("chunk-$RepresentationID$-$Number%05d$.m4s");
		command.add(manifestPath.getFileName().toString());
		return command;
	}

	private void runCommand(List<String> command, Path workingDirectory) {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(workingDirectory.toFile());
		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

		try {
			Process process = processBuilder.start();
			boolean completed = process.waitFor(10, TimeUnit.MINUTES);
			if (!completed) {
				process.destroyForcibly();
				throw new RuntimeException("FFmpeg timed out. Command: " + String.join(" ", command));
			}
			int exitCode = process.exitValue();
			if (exitCode != 0) {
				throw new RuntimeException("FFmpeg exited with code " + exitCode + ". Command: " + String.join(" ", command));
			}
		} catch (IOException ex) {
			throw new RuntimeException("Failed to start FFmpeg process. Ensure ffmpeg is installed and available.", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("FFmpeg process was interrupted.", ex);
		}
	}
}
