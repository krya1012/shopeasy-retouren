package de.shopeasy.workflow.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import de.shopeasy.workflow.status.EventNotDeliveredException;
import de.shopeasy.workflow.status.StatusEvent;
import de.shopeasy.workflow.status.StatusEventSink;

/** Collects the events instead of sending them; can be switched to fail like an unreachable broker. */
public class RecordingEventSink implements StatusEventSink {

    public final List<StatusEvent> events = new CopyOnWriteArrayList<>();
    private final AtomicBoolean down = new AtomicBoolean();
    /** Runs after an event was taken (to simulate a failure right after the broker confirmed). */
    public volatile Runnable afterPublish;

    public void reset() {
        events.clear();
        down.set(false);
        afterPublish = null;
    }

    public void setDown(boolean down) {
        this.down.set(down);
    }

    @Override
    public void publish(StatusEvent event) {
        if (down.get()) {
            throw new EventNotDeliveredException("Broker not reachable (simulated outage)");
        }
        events.add(event);
        if (afterPublish != null) {
            afterPublish.run();
        }
    }

    /** The events as {@code "RET-1 NEW->APPROVED"}. */
    public List<String> summary() {
        return events.stream().map(e -> e.returnId() + " " + e.oldStatus() + "->" + e.newStatus()).toList();
    }
}
