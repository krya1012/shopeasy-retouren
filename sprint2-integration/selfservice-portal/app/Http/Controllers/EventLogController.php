<?php

namespace App\Http\Controllers;

use App\Models\StatusEvent;
use Illuminate\Support\Facades\Gate;
use Inertia\Inertia;
use Inertia\Response;

/** The event log for clerks: every message the consumer handled, with the outcome. The page polls for news. */
class EventLogController extends Controller
{
    public function index(): Response
    {
        Gate::authorize('eventLog.view');

        return Inertia::render('Events/Index', [
            'events' => StatusEvent::latest('id')->limit(100)->get(),
            'counts' => StatusEvent::selectRaw('outcome, count(*) as total')->groupBy('outcome')->pluck('total', 'outcome'),
        ]);
    }
}
