<?php

use App\Messaging\Outcome;
use App\Messaging\StatusEventHandler;
use App\Models\ProcessedEvent;
use App\Models\StatusEvent;
use Illuminate\Http\Client\Request;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Sleep;
use Illuminate\Support\Str;

/*
| Tests for the consumer logic. Http::fake() replaces the REST mock, Sleep::fake() skips the retry pauses,
| so no broker, no mock and no waiting is needed.
*/

const MOCK_PATCH = 'localhost:8080/api/v1/returns/*';

function statusEvent(array $overrides = []): string
{
    return json_encode(array_merge([
        'eventId' => (string) Str::uuid(),
        'eventType' => 'ReturnStatusChanged',
        'returnId' => 'RET-2026-00457',
        'oldStatus' => 'NEW',
        'newStatus' => 'APPROVED',
        'occurredAt' => '2026-10-22T11:47:25.123Z',
        'source' => 'ShopEasy-Returns-Management',
    ], $overrides));
}

function consumer(): StatusEventHandler
{
    return app(StatusEventHandler::class);
}

beforeEach(fn () => Sleep::fake());

it('applies a valid event to the REST mock with expectedStatus and logs it', function () {
    Http::fake([MOCK_PATCH => Http::response(['returnId' => 'RET-2026-00457', 'status' => 'APPROVED'])]);
    $body = statusEvent();

    expect(consumer()->handle($body))->toBe(Outcome::Processed);

    Http::assertSent(fn (Request $r) => $r->method() === 'PATCH'
        && str_ends_with($r->url(), '/api/v1/returns/RET-2026-00457')
        && $r->data() === ['status' => 'APPROVED', 'expectedStatus' => 'NEW']);
    expect(ProcessedEvent::count())->toBe(1);
    expect(StatusEvent::sole())
        ->outcome->toBe('processed')
        ->return_id->toBe('RET-2026-00457')
        ->new_status->toBe('APPROVED');
});

it('skips a duplicate eventId without calling the REST mock again', function () {
    Http::fake([MOCK_PATCH => Http::response(['status' => 'APPROVED'])]);
    $body = statusEvent();

    expect(consumer()->handle($body))->toBe(Outcome::Processed);
    expect(consumer()->handle($body))->toBe(Outcome::Duplicate);

    Http::assertSentCount(1);
    expect(ProcessedEvent::count())->toBe(1);
    expect(StatusEvent::where('outcome', 'duplicate')->count())->toBe(1);
});

it('recognises a late duplicate even if the return has moved on since', function () {
    // The mock would now answer 409 for the old event. The duplicate check comes first, so it is not even asked.
    Http::fake([MOCK_PATCH => Http::response(['status' => 'APPROVED'])]);
    $approved = statusEvent();
    consumer()->handle($approved);

    Http::fake(); // starts a fresh request record; the stub from above is irrelevant because nothing must be sent

    expect(consumer()->handle($approved))->toBe(Outcome::Duplicate);
    Http::assertNothingSent();
});

it('rejects a message that is not JSON', function () {
    Http::fake();

    expect(consumer()->handle('this is not JSON'))->toBe(Outcome::Rejected);

    Http::assertNothingSent();
    expect(StatusEvent::sole())->outcome->toBe('invalid')->event_id->toBeNull();
});

it('rejects JSON that is not an object', function () {
    expect(consumer()->handle('[1, 2, 3]'))->toBe(Outcome::Rejected);
    expect(StatusEvent::sole()->outcome)->toBe('invalid');
});

it('rejects an event that violates the schema and keeps the readable fields in the log', function (array $broken) {
    Http::fake();

    expect(consumer()->handle(json_encode($broken)))->toBe(Outcome::Rejected);

    Http::assertNothingSent();
    expect(ProcessedEvent::count())->toBe(0);
    expect(StatusEvent::sole())->outcome->toBe('invalid')->reason->toContain('Schema violation');
})->with([
    'returnId missing' => [['eventId' => '3d70997d-0378-46d7-ae36-c9d8672865dc', 'oldStatus' => 'NEW', 'newStatus' => 'APPROVED', 'occurredAt' => '2026-10-22T11:47:25Z']],
    'unknown status' => [['eventId' => '3d70997d-0378-46d7-ae36-c9d8672865dc', 'returnId' => 'RET-1', 'oldStatus' => 'NEW', 'newStatus' => 'NONSENSE', 'occurredAt' => '2026-10-22T11:47:25Z']],
    'eventId is no uuid' => [['eventId' => 'abc', 'returnId' => 'RET-1', 'oldStatus' => 'NEW', 'newStatus' => 'APPROVED', 'occurredAt' => '2026-10-22T11:47:25Z']],
    'occurredAt is no date' => [['eventId' => '3d70997d-0378-46d7-ae36-c9d8672865dc', 'returnId' => 'RET-1', 'oldStatus' => 'NEW', 'newStatus' => 'APPROVED', 'occurredAt' => 'yesterday']],
]);

