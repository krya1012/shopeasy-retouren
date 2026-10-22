package de.shopeasy.restmock.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Einfacher Client für den REST-Mock. Er ruft alle Endpunkte einmal auf und gibt Statuscode und Antwort aus.
 * Start: {@code ./gradlew :rest-mock:runClient} (der Server muss laufen). Optional die Basis-URL als Argument.
 */
public class RestClientDemo {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final String baseUrl;

    RestClientDemo(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static void main(String[] args) {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        RestClientDemo client = new RestClientDemo(baseUrl);
        try {
            client.run();
        } catch (IOException e) {
            System.err.println("Server nicht erreichbar unter " + baseUrl + ": " + e.getMessage());
            System.err.println("Bitte zuerst den Server starten: ./gradlew :rest-mock:bootRun");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void run() throws IOException, InterruptedException {
        step("1. Alle Retouren", "GET", "/api/v1/returns", null);
        step("2. Nur neue Retouren von Zalando", "GET", "/api/v1/returns?status=NEW&marketplace=ZALANDO", null);
        step("3. Eine Retoure über die returnId", "GET", "/api/v1/returns/RET-2026-00457", null);
        step("4. Bestelldaten über die orderId", "GET", "/api/v1/orders/ZAL-100457", null);
        step("5. Status auf APPROVED setzen", "PATCH", "/api/v1/returns/RET-2026-00457", "{\"status\":\"APPROVED\"}");
        step("6. Fehler: unbekannte Retoure (404)", "GET", "/api/v1/returns/RET-GIBT-ES-NICHT", null);
        step("7. Fehler: ungültiger Status (400)", "PATCH", "/api/v1/returns/RET-2026-00457", "{\"status\":\"QUATSCH\"}");
        step("8. Fehler: ungültiger Filter (400)", "GET", "/api/v1/returns?marketplace=EBAY", null);
    }

    private void step(String title, String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json");
            request.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("\n== " + title);
        System.out.println(method + " " + path + (body == null ? "" : "  " + body));
        System.out.println("-> " + response.statusCode());
        System.out.println(response.body());
    }
}
