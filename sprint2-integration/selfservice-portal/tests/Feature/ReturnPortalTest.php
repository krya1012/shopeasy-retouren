<?php

use App\Models\StatusEvent;
use App\Models\User;
use Illuminate\Http\Client\Request;
use Illuminate\Support\Facades\Http;
use Inertia\Testing\AssertableInertia as Assert;

/*
| Tests for the self-service pages. Http::fake() stands in for the Java REST mock.
| The customer in these tests owns RET-1; RET-2 belongs to somebody else.
*/

function returnData(string $id, string $email, string $status = 'NEW'): array
{
    return [
        'returnId' => $id,
        'orderId' => 'ORD-'.$id,
        'marketplace' => 'ZALANDO',
        'status' => $status,
        'reason' => 'SIZE_TOO_SMALL',
        'requestedAt' => '2026-09-18T09:14:00Z',
        'items' => [['sku' => 'SH-1', 'productName' => 'Blazer', 'quantity' => 2, 'unitPrice' => 89.9, 'currency' => 'EUR']],
        'customer' => ['customerId' => 'C-1', 'email' => $email],
    ];
}

function fakeMock(array $extra = []): void
{
    Http::fake($extra + [
        'localhost:8080/api/v1/returns/RET-1' => Http::response(returnData('RET-1', 'j.mueller@example.com')),
        'localhost:8080/api/v1/returns/RET-2' => Http::response(returnData('RET-2', 'other@example.com')),
        'localhost:8080/api/v1/returns*' => Http::response(['data' => [
            returnData('RET-1', 'j.mueller@example.com'),
            returnData('RET-2', 'other@example.com'),
        ]]),
        'localhost:8080/api/v1/orders/*' => Http::response(['orderId' => 'ORD-RET-1', 'totalAmount' => 179.8, 'currency' => 'EUR']),
    ]);
}

beforeEach(function () {
    $this->customer = User::factory()->create(['email' => 'J.Mueller@example.com']); // case must not matter
    $this->clerk = User::factory()->clerk()->create();
});

it('sends guests to the login page', function () {
    $this->get('/returns')->assertRedirect(route('login'));
    $this->get('/returns/RET-1')->assertRedirect(route('login'));
    $this->get('/events')->assertRedirect(route('login'));
});

it('shows a customer only their own returns', function () {
    fakeMock();

    $this->actingAs($this->customer)->get('/returns')
        ->assertOk()
        ->assertInertia(fn (Assert $page) => $page
            ->component('Returns/Index')
            ->has('returns', 1)
            ->where('returns.0.returnId', 'RET-1')
            ->where('returns.0.itemCount', 2));
});

it('shows a clerk all returns', function () {
    fakeMock();

    $this->actingAs($this->clerk)->get('/returns')
        ->assertInertia(fn (Assert $page) => $page->has('returns', 2));
});

it('passes the filters to the REST mock', function () {
    fakeMock();

    $this->actingAs($this->clerk)->get('/returns?status=APPROVED&marketplace=AMAZON')->assertOk();

    Http::assertSent(fn (Request $r) => str_contains($r->url(), 'status=APPROVED') && str_contains($r->url(), 'marketplace=AMAZON'));
});

it('rejects an unknown filter value', function () {
    fakeMock();

    $this->actingAs($this->clerk)->get('/returns?status=NONSENSE')->assertSessionHasErrors('status');
});

it('still renders the list when the REST mock is down', function () {
    Http::fake(['*' => Http::response('down', 503)]);

    $this->actingAs($this->clerk)->get('/returns')
        ->assertOk()
        ->assertInertia(fn (Assert $page) => $page
            ->has('returns', 0)
            ->where('apiError', fn ($m) => str_contains($m, 'nicht erreichbar')));
});

it('lets a customer open their own return without status buttons', function () {
    fakeMock();
    StatusEvent::create(['return_id' => 'RET-1', 'new_status' => 'APPROVED', 'outcome' => 'processed']);

    $this->actingAs($this->customer)->get('/returns/RET-1')
        ->assertOk()
        ->assertInertia(fn (Assert $page) => $page
            ->component('Returns/Show')
            ->where('return.returnId', 'RET-1')
            ->where('canChangeStatus', false)
            ->where('nextStatuses', [])
            ->where('order.totalAmount', 179.8)
            ->has('events', 1));
});

it('hides the return of another customer as 404', function () {
    fakeMock();

    $this->actingAs($this->customer)->get('/returns/RET-2')->assertNotFound();
});

it('offers a clerk only the allowed next statuses', function () {
    fakeMock();

    $this->actingAs($this->clerk)->get('/returns/RET-1')
        ->assertInertia(fn (Assert $page) => $page
            ->where('canChangeStatus', true)
            ->where('nextStatuses', ['APPROVED', 'REJECTED', 'IN_REVIEW']));
});

it('forbids a customer to change a status and does not call the REST mock', function () {
    fakeMock();

    $this->actingAs($this->customer)
        ->post('/returns/RET-1/status', ['status' => 'APPROVED', 'currentStatus' => 'NEW'])
        ->assertForbidden();

    Http::assertNothingSent();
    expect(StatusEvent::count())->toBe(0);
});

it('lets a clerk change a status, sends expectedStatus and logs the change', function () {
    fakeMock(['localhost:8080/api/v1/returns/RET-1' => Http::response(returnData('RET-1', 'j.mueller@example.com', 'APPROVED'))]);

    $this->actingAs($this->clerk)
        ->post('/returns/RET-1/status', ['status' => 'APPROVED', 'currentStatus' => 'NEW'])
        ->assertRedirect();

    Http::assertSent(fn (Request $r) => $r->method() === 'PATCH'
        && $r->data() === ['status' => 'APPROVED', 'expectedStatus' => 'NEW']);
    expect(StatusEvent::sole())
        ->outcome->toBe('manual')
        ->old_status->toBe('NEW')
        ->new_status->toBe('APPROVED');
});

it('does not log anything when the REST mock refuses the change (409)', function () {
    fakeMock(['localhost:8080/api/v1/returns/RET-1' => Http::response(['detail' => 'Status is APPROVED, not the expected NEW'], 409)]);

    $this->actingAs($this->clerk)
        ->post('/returns/RET-1/status', ['status' => 'REJECTED', 'currentStatus' => 'NEW'])
        ->assertRedirect();

    expect(StatusEvent::count())->toBe(0);
});

it('validates the status of a change request', function (array $input) {
    fakeMock();

    $this->actingAs($this->clerk)->post('/returns/RET-1/status', $input)->assertSessionHasErrors();

    Http::assertNothingSent();
})->with([
    'missing status' => [['currentStatus' => 'NEW']],
    'unknown status' => [['status' => 'NONSENSE', 'currentStatus' => 'NEW']],
    'missing currentStatus' => [['status' => 'APPROVED']],
]);

it('shows the event log to clerks only', function () {
    StatusEvent::create(['return_id' => 'RET-1', 'new_status' => 'APPROVED', 'outcome' => 'processed']);
    StatusEvent::create(['outcome' => 'invalid', 'reason' => 'Message is not a JSON object']);

    $this->actingAs($this->customer)->get('/events')->assertForbidden();

    $this->actingAs($this->clerk)->get('/events')
        ->assertOk()
        ->assertInertia(fn (Assert $page) => $page
            ->component('Events/Index')
            ->has('events', 2)
            ->where('counts.processed', 1)
            ->where('counts.invalid', 1));
});
