package de.shopeasy.workflow;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Application for the tests of the model with MOCKED workers ({@code ReturnsProcessTest}). It has no component scan,
 * so the real workers of the application are not started and do not compete with the mocks of the test.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class ProcessTestApplication {
}
