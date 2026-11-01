<?php

namespace App\Services;

use Illuminate\Http\Client\ConnectionException;
use Illuminate\Http\Client\PendingRequest;
use Illuminate\Http\Client\Response;
use Illuminate\Support\Facades\Http;

/**
 * Client for the Java REST mock (sprint2-integration/rest-mock). Every other class in the portal talks to the
 * mock only through this one, so the URL, the timeout and the error mapping are in one place.
 *
 * Laravel basics: the {@see Http} facade is a wrapper around Guzzle. In tests, {@code Http::fake()} replaces
 * the real network call, so no mock server has to run.
 */
class ReturnsApiClient
{
    /**
     * @param  array{status?: string, marketplace?: string}  $filters
     * @return list<array<string, mixed>>
     *
     * @throws ReturnsApiException
     */
    public function list(array $filters = []): array
    {
        $response = $this->send(fn () => $this->http()->get('/api/v1/returns', array_filter($filters)));

        return $response->json('data') ?? [];
    }

    /**
     * @return array<string, mixed>
     *
     * @throws ReturnsApiException
     */
    public function get(string $returnId): array
    {
        return $this->send(fn () => $this->http()->get('/api/v1/returns/'.rawurlencode($returnId)), "Return {$returnId}")
            ->json();
    }

    /**
     * @return array<string, mixed>
     *
     * @throws ReturnsApiException
     */
    public function order(string $orderId): array
    {
        return $this->send(fn () => $this->http()->get('/api/v1/orders/'.rawurlencode($orderId)), "Order {$orderId}")
            ->json();
    }

    /**
     * Changes the status of a return.
     *
     * @param  string|null  $expectedStatus  if given, the mock answers 409 when the return has another status
     *                                       (the caller's information is old)
     * @return array<string, mixed> the updated return
     *
     * @throws ReturnsApiException
     */
    public function updateStatus(string $returnId, string $status, ?string $expectedStatus = null): array
    {
        $body = array_filter(['status' => $status, 'expectedStatus' => $expectedStatus], fn ($v) => $v !== null);

        return $this->send(
            fn () => $this->http()->patch('/api/v1/returns/'.rawurlencode($returnId), $body),
            "Return {$returnId}",
        )->json();
    }

    private function http(): PendingRequest
    {
        return Http::baseUrl(config('services.rest_mock.url'))
            ->timeout(config('services.rest_mock.timeout'))
            ->acceptJson()
            ->asJson();
    }

    /**
     * Runs the request and turns every non-2xx answer into a {@see ReturnsApiException}.
     *
     * @param  callable(): Response  $request
     */
    private function send(callable $request, string $what = 'Resource'): Response
    {
        try {
            $response = $request();
        } catch (ConnectionException $e) {
            throw ReturnsApiException::unavailable($e->getMessage());
        }

        if ($response->successful()) {
            return $response;
        }

        // The mock sends RFC 9457 problem details: {"title": ..., "detail": ..., "status": ...}
        $detail = $response->json('detail') ?? $response->reason();

        throw match (true) {
            $response->status() === 404 => ReturnsApiException::notFound($what),
            $response->status() === 409 => ReturnsApiException::conflict($detail),
            $response->serverError() => ReturnsApiException::unavailable($detail, $response->status()),
            default => ReturnsApiException::badRequest($response->status(), $detail),
        };
    }
}
