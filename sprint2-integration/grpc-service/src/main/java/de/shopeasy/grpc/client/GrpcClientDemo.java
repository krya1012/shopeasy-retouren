package de.shopeasy.grpc.client;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import de.shopeasy.grpc.ConditionCheckRequest;
import de.shopeasy.grpc.ConditionCheckResponse;
import de.shopeasy.grpc.ObservedCondition;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Client für den ArticleConditionService. Ohne Argumente spielt er alle Szenarien durch (Demo):
 * Annahme, Ablehnung, Grenzfall, Stapel, Fehlerfälle. Mit Argumenten macht er genau eine Abfrage:
 * <pre>
 * check RET-2026-00457 SH-BLZ-042-M AS_NEW [tags=false]   Zustand prüfen (tags=true ist Standard)
 * get RET-2026-00457 SH-BLZ-042-M                          früheres Ergebnis abrufen
 * </pre>
 * Start: {@code ./gradlew :grpc-service:runClient --args="get RET-A SKU"} (der Server muss laufen).
 * Adresse standardmäßig localhost:9090, mit der Umgebungsvariable GRPC_TARGET änderbar (z. B. "rechner:9090").
 */
public class GrpcClientDemo {

    private static final String USAGE = """
            Benutzung:
              check <returnId> <sku> <ZUSTAND> [tags=true|false]
                ZUSTAND: AS_NEW, MINOR_WEAR, VISIBLY_WORN, DAMAGED, MISSING_TAGS, MISSING_PARTS
              get <returnId> <sku>
              (ohne Argumente: Demo mit allen Szenarien)""";

    public static void main(String[] args) throws InterruptedException {
        String target = System.getenv().getOrDefault("GRPC_TARGET", "localhost:9090");
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        int exitCode = 0;
        try {
            GrpcClientDemo demo = new GrpcClientDemo(new ConditionClient(channel));
            if (args.length == 0) {
                demo.run();
            } else if (!demo.command(args)) {
                System.err.println(USAGE);
                exitCode = 2;
            }
        } finally {
            channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
        }
        System.exit(exitCode);
    }

    private final ConditionClient client;
    /** Macht die return_ids je Lauf eindeutig, damit ein zweiter Lauf nicht an Duplikaten scheitert. */
    private final String run = String.valueOf(System.currentTimeMillis() / 1000 % 100000);

    GrpcClientDemo(ConditionClient client) {
        this.client = client;
    }

    /** @return false, wenn die Argumente nicht passen */
    private boolean command(String[] args) {
        switch (args[0]) {
            case "check" -> {
                if (args.length < 4 || args.length > 5) {
                    return false;
                }
                ObservedCondition condition;
                try {
                    condition = ObservedCondition.valueOf(args[3]);
                } catch (IllegalArgumentException e) {
                    return false;
                }
                boolean tags = args.length < 5 || !args[4].equals("tags=false");
                show("Prüfung " + args[1] + " / " + args[2],
                        () -> client.checkCondition(request(args[1], args[2], condition, tags)));
            }
            case "get" -> {
                if (args.length != 3) {
                    return false;
                }
                show("Ergebnis " + args[1] + " / " + args[2], () -> client.getCheckResult(args[1], args[2]));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void run() {
        // Szenarien mit Entscheidung
        show("1. Annahme: neuwertig, Etikett vorhanden",
                () -> client.checkCondition(request("RET-A-" + run, "SH-BLZ-042-M", ObservedCondition.AS_NEW, true)));
        show("2. Ablehnung: beschädigt",
                () -> client.checkCondition(request("RET-B-" + run, "SH-JKT-088-L", ObservedCondition.DAMAGED, true)));
        show("3. Grenzfall: leichte Gebrauchsspuren",
                () -> client.checkCondition(request("RET-C-" + run, "SH-DRS-017-S", ObservedCondition.MINOR_WEAR, true)));
        show("4. Grenzfall: neuwertig, aber Etikett fehlt",
                () -> client.checkCondition(request("RET-D-" + run, "SH-PNT-031-32", ObservedCondition.AS_NEW, false)));

        // Stapel und Abruf
        show("5. Stapel mit zwei Artikeln", () -> client.checkConditionBatch(List.of(
                request("RET-E-" + run, "SH-SNK-120-42", ObservedCondition.AS_NEW, true),
                request("RET-E-" + run, "SH-SHT-009-M", ObservedCondition.VISIBLY_WORN, true))));
        show("6. Früheres Ergebnis abrufen", () -> client.getCheckResult("RET-A-" + run, "SH-BLZ-042-M"));

        // Fehlerfälle
        show("7. Fehler: dieselbe Prüfung noch einmal (ALREADY_EXISTS)",
                () -> client.checkCondition(request("RET-A-" + run, "SH-BLZ-042-M", ObservedCondition.AS_NEW, true)));
        show("8. Fehler: Zustand fehlt (INVALID_ARGUMENT)",
                () -> client.checkCondition(request("RET-F-" + run, "SH-BLT-003-U",
                        ObservedCondition.OBSERVED_CONDITION_UNSPECIFIED, true)));
        show("9. Fehler: Stapel mit falscher zweiter Position (INVALID_ARGUMENT, nichts wird gespeichert)",
                () -> client.checkConditionBatch(List.of(
                        request("RET-G-" + run, "SH-SNK-120-42", ObservedCondition.AS_NEW, true),
                        request("", "SH-SHT-009-M", ObservedCondition.AS_NEW, true))));
        show("10. Fehler: Ergebnis gibt es nicht (NOT_FOUND)",
                () -> client.getCheckResult("RET-GIBT-ES-NICHT", "SH-X"));
        show("11. Ausfall: Service kurz nicht verfügbar (UNAVAILABLE), der Client wiederholt",
                () -> client.checkCondition(request("RET-FAIL-" + run, "SH-BLZ-042-M", ObservedCondition.AS_NEW, true)));
        show("12. Frist: Service antwortet zu langsam (DEADLINE_EXCEEDED), der Client wiederholt nicht",
                () -> client.checkCondition(request("RET-SLOW-" + run, "SH-BLZ-042-M", ObservedCondition.AS_NEW, true)));
    }

    private static ConditionCheckRequest request(String returnId, String sku, ObservedCondition condition,
                                                 boolean tagsPresent) {
        return ConditionCheckRequest.newBuilder()
                .setReturnId(returnId)
                .setSku(sku)
                .setObservedCondition(condition)
                .setTagsPresent(tagsPresent)
                .setOriginalPackaging(true)
                .setInspectorNote("Demo")
                .setInspectedAt(Instant.now().toString())
                .build();
    }

    private static void show(String title, Supplier<Object> call) {
        System.out.println("\n== " + title);
        try {
            Object result = call.get();
            if (result instanceof List<?> list) {
                list.forEach(r -> System.out.println("-> " + format((ConditionCheckResponse) r)));
            } else {
                System.out.println("-> " + format((ConditionCheckResponse) result));
            }
        } catch (ConditionClientException e) {
            System.out.println("-> FEHLER [" + e.getCode() + "] " + e.getMessage());
        }
    }

    private static String format(ConditionCheckResponse r) {
        return r.getReturnId() + " / " + r.getSku() + ": " + r.getDecision()
                + " (Score " + r.getConfidenceScore() + ", manuell prüfen: " + r.getRequiresManualReview()
                + ") - " + r.getReason();
    }
}
