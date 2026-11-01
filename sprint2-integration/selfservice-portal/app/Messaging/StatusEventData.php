<?php

namespace App\Messaging;

use stdClass;

/**
 * A status event that passed the JSON Schema check. Only this typed object is used after the validation, so
 * the rest of the code never reads fields from an untyped decoded message.
 */
final readonly class StatusEventData
{
    public function __construct(
        public string $eventId,
        public string $returnId,
        public string $oldStatus,
        public string $newStatus,
    ) {}

    /** @param  stdClass  $data  a decoded message that {@see EventValidator} accepted (all fields exist and are strings) */
    public static function fromValidated(stdClass $data): self
    {
        return new self($data->eventId, $data->returnId, $data->oldStatus, $data->newStatus);
    }
}
