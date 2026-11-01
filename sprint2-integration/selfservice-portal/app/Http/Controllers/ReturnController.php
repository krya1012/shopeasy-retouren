<?php

namespace App\Http\Controllers;

use App\Models\StatusEvent;
use App\Policies\ReturnPolicy;
use App\Services\ReturnsApiClient;
use App\Services\ReturnsApiException;
use App\Support\StatusTransitions;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;
use Illuminate\Validation\Rule;
use Inertia\Inertia;
use Inertia\Response;

/**
 * The self-service pages for returns. All data comes from the REST mock through {@see ReturnsApiClient};
 * this controller only decides who sees what (see {@see ReturnPolicy}) and shapes the data for Vue.
 */
class ReturnController extends Controller
{
    public const MARKETPLACES = ['AMAZON', 'ZALANDO'];

    public function __construct(
        private readonly ReturnsApiClient $api,
        private readonly StatusTransitions $transitions,
    ) {}

    public function index(Request $request): Response
    {
        $filters = $request->validate([
            'status' => ['nullable', Rule::in($this->transitions->statuses())],
            'marketplace' => ['nullable', Rule::in(self::MARKETPLACES)],
        ]);
        $filters = array_filter($filters);

        $apiError = null;
        try {
            $returns = $this->api->list($filters);
        } catch (ReturnsApiException $e) {
            $returns = [];
            $apiError = $this->messageFor($e);
        }

        // Customers get only their own returns. The filtering happens here on the server, never in the browser.
        $visible = array_values(array_filter($returns, fn (array $r) => Gate::allows('return.view', [$r])));

        return Inertia::render('Returns/Index', [
            'returns' => array_map($this->summary(...), $visible),
            'filters' => (object) $filters,
            'statuses' => $this->transitions->statuses(),
            'marketplaces' => self::MARKETPLACES,
            'apiError' => $apiError,
        ]);
    }

    public function show(string $returnId): Response
    {
        $return = $this->findOrFail($returnId);

        // Someone else's return looks exactly like a missing one (404, not 403), so ids cannot be probed.
        abort_unless(Gate::allows('return.view', [$return]), 404);

        $canChange = Gate::allows('return.changeStatus');

        return Inertia::render('Returns/Show', [
            'return' => $return,
            'order' => $this->findOrder($return['orderId'] ?? null),
            'events' => StatusEvent::where('return_id', $returnId)->latest('id')->limit(50)->get(),
            'canChangeStatus' => $canChange,
            'nextStatuses' => $canChange ? $this->transitions->allowedFrom($return['status']) : [],
        ]);
    }

    /**
     * A clerk changes the status. The form sends the status it was showing ({@code currentStatus}); the REST mock
     * refuses the change with 409 if the return has been changed in the meantime (for example by a message).
     */
    public function updateStatus(Request $request, string $returnId): RedirectResponse
    {
        Gate::authorize('return.changeStatus');

        $data = $request->validate([
            'status' => ['required', Rule::in($this->transitions->statuses())],
            'currentStatus' => ['required', Rule::in($this->transitions->statuses())],
        ]);

        try {
            $this->api->updateStatus($returnId, $data['status'], $data['currentStatus']);
        } catch (ReturnsApiException $e) {
            Inertia::flash('toast', ['type' => 'error', 'message' => $this->messageFor($e)]);

            return back();
        }

        StatusEvent::create([
            'return_id' => $returnId,
            'old_status' => $data['currentStatus'],
            'new_status' => $data['status'],
            'occurred_at' => now(),
            'outcome' => StatusEvent::MANUAL,
            'reason' => 'Geändert von '.$request->user()->name,
        ]);
        Inertia::flash('toast', ['type' => 'success', 'message' => 'Der Status wurde geändert.']);

        return back();
    }

    /** @return array<string, mixed> */
    private function findOrFail(string $returnId): array
    {
        try {
            return $this->api->get($returnId);
        } catch (ReturnsApiException $e) {
            abort($e->isRetryable() ? 503 : 404, $this->messageFor($e));
        }
    }

    /** @return array<string, mixed>|null */
    private function findOrder(?string $orderId): ?array
    {
        if ($orderId === null) {
            return null;
        }
        try {
            return $this->api->order($orderId);
        } catch (ReturnsApiException) {
            return null; // The page is useful without the order, so a missing order is not an error.
        }
    }

    /**
     * @param  array<string, mixed>  $r
     * @return array<string, mixed>
     */
    private function summary(array $r): array
    {
        return [
            'returnId' => $r['returnId'],
            'orderId' => $r['orderId'],
            'marketplace' => $r['marketplace'],
            'status' => $r['status'],
            'reason' => $r['reason'] ?? null,
            'requestedAt' => $r['requestedAt'] ?? null,
            'itemCount' => array_sum(array_column($r['items'] ?? [], 'quantity')),
        ];
    }

    private function messageFor(ReturnsApiException $e): string
    {
        return match (true) {
            $e->httpStatus === 404 => 'Retoure nicht gefunden.',
            $e->httpStatus === 409 => 'Statuswechsel abgelehnt: '.$e->detail,
            $e->isRetryable() => 'Das Retourensystem ist gerade nicht erreichbar. Bitte später erneut versuchen.',
            default => 'Anfrage abgelehnt: '.$e->getMessage(),
        };
    }
}
