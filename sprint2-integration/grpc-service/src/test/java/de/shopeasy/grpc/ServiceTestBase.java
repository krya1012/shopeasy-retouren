package de.shopeasy.grpc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import de.shopeasy.grpc.server.ArticleConditionServiceImpl;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/** Startet für jeden Test einen frischen Service im selben Prozess (ohne Netzwerk). */
abstract class ServiceTestBase {

    protected ArticleConditionServiceGrpc.ArticleConditionServiceBlockingStub stub;
    protected ManagedChannel channel;
    private Server server;

    @BeforeEach
    void startServer() throws Exception {
        String name = InProcessServerBuilder.generateName();
        Clock fixed = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);
        server = InProcessServerBuilder.forName(name)
                .addService(new ArticleConditionServiceImpl(fixed, 500)).build().start();
        channel = InProcessChannelBuilder.forName(name).build();
        stub = ArticleConditionServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void stopServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }
}
