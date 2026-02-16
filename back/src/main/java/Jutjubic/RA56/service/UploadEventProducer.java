package Jutjubic.RA56.service;

import Jutjubic.RA56.dto.UploadEventJsonMessage;
import Jutjubic.RA56.proto.UploadEventProto;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UploadEventProducer {
	private final RabbitTemplate rabbitTemplate;
	private final String exchange;
	private final String jsonRoutingKey;
	private final String protobufRoutingKey;

	public UploadEventProducer(
			RabbitTemplate rabbitTemplate,
			@Value("${app.upload-events.exchange}") String exchange,
			@Value("${app.upload-events.json.routing-key}") String jsonRoutingKey,
			@Value("${app.upload-events.protobuf.routing-key}") String protobufRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchange = exchange;
		this.jsonRoutingKey = jsonRoutingKey;
		this.protobufRoutingKey = protobufRoutingKey;
	}

	public void publishJson(UploadEventJsonMessage payload) {
		rabbitTemplate.convertAndSend(exchange, jsonRoutingKey, payload);
	}

	public void publishProtobuf(UploadEventProto.UploadEvent payload) {
		MessageProperties properties = new MessageProperties();
		properties.setContentType("application/x-protobuf");
		Message message = new Message(payload.toByteArray(), properties);
		rabbitTemplate.send(exchange, protobufRoutingKey, message);
	}
}
