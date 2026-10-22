package de.shopeasy.restmock;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Mock der Marktplatz-Schnittstelle (Amazon, Zalando) für Retourenmeldungen. */
@OpenAPIDefinition(info = @Info(title = "ShopEasy Retourenmeldungen (Mock)", version = "1.0",
        description = "Mock der Marktplatz-Schnittstelle für Retourenmeldungen (Amazon, Zalando)"))
@SpringBootApplication
public class RestMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestMockApplication.class, args);
    }
}
