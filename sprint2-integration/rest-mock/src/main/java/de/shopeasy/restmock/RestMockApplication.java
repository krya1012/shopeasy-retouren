package de.shopeasy.restmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Mock der Marktplatz-Schnittstelle (Amazon, Zalando) für Retourenmeldungen. */
@SpringBootApplication
public class RestMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestMockApplication.class, args);
    }
}
