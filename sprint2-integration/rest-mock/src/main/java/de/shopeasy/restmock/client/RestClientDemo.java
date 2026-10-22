package de.shopeasy.restmock.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Client für den REST-Mock. Ohne Argumente ruft er alle Endpunkte einmal auf (Demo).
 * Mit Argumenten macht er genau eine Abfrage:
 * <pre>
 * list [status=NEW] [marketplace=ZALANDO]   Liste, Filter sind optional
 * get RET-2026-00457                        eine Retourenmeldung
 * order ORD-2026-118932                     Bestelldaten
 * status RET-2026-00457 APPROVED            Status ändern
 * </pre>
 * Start: {@code ./gradlew :rest-mock:runClient --args="list status=NEW"} (der Server muss laufen).
 * Die Basis-URL steht standardmäßig auf http://localhost:8080, mit der Umgebungsvariable MOCK_URL änderbar.
 */
public class RestClientDemo {

    private static final String USAGE = """
            Benutzung:
              list [status=NEW] [marketplace=ZALANDO]
              get <returnId>
              order <orderId>
              status <returnId> <STATUS>
              (ohne Argumente: Demo mit allen Aufrufen)""";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final String baseUrl;

    RestClientDemo(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static void main(String[] args) {
        String baseUrl = System.getenv().getOrDefault("MOCK_URL", "http://localhost:8080");
        RestClientDemo client = new RestClientDemo(baseUrl);
        try {
            if (args.length == 0) {
                client.demo();
            } else if (!client.command(args)) {
                System.err.println(USAGE);
                System.exit(2);
            }
        } catch (IOException e) {
            System.err.println("Server nicht erreichbar unter " + baseUrl + ": " + e.getMessage());
            System.err.println("Bitte zuerst den Server starten: ./gradlew :rest-mock:bootRun");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** @return false, wenn die Argumente nicht passen */
    private boolean command(String[] args) throws IOException, InterruptedException {
        switch (args[0]) {
            case "list" -> {
                List<String> filters = new ArrayList<>();
                for (int i = 1; i < args.length; i++) {
                    String[] pair = args[i].split("=", 2);
                    if (pair.length != 2 || !(pair[0].equals("status") || pair[0].equals("marketplace"))) {
                        return false;
                    }
                    filters.add(pair[0] + "=" + URLEncoder.encode(pair[1], StandardCharsets.UTF_8));
                }
                String query = filters.isEmpty() ? "" : "?" + String.join("&", filters);
                call("GET", "/api/v1/returns" + query, null);
            }
            case "get" -> {
                if (args.length != 2) {
                    return false;
                }
                call("GET", "/api/v1/returns/" + encode(args[1]), null);
            }
            case "order" -> {
                if (args.length != 2) {
                    return false;
                }
                call("GET", "/api/v1/orders/" + encode(args[1]), null);
            }
            case "status" -> {
                if (args.length != 3) {
                    return false;
                }
                call("PATCH", "/api/v1/returns/" + encode(args[1]), "{\"status\":\"" + args[2] + "\"}");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void demo() throws IOException, InterruptedException {
        step("1. Alle Retouren", "GET", "/api/v1/returns", null);
        step("2. Nur neue Retouren von Zalando", "GET", "/api/v1/returns?status=NEW&marketplace=ZALANDO", null);
        step("3. Eine Retoure über die returnId", "GET", "/api/v1/returns/RET-2026-00457", null);
        step("4. Bestelldaten über die orderId", "GET", "/api/v1/orders/ORD-2026-118932", null);
        step("5. Status auf APPROVED setzen", "PATCH", "/api/v1/returns/RET-2026-00457", "{\"status\":\"APPROVED\"}");
        step("6. Fehler: unbekannte Retoure (404)", "GET", "/api/v1/returns/RET-GIBT-ES-NICHT", null);
        step("7. Fehler: ungültiger Status (400)", "PATCH", "/api/v1/returns/RET-2026-00457", "{\"status\":\"QUATSCH\"}");
        step("8. Fehler: ungültiger Filter (400)", "GET", "/api/v1/returns?marketplace=EBAY", null);
    }

    private void step(String title, String method, String path, String body) throws IOException, InterruptedException {
        System.out.println("\n== " + title);
        call(method, path, body);
    }

    private void call(String method, String path, String body) throws IOException, InterruptedException {
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
        System.out.println(method + " " + path + (body == null ? "" : "  " + body));
        System.out.println("-> " + response.statusCode());
        System.out.println(response.body());
    }

    private static String encode(String pathPart) {
        return URLEncoder.encode(pathPart, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