it('accepts unknown extra fields (tolerant reader)', function () {
    Http::fake([MOCK_PATCH => Http::response(['status' => 'APPROVED'])]);

    expect(consumer()->handle(statusEvent(['priority' => 'high'])))->toBe(Outcome::Processed);
});

it('rejects the event when the return is unknown (404) and forgets nothing it should not', function () {
    Http::fake([MOCK_PATCH => Http::response(['detail' => 'Return RET-2026-99999 not found.'], 404)]);

    expect(consumer()->handle(statusEvent(['returnId' => 'RET-2026-99999'])))->toBe(Outcome::Rejected);

    Http::assertSentCount(1); // 404 is final, no retry
    expect(ProcessedEvent::count())->toBe(0);
    expect(StatusEvent::sole())->outcome->toBe('rejected')->reason->toContain('404');
});

it('rejects the event when the mock refuses the status change (409) and does not retry', function () {
    Http::fake([MOCK_PATCH => Http::response(['detail' => 'Status change not allowed: NEW -> INSPECTED'], 409)]);

    expect(consumer()->handle(statusEvent(['newStatus' => 'INSPECTED'])))->toBe(Outcome::Rejected);

    Http::assertSentCount(1);
    expect(ProcessedEvent::count())->toBe(0);
    expect(StatusEvent::sole()->reason)->toContain('409')->toContain('NEW -> INSPECTED');
});

it('retries a 5xx answer and succeeds when the mock recovers', function () {
    Http::fake([MOCK_PATCH => Http::sequence()
        ->push(['detail' => 'boom'], 503)
        ->push(['detail' => 'boom'], 500)
        ->push(['status' => 'APPROVED'], 200),
    ]);

    expect(consumer()->handle(statusEvent()))->toBe(Outcome::Processed);

    Http::assertSentCount(3);
    Sleep::assertSequence([Sleep::for(200)->milliseconds(), Sleep::for(400)->milliseconds()]);
    expect(ProcessedEvent::count())->toBe(1);
});

it('rejects the event after four failed attempts and remembers nothing', function () {
    Http::fake([MOCK_PATCH => Http::response(['detail' => 'down'], 503)]);

    expect(consumer()->handle(statusEvent()))->toBe(Outcome::Rejected);

    Http::assertSentCount(4); // first try + 3 retries
    Sleep::assertSequence([
        Sleep::for(200)->milliseconds(),
        Sleep::for(400)->milliseconds(),
        Sleep::for(800)->milliseconds(),
    ]);
    expect(ProcessedEvent::count())->toBe(0);
    expect(StatusEvent::sole())->outcome->toBe('rejected')->reason->toContain('unavailable');
});

it('retries connection errors like 5xx answers', function () {
    Http::fake([MOCK_PATCH => Http::sequence()
        ->pushFailedConnection('connection refused')
        ->push(['status' => 'APPROVED']),
    ]);

    expect(consumer()->handle(statusEvent()))->toBe(Outcome::Processed);
    Http::assertSentCount(2);
});

it('can process the same event again after a rejected attempt (it was not remembered)', function () {
    $body = statusEvent();
    // Four failed attempts (first try + 3 retries) reject the message, the next delivery then succeeds.
    // A sequence is used because a second Http::fake() would not replace the first stub (the first match wins).
    Http::fake([MOCK_PATCH => Http::sequence()
        ->push(['detail' => 'down'], 503)
        ->push(['detail' => 'down'], 503)
        ->push(['detail' => 'down'], 503)
        ->push(['detail' => 'down'], 503)
        ->push(['status' => 'APPROVED']),
    ]);

    expect(consumer()->handle($body))->toBe(Outcome::Rejected);
    expect(consumer()->handle($body))->toBe(Outcome::Processed);
});
