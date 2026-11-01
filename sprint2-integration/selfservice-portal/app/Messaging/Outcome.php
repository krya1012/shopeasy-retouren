<?php

namespace App\Messaging;

/** What the consumer has to do with the message after {@see StatusEventHandler::handle()}. */
enum Outcome: string
{
    /** Applied to the REST mock. Acknowledge the message. */
    case Processed = 'processed';

    /** The same eventId was handled before. Acknowledge and ignore it. */
    case Duplicate = 'duplicate';

    /** Cannot be handled, now or ever. Reject without requeue, so the broker sends it to the dead letter queue. */
    case Rejected = 'rejected';
}
