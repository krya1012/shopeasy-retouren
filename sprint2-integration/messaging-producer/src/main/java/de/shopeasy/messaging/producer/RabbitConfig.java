package de.shopeasy.messaging.producer;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

/**
 * Der Producer kennt nur den Exchange und den Routing-Key, keine Queues und keine Empfänger.
 * Die Queues legt der Consumer an (Entkopplung).
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "retouren.status";
    public static final String ROUTING_KEY_PREFIX = "retoure.status.";

    @Bean
    TopicExchange statusExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    JsonMapper jsonMapper() {
        return JsonMapper.builder().build(); // der Producer schreibt nur JSON, er liest keins
    }
}
