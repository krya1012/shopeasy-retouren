<?php

namespace App\Messaging;

use App\Console\Commands\ConsumeStatusEvents;
use App\Models\ProcessedEvent;
use App\Models\StatusEvent;
use App\Services\ReturnsApiClient;
use App\Services\ReturnsApiException;
use Carbon\Carbon;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Sleep;
use stdClass;
use Throwable;

/**
 * The logic of the consumer: one message in, one {@see Outcome} out. It does not know RabbitMQ (the command
 * {@see ConsumeStatusEvents} does), so tests can call it directly.
 *
 * Steps for every message:
 *  1. Parse and validate it against the JSON Schema. Broken messages are rejected (dead letter queue).
 *  2. Duplicate check by eventId, BEFORE the call to the REST mock. A late duplicate of an old event would
 *     otherwise get "409 forbidden status change" from the mock and end up in the dead letter queue by mistake.
 *  3. PATCH the REST mock with expectedStatus = oldStatus. A 404 or 409 is a final answer: reject. A connection
 *     error or a 5xx is retried a few times with a growing pause, then the message is rejected as well. Rejecting
 *     (instead of requeueing) avoids an endless loop, because the queue has no delivery limit.
 *  4. Remember the eventId (processed_events) only after the mock accepted the change. If the program crashes
 *     in between, the broker delivers the message again; the repeated PATCH is harmless because the mock answers
 *     200 when the status is already set.
 *  5. Write the result to the event log (status_events), whatever it was.
 */
class StatusEventHandler
{
    /** Pauses before retry 1, 2 and 3 (so up to four attempts in total). */
    public const RETRY_DELAYS_MS = [200, 400, 800];

    public function __construct(
        private readonly ReturnsApiClient $api,
        private readonly EventValidator $validator,
    ) {}

    public function handle(string $body): Outcome
    {
        $data = json_decode($body);

        if (! $data instanceof stdClass) {
            return $this->reject(StatusEvent::INVALID, null, 'Message is not a JSON object');
        }

        $problem = $this->validator->validate($data);
        if ($problem !== null) {
            return $this->reject(StatusEvent::INVALID, $data, "Schema violation: {$problem}");
        }

        $event = StatusEventData::fromValidated($data);

        if (ProcessedEvent::where('event_id', $event->eventId)->exists()) {
            $this->log(StatusEvent::DUPLICATE, $data, 'Event was already processed');
            Log::info("Duplicate event {$event->eventId} for {$event->returnId} ignored");

            return Outcome::Duplicate;
        }

        try {
            $this->applyToRestMock($event);
        } catch (ReturnsApiException $e) {
            return $this->reject(StatusEvent::REJECTED, $data, $e->getMessage());
        }

        try {
            ProcessedEvent::create(['event_id' => $event->eventId, 'return_id' => $event->returnId]);
        } catch (UniqueConstraintViolationException) {
            // A second consumer was faster. The change is applied either way.
        }
        $this->log(StatusEvent::PROCESSED, $data);
        Log::info("Event {$event->eventId}: {$event->returnId} {$event->oldStatus} -> {$event->newStatus} applied");

        return Outcome::Processed;
    }

    /** @throws ReturnsApiException when the mock refuses the change or stays unavailable */
    private function applyToRestMock(StatusEventData $event): void
    {
        for ($attempt = 0; ; $attempt++) {
            try {
                $this->api->updateStatus($event->returnId, $event->newStatus, $event->oldStatus);

                return;
            } catch (ReturnsApiException $e) {
                $delay = self::RETRY_DELAYS_MS[$attempt] ?? null;
                if (! $e->isRetryable() || $delay === null) {
                    throw $e;
                }
                Log::warning("REST mock failed for {$event->returnId} (attempt ".($attempt + 1)."), retry in {$delay} ms: {$e->getMessage()}");
                Sleep::for($delay)->milliseconds();
            }
        }
    }

    private function reject(string $kind, ?stdClass $data, string $reason): Outcome
    {
        $this->log($kind, $data, $reason);
        Log::warning("Message rejected ({$kind}): {$reason}");

        return Outcome::Rejected;
    }

    /** Writes one row of the event log. For broken messages, only the fields that are readable are stored. */
    private function log(string $outcome, ?stdClass $data, ?string $reason = null): void
    {
        StatusEvent::create([
            'event_id' => $this->text($data, 'eventId'),
            'return_id' => $this->text($data, 'returnId'),
            'old_status' => $this->text($data, 'oldStatus'),
            'new_status' => $this->text($data, 'newStatus'),
            'occurred_at' => $this->time($this->text($data, 'occurredAt')),
            'outcome' => $outcome,
            'reason' => $reason,
        ]);
    }

    private function text(?stdClass $data, string $field): ?string
    {
        $value = $data->{$field} ?? null;

        return is_string($value) ? mb_substr($value, 0, 255) : null;
    }

    private function time(?string $value): ?Carbon
    {
        if ($value === null) {
            return null;
        }
        try {
            return Carbon::parse($value);
        } catch (Throwable) {
            return null;
        }
    }
}
