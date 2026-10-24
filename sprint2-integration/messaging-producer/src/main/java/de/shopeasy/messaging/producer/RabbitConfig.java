package de.shopeasy.messaging.producer;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

/**
 * The producer only knows the exchange and the routing key. It knows no queues and no receivers. The consumer
 * creates the queues and bindings (decoupling): a new receiver needs no change here.
 *
 * <p><b>Spring Boot basics:</b> {@code @Configuration} marks a class that creates beans. Every method marked
 * {@code @Bean} is called once at startup and its result is kept by Spring. Spring AMQP declares the
 * {@link TopicExchange} bean in the broker (if it does not exist yet).
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "returns.status";
    public static final String ROUTING_KEY_PREFIX = "return.status.";

    /** The exchange we send to. "Durable" = it survives a broker restart. */
    @Bean
    TopicExchange statusExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /** Turns {@link StatusEvent} objects into JSON. */
    @Bean
    JsonMapper jsonMapper() {
        return JsonMapper.builder().build(); // the producer only writes JSON, it never reads any
    }
}
