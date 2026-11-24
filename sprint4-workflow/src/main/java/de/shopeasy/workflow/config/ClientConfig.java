package de.shopeasy.workflow.config;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import de.shopeasy.grpc.client.ConditionClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Creates the connections to the three Sprint 2 services.
 *
 * <p>{@code @Configuration} + {@code @Bean}: Spring calls every {@code @Bean} method once and keeps the result as a
 * "bean", which other classes get through their constructor (dependency injection). Tests replace a bean by
 * defining another one of the same type.
 */
@Configuration
public class ClientConfig {

    /** The exchange of the status events; same name and type as in the Sprint 2 modules (durable topic exchange). */
    public static final String EXCHANGE = "returns.status";

    /** HTTP client for the REST mock with short timeouts, so an unreachable service fails fast and the job is retried. */
    @Bean
    RestClient restMockClient(WorkflowProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl(properties.restMockUrl()).requestFactory(factory).build();
    }

    /** The gRPC connection. It connects on the first call, so the application starts even if the service is down. */
    @Bean(destroyMethod = "shutdownNow")
    ManagedChannel conditionChannel(WorkflowProperties properties) {
        return ManagedChannelBuilder.forTarget(properties.grpcTarget()).usePlaintext().build();
    }

    /** The Sprint 2 client: deadline of 3 seconds per call, three attempts on UNAVAILABLE. */
    @Bean
    ConditionClient conditionClient(ManagedChannel conditionChannel) {
        return new ConditionClient(conditionChannel);
    }

    /** Declares the exchange at the broker (the queues are declared by the receivers, as in Sprint 2). */
    @Bean
    TopicExchange returnsStatusExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /** JSON converter for the events (Jackson 3, as in the Sprint 2 producer). */
    @Bean
    @ConditionalOnMissingBean
    JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemUTC();
    }
}
