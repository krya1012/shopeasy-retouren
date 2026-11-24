package de.shopeasy.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Job worker application of the returns process (Sprint 4).
 *
 * <p>Spring Boot idea: {@code @SpringBootApplication} starts the application context and scans this package for
 * classes marked {@code @Component}. The Camunda Spring Boot starter connects to the Camunda cluster and registers
 * every method with {@code @JobWorker} as a worker for its job type: it asks the engine for jobs of that type, calls
 * the method with the process variables, and reports the result (variables, a BPMN error or a failure) back.
 *
 * <ul>
 *   <li>{@code @ConfigurationPropertiesScan}: the settings under {@code workflow.*} are read into
 *       {@link de.shopeasy.workflow.config.WorkflowProperties}.</li>
 *   <li>{@code @EnableScheduling}: switches on {@code @Scheduled} methods (the marketplace poller).</li>
 * </ul>
 * Start: {@code ./gradlew bootRun} in this folder (the model is deployed with {@code c8ctl deploy}, see README).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
