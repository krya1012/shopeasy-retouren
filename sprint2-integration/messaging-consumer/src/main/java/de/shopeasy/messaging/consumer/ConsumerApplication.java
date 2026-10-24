package de.shopeasy.messaging.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the consumer. Start it with {@code ./gradlew :messaging-consumer:bootRun}.
 *
 * <p><b>Spring Boot basics</b>
 * <ul>
 *   <li>{@code @SpringBootApplication} switches on "auto-configuration": Spring Boot looks at the libraries on the
 *       classpath (here: spring-amqp) and sets up what we need, for example the connection to RabbitMQ.</li>
 *   <li>It also scans this package and all sub-packages for classes marked with {@code @Component},
 *       {@code @Configuration} and similar. Spring creates one object ("bean") of each class and keeps it.</li>
 *   <li>{@code SpringApplication.run} starts all of this. The program then keeps running, because the
 *       {@code @RabbitListener} methods in {@link StatusListeners} wait for messages.</li>
 * </ul>
 * Settings (RabbitMQ address, log format, ...) are in {@code src/main/resources/application.properties}.
 */
@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
