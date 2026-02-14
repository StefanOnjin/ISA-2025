package Jutjubic.RA56.service;

import Jutjubic.RA56.dto.TranscodingJobMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TranscodingJobProducer {

	private final RabbitTemplate rabbitTemplate;
	private final String exchange;
	private final String routingKey;

	public TranscodingJobProducer(
			RabbitTemplate rabbitTemplate,
			@Value("${app.transcoding.exchange}") String exchange,
			@Value("${app.transcoding.routing-key}") String routingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchange = exchange;
		this.routingKey = routingKey;
	}

	public void publish(TranscodingJobMessage message) {
		rabbitTemplate.convertAndSend(exchange, routingKey, message);
	}
}
