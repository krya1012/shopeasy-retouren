<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Model;

/**
 * An event that was already applied to the REST mock. The unique event_id is the duplicate check.
 *
 * @property int $id
 * @property string $event_id
 * @property string $return_id
 */
#[Fillable(['event_id', 'return_id'])]
class ProcessedEvent extends Model
{
    public $timestamps = false;
}
