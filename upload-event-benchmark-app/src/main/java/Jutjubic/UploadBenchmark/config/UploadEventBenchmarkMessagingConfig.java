package Jutjubic.UploadBenchmark.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class UploadEventBenchmarkMessagingConfig {
	@Value("${app.upload-events.exchange}")
	private String exchangeName;

	@Value("${app.upload-events.json.queue}")
	private String jsonQueueName;

	@Value("${app.upload-events.json.routing-key}")
	private String jsonRoutingKey;

	@Value("${app.upload-events.protobuf.queue}")
	private String protobufQueueName;

	@Value("${app.upload-events.protobuf.routing-key}")
	private String protobufRoutingKey;

	@Bean
	public DirectExchange uploadEventExchange() {
		return new DirectExchange(exchangeName, true, false);
	}

	@Bean
	public Queue uploadEventJsonQueue() {
		return new Queue(jsonQueueName, true);
	}

	@Bean
	public Queue uploadEventProtobufQueue() {
		return new Queue(protobufQueueName, true);
	}

	@Bean
	public Binding uploadEventJsonBinding(
			@Qualifier("uploadEventJsonQueue") Queue queue,
			@Qualifier("uploadEventExchange") DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(jsonRoutingKey);
	}

	@Bean
	public Binding uploadEventProtobufBinding(
			@Qualifier("uploadEventProtobufQueue") Queue queue,
			@Qualifier("uploadEventExchange") DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(protobufRoutingKey);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
