package de.shopeasy.grpc.server;

import java.io.IOException;
import java.time.Clock;
import java.util.logging.Logger;

import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * Starts the gRPC service. The port is the first argument, otherwise 9090.
 * Start: {@code ./gradlew :grpc-service:run}.
 *
 * <p>This module does not use Spring Boot. gRPC has its own small server (grpc-java); we create it with a
 * builder, register our service on it, and start it. The server code needs no framework.
 */
public class ConditionServer {

    private static final Logger LOG = Logger.getLogger(ConditionServer.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        Server server = ServerBuilder.forPort(port)
                .addService(new ArticleConditionServiceImpl(Clock.systemUTC()))
                .build()
                .start();
        LOG.info("ArticleConditionService is running on port " + port);

        // The shutdown hook runs when the program is stopped (Ctrl+C), so the server ends cleanly.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Server is shutting down");
            server.shutdown();
        }));
        server.awaitTermination(); // keep the program alive until the server ends
    }
}
