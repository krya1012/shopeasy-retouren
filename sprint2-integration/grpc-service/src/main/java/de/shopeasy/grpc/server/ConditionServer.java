package de.shopeasy.grpc.server;

import java.io.IOException;
import java.time.Clock;
import java.util.logging.Logger;

import io.grpc.Server;
import io.grpc.ServerBuilder;

/** Startet den gRPC-Service. Port als Argument, sonst 9090. Start: {@code ./gradlew :grpc-service:run}. */
public class ConditionServer {

    private static final Logger LOG = Logger.getLogger(ConditionServer.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
        Server server = ServerBuilder.forPort(port)
                .addService(new ArticleConditionServiceImpl(Clock.systemUTC()))
                .build()
                .start();
        LOG.info("ArticleConditionService läuft auf Port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Server wird beendet");
            server.shutdown();
        }));
        server.awaitTermination();
    }
}
