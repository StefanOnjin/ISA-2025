package Jutjubic.RA56.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;

import java.util.HashMap;
import java.util.Map;

@EnableRabbit
@Configuration
public class TranscodingMessagingConfig {

	@Value("${app.transcoding.exchange}")
	private String exchangeName;

	@Value("${app.transcoding.queue}")
	private String queueName;

	@Value("${app.transcoding.routing-key}")
	private String routingKey;

	@Value("${app.transcoding.dlq}")
	private String deadLetterQueueName;

	@Bean
	public DirectExchange transcodingExchange() {
		return new DirectExchange(exchangeName, true, false);
	}

	@Bean
	public Queue transcodingQueue() {
		Map<String, Object> args = new HashMap<>();
		args.put("x-dead-letter-exchange", exchangeName);
		args.put("x-dead-letter-routing-key", deadLetterQueueName);
		return new Queue(queueName, true, false, false, args);
	}

	@Bean
	public Queue transcodingDeadLetterQueue() {
		return new Queue(deadLetterQueueName, true);
	}

	@Bean
	public Binding transcodingBinding(
			@Qualifier("transcodingQueue") Queue transcodingQueue,
			@Qualifier("transcodingExchange") DirectExchange transcodingExchange) {
		return BindingBuilder.bind(transcodingQueue).to(transcodingExchange).with(routingKey);
	}

	@Bean
	public Binding transcodingDeadLetterBinding(
			@Qualifier("transcodingDeadLetterQueue") Queue transcodingDeadLetterQueue,
			@Qualifier("transcodingExchange") DirectExchange transcodingExchange) {
		return BindingBuilder.bind(transcodingDeadLetterQueue).to(transcodingExchange).with(deadLetterQueueName);
	}

	@Bean
	public MessageConverter transcodingMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public SimpleRabbitListenerContainerFactory transcodingListenerContainerFactory(
			ConnectionFactory connectionFactory,
			SimpleRabbitListenerContainerFactoryConfigurer configurer,
			MessageConverter transcodingMessageConverter) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setMessageConverter(transcodingMessageConverter);
		factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
		factory.setPrefetchCount(1);
		factory.setConcurrentConsumers(2);
		factory.setMaxConcurrentConsumers(4);
		factory.setDefaultRequeueRejected(false);
		return factory;
	}
}
