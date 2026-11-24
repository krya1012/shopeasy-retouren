package de.shopeasy.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Job worker application of the returns process (Sprint 4).
 *
 * <p>Spring Boot idea: {@code @SpringBootApplication} starts the application context. The Camunda Spring Boot
 * starter then connects to the Camunda cluster and registers every method with {@code @JobWorker} as a worker for
 * its job type. The workers are added in the next step; the model (process folder) is already complete and is
 * tested with mocked workers (see {@code ReturnsProcessTest}).
 */
@SpringBootApplication
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
