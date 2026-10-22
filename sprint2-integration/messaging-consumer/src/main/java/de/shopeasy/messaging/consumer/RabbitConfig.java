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
 * Die Topologie im Broker. Der Consumer legt sie beim Start an.
 *
 * <pre>
 * Producer --&gt; Exchange retouren.status (Topic)
 *               |-- retoure.status.#  --&gt; Queue verwaltung  (alle Statuswechsel)
 *               |-- ...APPROVED/REJECTED/ACCEPTED/REFUNDED --&gt; Queue kundeninfo
 *             abgelehnte Nachrichten --&gt; Exchange retouren.status.dlx --&gt; Queue dlq
 * </pre>
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "retouren.status";
    public static final String DEAD_LETTER_EXCHANGE = "retouren.status.dlx";
    public static final String QUEUE_VERWALTUNG = "retouren.status.verwaltung";
    public static final String QUEUE_KUNDENINFO = "retouren.status.kundeninfo";
    public static final String QUEUE_DLQ = "retouren.status.dlq";
    private static final String DLQ_ROUTING_KEY = "fehler";

    @Bean
    TopicExchange statusExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue verwaltungQueue() {
        return withDeadLetter(QUEUE_VERWALTUNG);
    }

    @Bean
    Queue kundeninfoQueue() {
        return withDeadLetter(QUEUE_KUNDENINFO);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    Binding verwaltungBinding() {
        return BindingBuilder.bind(verwaltungQueue()).to(statusExchange()).with("retoure.status.#");
    }

    /** Der Kundeninfo-Empfänger bekommt nur die Statuswechsel, über die Kund:innen informiert werden. */
    @Bean
    Declarables kundeninfoBindings() {
        return new Declarables(
                kundeninfoBinding("APPROVED"),
                kundeninfoBinding("REJECTED"),
                kundeninfoBinding("ACCEPTED"),
                kundeninfoBinding("REFUNDED"));
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ_ROUTING_KEY);
    }

    @Bean
    JsonMapper jsonMapper() {
        // Unbekannte Felder werden ignoriert, damit der Producer das Event erweitern kann (tolerant reader)
        return JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    private Binding kundeninfoBinding(String status) {
        return BindingBuilder.bind(kundeninfoQueue()).to(statusExchange()).with("retoure.status." + status);
    }

    private static Queue withDeadLetter(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }
}
