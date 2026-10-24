package de.shopeasy.messaging.consumer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * The layout ("topology") in the RabbitMQ broker. The consumer creates it at startup.
 *
 * <pre>
 * Producer --&gt; exchange returns.status (topic)
 *               |-- return.status.#  --&gt; queue management    (all status changes)
 *               |-- ...APPROVED/REJECTED/ACCEPTED/REFUNDED --&gt; queue customer-info
 *             rejected messages --&gt; exchange returns.status.dlx --&gt; queue dlq
 * </pre>
 *
 * <p><b>RabbitMQ basics:</b> a producer never sends to a queue directly. It sends to an <i>exchange</i> with a
 * <i>routing key</i> (here e.g. {@code return.status.APPROVED}). The exchange forwards the message to every queue
 * whose <i>binding</i> matches the key. In a topic exchange {@code #} means "any number of words". So the
 * producer does not know who receives its events.
 *
 * <p><b>Spring Boot basics:</b> {@code @Configuration} marks a class that creates beans. Every method marked
 * {@code @Bean} is called once at startup, and its result becomes a bean. Spring AMQP finds these exchange, queue
 * and binding beans and declares them in the broker automatically.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "returns.status";
    public static final String DEAD_LETTER_EXCHANGE = "returns.status.dlx";
    public static final String QUEUE_MANAGEMENT = "returns.status.management";
    public static final String QUEUE_CUSTOMER_INFO = "returns.status.customer-info";
    public static final String QUEUE_DLQ = "returns.status.dlq";
    private static final String DLQ_ROUTING_KEY = "dead-letter";

    /** The topic exchange the producer sends to. "Durable" = it survives a broker restart. */
    @Bean
    TopicExchange statusExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /** Exchange for rejected messages (DLX = dead letter exchange). */
    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue managementQueue() {
        return withDeadLetter(QUEUE_MANAGEMENT);
    }

    @Bean
    Queue customerInfoQueue() {
        return withDeadLetter(QUEUE_CUSTOMER_INFO);
    }

    /** The dead letter queue (DLQ): rejected messages are collected here. */
    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    /** The management queue gets all status changes. */
    @Bean
    Binding managementBinding() {
        return BindingBuilder.bind(managementQueue()).to(statusExchange()).with("return.status.#");
    }

    /** The customer-info queue only gets the status changes customers are informed about. */
    @Bean
    Declarables customerInfoBindings() {
        return new Declarables(
                customerInfoBinding("APPROVED"),
                customerInfoBinding("REJECTED"),
                customerInfoBinding("ACCEPTED"),
                customerInfoBinding("REFUNDED"));
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }

    /** Reads and writes JSON. Used by {@link EventParser}. */
    @Bean
    JsonMapper jsonMapper() {
        // Unknown fields are ignored, so the producer may add fields later (tolerant reader)
        return JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    private Binding customerInfoBinding(String status) {
        return BindingBuilder.bind(customerInfoQueue()).to(statusExchange()).with("return.status." + status);
    }

    /** A durable queue whose rejected messages are forwarded to the dead letter exchange. */
    private static Queue withDeadLetter(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }
}
