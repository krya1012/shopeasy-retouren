package de.shopeasy.messaging.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Producer für Status-Events. Start: {@code ./gradlew :messaging-producer:bootRun} (Broker und Consumer laufen). */
@SpringBootApplication
public class ProducerApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ProducerApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ProducerApplication.class, args);
        int exitCode = 0;
        try {
            context.getBean(DemoScenario.class).run();
            LOG.info("Demo-Szenario vollständig gesendet.");
        } catch (EventNotDeliveredException e) {
            LOG.error("FEHLER: Event nicht zugestellt. {}", e.getMessage());
            exitCode = 1;
        }
        System.exit(SpringApplication.exit(context, () -> 0) + exitCode);
    }
}
