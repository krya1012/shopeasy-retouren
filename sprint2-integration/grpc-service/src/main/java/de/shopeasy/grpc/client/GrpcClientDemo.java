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
 * Command line client for the ArticleConditionService. Without arguments it plays all scenarios (demo):
 * acceptance, rejection, borderline case, batch, error cases. With arguments it makes exactly one call:
 * <pre>
 * check RET-2026-00457 SH-BLZ-042-M AS_NEW [tags=false] [packaging=false]   check a condition
 *                                                          (tag and original packaging are the default)
 * get RET-2026-00457 SH-BLZ-042-M                          fetch an earlier result
 * </pre>
 * Start: {@code ./gradlew :grpc-service:runClient --args="get RET-A SKU"} (the server must be running).
 * The address is localhost:9090 by default; change it with the environment variable GRPC_TARGET
 * (for example "myhost:9090").
 *
 * <p>gRPC note: a {@link ManagedChannel} is the connection to the server. {@code usePlaintext()} means no
 * encryption, which is fine for a local demo. The channel is closed at the end.
 */
public class GrpcClientDemo {

    private static final String USAGE = """
            Usage:
              check <returnId> <sku> <CONDITION> [tags=true|false] [packaging=true|false]
                CONDITION: AS_NEW, MINOR_WEAR, VISIBLY_WORN, DAMAGED, MISSING_TAGS, MISSING_PARTS
                           (UNSPECIFIED is invalid and shows the error INVALID_ARGUMENT)
              get <returnId> <sku>
              (without arguments: demo with all scenarios)""";

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
    /** Makes the return ids unique per run, so a second run does not fail because of duplicates. */
    private final String run = String.valueOf(System.currentTimeMillis() / 1000 % 100000);

    GrpcClientDemo(ConditionClient client) {
        this.client = client;
    }

    /** @return false if the arguments do not fit */
    private boolean command(String[] args) {
        switch (args[0]) {
            case "check" -> {
                if (args.length < 4 || args.length > 6) {
                    return false;
                }
                ObservedCondition condition;
                try {
                    condition = ObservedCondition.valueOf(
                            args[3].equals("UNSPECIFIED") ? "OBSERVED_CONDITION_UNSPECIFIED" : args[3]);
                } catch (IllegalArgumentException e) {
                    return false;
                }
                boolean tags = true;
                boolean packaging = true;
                for (int i = 4; i < args.length; i++) {
                    switch (args[i]) {
                        case "tags=true", "packaging=true" -> { }
                        case "tags=false" -> tags = false;
                        case "packaging=false" -> packaging = false;
                        default -> {
                            return false;
                        }
                    }
                }
                boolean tagsPresent = tags;
                boolean packagingPresent = packaging;
                show("Check " + args[1] + " / " + args[2],
                        () -> client.checkCondition(request(args[1], args[2], condition, tagsPresent, packagingPresent)));
            }
            case "get" -> {
                if (args.length != 3) {
                    return false;
                }
                show("Result " + args[1] + " / " + args[2], () -> client.getCheckResult(args[1], args[2]));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void run() {
        // Scenarios with a decision
        show("1. Acceptance: as new, tag present",
                () -> client.checkCondition(request("RET-A-" + run, "SH-BLZ-042-M", ObservedCondition.AS_NEW, true)));
        show("2. Rejection: damaged",
                () -> client.checkCondition(request("RET-B-" + run, "SH-JKT-088-L", ObservedCondition.DAMAGED, true)));
        show("3. Borderline case: minor signs of wear",
                () -> client.checkCondition(request("RET-C-" + run, "SH-DRS-017-S", ObservedCondition.MINOR_WEAR, true)));
        show("4. Borderline case: as new, but tag is missing",
                () -> client.checkCondition(request("RET-D-" + run, "SH-PNT-031-32", ObservedCondition.AS_NEW, false)));

        // Batch and fetching a result
        show("5. Batch with two articles", () -> client.checkConditionBatch(List.of(
                request("RET-E-" + run, "SH-SNK-120-42", ObservedCondition.AS_NEW, true),
                request("RET-E-" + run, "SH-SHT-009-M", ObservedCondition.VISIBLY_WORN, true))));
        show("6. Fetch an earlier result", () -> client.getCheckResult("RET-A-" + run, "SH-BLZ-042-M"));

        // Error cases
        show("7. Error: the same check again (ALREADY_EXISTS)",
                () -> client.checkCondition(request("RET-A-" + run, "SH-BLZ-042-M", ObservedCondition.AS_NEW, true)));
        show("8. Error: condition is missing (INVALID_ARGUMENT)",
                () -> client.checkCondition(request("RET-F-" + run, "SH-BLT-003-U",
                        ObservedCondition.OBSERVED_CONDITION_UNSPECIFIED, true)));
        show("9. Error: batch with a wrong second item (INVALID_ARGUMENT, nothing is stored)",
                () -> client.checkConditionBatch(List.of(
                        request("RET-G-" + run, "SH-SNK-120-42", ObservedCondition.AS_NEW, true),
                        request("", "SH-SHT-009-M", ObservedCondition.AS_NEW, true))));
        show("10. Error: the result does not exist (NOT_FOUND)",
                () -> client.getCheckResult("RET-DOES-NOT-EXIST", "SH-X"));
        show("11. Outage: service briefly unavailable (UNAVAILABLE), the client retries",
                () -> client.checkCondition(request("RET-FAIL-" + run, "SH-BLZ-042-M", ObservedCondition.AS_NEW, true)));
        show("12. Deadline: service answers too slowly (DEADLINE_EXCEEDED), the client does not retry",
                () -> client.checkCondition(request("RET-SLOW-" + run, "SH-BLZ-042-M", ObservedCondition.AS_NEW, true)));
    }

    private static ConditionCheckRequest request(String returnId, String sku, ObservedCondition condition,
                                                 boolean tagsPresent) {
        return request(returnId, sku, condition, tagsPresent, true);
    }

    /** Builds a request. The generated classes use the "builder" pattern: set the fields, then call build(). */
    private static ConditionCheckRequest request(String returnId, String sku, ObservedCondition condition,
                                                 boolean tagsPresent, boolean packaging) {
        return ConditionCheckRequest.newBuilder()
                .setReturnId(returnId)
                .setSku(sku)
                .setObservedCondition(condition)
                .setTagsPresent(tagsPresent)
                .setOriginalPackaging(packaging)
                .setInspectorNote("Demo")
                .setInspectedAt(Instant.now().toString())
                .build();
    }

    /** Runs a call and prints the result or the error. */
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
            System.out.println("-> ERROR [" + e.getCode() + "] " + e.getMessage());
        }
    }

    private static String format(ConditionCheckResponse r) {
        return r.getReturnId() + " / " + r.getSku() + ": " + r.getDecision()
                + " (score " + r.getConfidenceScore() + ", manual review: " + r.getRequiresManualReview()
                + ") - " + r.getReason();
    }
}
