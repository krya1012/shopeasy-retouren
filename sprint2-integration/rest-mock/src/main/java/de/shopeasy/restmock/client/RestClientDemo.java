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
 * Command line client for the REST mock. Without arguments it calls every endpoint once (demo).
 * With arguments it makes exactly one call:
 * <pre>
 * list [status=NEW] [marketplace=ZALANDO]   list, the filters are optional
 * get RET-2026-00457                        one return report
 * order ORD-2026-118932                     order data
 * status RET-2026-00457 APPROVED            change the status
 * </pre>
 * Start: {@code ./gradlew :rest-mock:runClient --args="list status=NEW"} (the server must be running).
 * The base URL is http://localhost:8080 by default; change it with the environment variable MOCK_URL.
 *
 * <p>Java note: this client uses the {@link HttpClient} that is part of Java itself (since Java 11), so it needs
 * no extra library. It prints the HTTP status code and the JSON body of every answer.
 */
public class RestClientDemo {

    private static final String USAGE = """
            Usage:
              list [status=NEW] [marketplace=ZALANDO]
              get <returnId>
              order <orderId>
              status <returnId> <STATUS>
              (without arguments: demo with all calls)""";

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
            System.err.println("Server not reachable at " + baseUrl + ": " + e.getMessage());
            System.err.println("Please start the server first: ./gradlew :rest-mock:bootRun");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** @return false if the arguments do not fit */
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
        step("1. All returns", "GET", "/api/v1/returns", null);
        step("2. Only new returns from Zalando", "GET", "/api/v1/returns?status=NEW&marketplace=ZALANDO", null);
        step("3. One return by returnId", "GET", "/api/v1/returns/RET-2026-00457", null);
        step("4. Order data by orderId", "GET", "/api/v1/orders/ORD-2026-118932", null);
        step("5. Set the status to APPROVED", "PATCH", "/api/v1/returns/RET-2026-00457", "{\"status\":\"APPROVED\"}");
        step("6. Error: unknown return (404)", "GET", "/api/v1/returns/RET-DOES-NOT-EXIST", null);
        step("7. Error: invalid status (400)", "PATCH", "/api/v1/returns/RET-2026-00457", "{\"status\":\"NONSENSE\"}");
        step("8. Error: invalid filter (400)", "GET", "/api/v1/returns?marketplace=EBAY", null);
        step("9. Error: forbidden status change REJECTED -> APPROVED (409)", "PATCH",
                "/api/v1/returns/RET-2026-00462", "{\"status\":\"APPROVED\"}");
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
