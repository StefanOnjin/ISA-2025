package Jutjubic.UploadBenchmark.service;

import Jutjubic.UploadBenchmark.dto.UploadEventJsonMessage;
import Jutjubic.UploadBenchmark.proto.UploadEventProto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class UploadEventBenchmarkService {
	private static final long MIN_MESSAGES = 50;

	private final ObjectMapper objectMapper;
	private final AtomicLong jsonCount = new AtomicLong();
	private final AtomicLong jsonSerializeNanos = new AtomicLong();
	private final AtomicLong jsonDeserializeNanos = new AtomicLong();
	private final AtomicLong jsonBytes = new AtomicLong();

	private final AtomicLong protobufCount = new AtomicLong();
	private final AtomicLong protobufSerializeNanos = new AtomicLong();
	private final AtomicLong protobufDeserializeNanos = new AtomicLong();
	private final AtomicLong protobufBytes = new AtomicLong();

	private final AtomicBoolean summaryLogged = new AtomicBoolean(false);

	public UploadEventBenchmarkService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void handleJson(byte[] body) {
		try {
			long deserializeStart = System.nanoTime();
			UploadEventJsonMessage event = objectMapper.readValue(body, UploadEventJsonMessage.class);
			long deserializeNanos = System.nanoTime() - deserializeStart;

			long serializeStart = System.nanoTime();
			byte[] serialized = objectMapper.writeValueAsBytes(event);
			long serializeNanos = System.nanoTime() - serializeStart;

			jsonCount.incrementAndGet();
			jsonDeserializeNanos.addAndGet(deserializeNanos);
			jsonSerializeNanos.addAndGet(serializeNanos);
			jsonBytes.addAndGet(body.length > 0 ? body.length : serialized.length);
			logSummaryIfReady();
		} catch (Exception ignored) {
		}
	}

	public void handleProtobuf(byte[] body) {
		try {
			long deserializeStart = System.nanoTime();
			UploadEventProto.UploadEvent event = UploadEventProto.UploadEvent.parseFrom(body);
			long deserializeNanos = System.nanoTime() - deserializeStart;

			long serializeStart = System.nanoTime();
			byte[] serialized = event.toByteArray();
			long serializeNanos = System.nanoTime() - serializeStart;

			protobufCount.incrementAndGet();
			protobufDeserializeNanos.addAndGet(deserializeNanos);
			protobufSerializeNanos.addAndGet(serializeNanos);
			protobufBytes.addAndGet(body.length > 0 ? body.length : serialized.length);
			logSummaryIfReady();
		} catch (Exception ignored) {
		}
	}

	public String buildReport() {
		long jCount = jsonCount.get();
		long pCount = protobufCount.get();
		StringBuilder sb = new StringBuilder();
		sb.append(formatLine("JSON", jCount, jsonSerializeNanos.get(), jsonDeserializeNanos.get(), jsonBytes.get()));
		sb.append('\n');
		sb.append(formatLine("Protobuf", pCount, protobufSerializeNanos.get(), protobufDeserializeNanos.get(), protobufBytes.get()));
		sb.append('\n');
		sb.append("Ready for comparison: ").append(jCount >= MIN_MESSAGES && pCount >= MIN_MESSAGES ? "YES" : "NO");
		return sb.toString();
	}

	private String formatLine(String format, long count, long serTotalNs, long deserTotalNs, long bytesTotal) {
		double avgSerMicros = count == 0 ? 0.0 : (serTotalNs / (double) count) / 1_000.0;
		double avgDeserMicros = count == 0 ? 0.0 : (deserTotalNs / (double) count) / 1_000.0;
		double avgBytes = count == 0 ? 0.0 : bytesTotal / (double) count;
		return String.format(
				"%s -> count=%d | avgSerialize=%.3f us | avgDeserialize=%.3f us | avgSize=%.2f bytes",
				format, count, avgSerMicros, avgDeserMicros, avgBytes
		);
	}

	private void logSummaryIfReady() {
		if (summaryLogged.get()) {
			return;
		}
		if (jsonCount.get() < MIN_MESSAGES || protobufCount.get() < MIN_MESSAGES) {
			return;
		}
		if (summaryLogged.compareAndSet(false, true)) {
			System.out.println();
			System.out.println("=== BENCHMARK SUMMARY ===");
			System.out.println(buildReport());
			System.out.println("=========================");
		}
	}
}
