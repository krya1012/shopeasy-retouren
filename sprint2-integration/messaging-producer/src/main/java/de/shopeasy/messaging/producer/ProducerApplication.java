package de.shopeasy.messaging.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Producer for status events. Start: {@code ./gradlew :messaging-producer:bootRun} (broker and consumer must be
 * running). The program plays the demo scenario once and then ends.
 *
 * <p><b>Spring Boot basics:</b>
 * <ul>
 *   <li>{@code @SpringBootApplication} switches on auto-configuration (Spring Boot sets up the RabbitMQ connection
 *       because spring-amqp is on the classpath) and scans this package for {@code @Component} and
 *       {@code @Configuration} classes.</li>
 *   <li>{@code SpringApplication.run} starts Spring and returns the "context", the container that holds all beans.
 *       {@code context.getBean(...)} fetches one of them.</li>
 *   <li>The exit code tells the caller whether it worked: 0 = ok, 1 = an event was not delivered.</li>
 * </ul>
 */
@SpringBootApplication
public class ProducerApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ProducerApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ProducerApplication.class, args);
        int exitCode = 0;
        try {
            context.getBean(DemoScenario.class).run();
            LOG.info("Demo scenario sent completely.");
        } catch (EventNotDeliveredException e) {
            LOG.error("ERROR: event not delivered. {}", e.getMessage());
            exitCode = 1;
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
