package de.shopeasy.workflow.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the worker application ({@code workflow.*} in {@code application.yaml}). A record with
 * {@code @ConfigurationProperties} is filled by Spring from the file, from command line arguments
 * ({@code --workflow.rest-mock-url=...}) or from environment variables; {@code @DefaultValue} is used if nothing is set.
 *
 * @param restMockUrl  base URL of the REST mock (Sprint 2)
 * @param grpcTarget   host and port of the gRPC condition service (Sprint 2)
 * @param retryBackoff pause before a failed job is tried again
 * @param poller       settings of the marketplace poller
 */
@ConfigurationProperties(prefix = "workflow")
public record WorkflowProperties(
        @DefaultValue("http://localhost:8081") String restMockUrl,
        @DefaultValue("localhost:9090") String grpcTarget,
        @DefaultValue("PT5S") Duration retryBackoff,
        @DefaultValue Poller poller) {

    /**
     * @param enabled  false switches the poller off (then only returns started by message are handled)
     * @param interval pause between two polls of the REST mock
     */
    public record Poller(@DefaultValue("true") boolean enabled, @DefaultValue("PT10S") Duration interval) {
    }
}
