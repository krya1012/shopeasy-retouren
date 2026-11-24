package de.shopeasy.workflow.poller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.shopeasy.workflow.returns.ReturnReport;
import de.shopeasy.workflow.returns.ReturnsApi;
import de.shopeasy.workflow.returns.ReturnsApiException;
import io.camunda.client.CamundaClient;

/**
 * Fetches new marketplace returns from the REST mock and starts a process instance for each (start event
 * {@code s_mkt}). The task says the mock is polled; the poller replaces the incoming message of the Sprint 1 model.
 *
 * <p><b>No second instance for the same return:</b> the start message carries the return ID as correlation key.
 * Zeebe starts a new instance for a start message only if no instance with the same key is still running. A return
 * stays in status NEW until the process has moved it on, so the poller sees it again at the next poll; the message is
 * then ignored (time to live 0: it is not kept for later). When the process has moved the return on (rejected, approved,
 * ...), it is no longer in the list.
 *
 * <p>If the REST mock or the cluster is not reachable, the poller writes a warning and tries again at the next poll.
 */
@Component
@ConditionalOnProperty(name = "workflow.poller.enabled", havingValue = "true", matchIfMissing = true)
public class MarketplacePoller {

    static final String MESSAGE = "marketplace-return-received";
    private static final Logger LOG = LoggerFactory.getLogger(MarketplacePoller.class);

    private final ReturnsApi api;
    private final CamundaClient camunda;

    public MarketplacePoller(ReturnsApi api, CamundaClient camunda) {
        this.api = api;
        this.camunda = camunda;
    }

    @Scheduled(fixedDelayString = "${workflow.poller.interval:PT10S}")
    public void poll() {
        try {
            for (ReturnReport report : api.listNewMarketplaceReturns()) {
                camunda.newPublishMessageCommand().messageName(MESSAGE).correlationKey(report.returnId())
                        .variables(Map.of("returnId", report.returnId())).send().join();
                LOG.debug("Start message sent for {}", report.returnId());
            }
        } catch (ReturnsApiException e) {
            LOG.warn("Poll skipped: {}", e.getMessage());
        } catch (RuntimeException e) {
            LOG.warn("Poll failed (is the Camunda cluster running?): {}", e.getMessage());
        }
    }
}
