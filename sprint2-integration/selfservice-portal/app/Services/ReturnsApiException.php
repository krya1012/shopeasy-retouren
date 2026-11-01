<?php

namespace App\Services;

use RuntimeException;

/**
 * The REST mock did not accept a request. {@see self::isRetryable()} tells the caller whether trying again
 * can help (server down or overloaded) or not (the request itself is wrong).
 */
class ReturnsApiException extends RuntimeException
{
    private function __construct(
        string $message,
        public readonly int $httpStatus,
        private readonly bool $retryable,
        /** The explanation from the REST mock without our own prefix, e.g. for showing it to a user. */
        public readonly string $detail = '',
    ) {
        parent::__construct($message);
    }

    public static function notFound(string $what): self
    {
        return new self("{$what} not found (404)", 404, false);
    }

    /** The mock refused the change, e.g. a forbidden status change or an out-of-date expectedStatus. */
    public static function conflict(string $detail): self
    {
        return new self("Conflict (409): {$detail}", 409, false, $detail);
    }

    public static function badRequest(int $httpStatus, string $detail): self
    {
        return new self("Bad request ({$httpStatus}): {$detail}", $httpStatus, false);
    }

    /** Connection error, timeout or a 5xx answer. Trying again later may work. */
    public static function unavailable(string $detail, int $httpStatus = 0): self
    {
        return new self("REST mock unavailable: {$detail}", $httpStatus, true);
    }

    public function isRetryable(): bool
    {
        return $this->retryable;
    }
}
