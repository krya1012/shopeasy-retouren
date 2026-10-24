package de.shopeasy.restmock;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mock of the marketplace interface (Amazon, Zalando) for return reports. A "mock" is a fake system with test
 * data that behaves like the real one, so we can build against it before the real one exists.
 *
 * <p><b>Spring Boot basics:</b>
 * <ul>
 *   <li>{@code @SpringBootApplication} switches on auto-configuration: Spring Boot sees spring-webmvc on the
 *       classpath and starts a web server (Tomcat) on port 8080 with everything needed for REST and JSON.</li>
 *   <li>It also scans this package and its sub-packages for classes marked {@code @RestController},
 *       {@code @Component}, ... and creates one object ("bean") of each.</li>
 *   <li>{@code SpringApplication.run} starts it all. Settings are in {@code application.properties}.</li>
 *   <li>{@code @OpenAPIDefinition} (springdoc) gives the generated API description its title and text.
 *       The description is at /v3/api-docs, a page to try the API is at /swagger-ui.html.</li>
 * </ul>
 * Start: {@code ./gradlew :rest-mock:bootRun}.
 */
@OpenAPIDefinition(info = @Info(title = "ShopEasy Return Reports (Mock)", version = "1.0",
        description = "Mock of the marketplace interface for return reports (Amazon, Zalando)"))
@SpringBootApplication
public class RestMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestMockApplication.class, args);
    }
}
