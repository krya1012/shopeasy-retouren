<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Carbon;

/**
 * One handled message in the event log. See the migration for why most fields can be null.
 *
 * @property int $id
 * @property string|null $event_id
 * @property string|null $return_id
 * @property string|null $old_status
 * @property string|null $new_status
 * @property Carbon|null $occurred_at
 * @property string $outcome processed | duplicate | rejected | invalid | manual
 * @property string|null $reason
 * @property Carbon $handled_at
 */
#[Fillable(['event_id', 'return_id', 'old_status', 'new_status', 'occurred_at', 'outcome', 'reason'])]
class StatusEvent extends Model
{
    public const PROCESSED = 'processed';

    public const DUPLICATE = 'duplicate';

    public const REJECTED = 'rejected';

    public const INVALID = 'invalid';

    /** A clerk changed the status in the portal (no message was involved). */
    public const MANUAL = 'manual';

    public $timestamps = false;

    protected function casts(): array
    {
        return [
            'occurred_at' => 'datetime',
            'handled_at' => 'datetime',
        ];
    }
}
