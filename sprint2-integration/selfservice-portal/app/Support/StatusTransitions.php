<?php

namespace App\Support;

use RuntimeException;

/**
 * The allowed status changes of a return. They are read from contracts/status-transitions.json, the same file
 * the Java modules are tested against, so the portal only offers changes the REST mock will accept.
 */
class StatusTransitions
{
    /** @var array<string, list<string>> */
    private array $table;

    public function __construct(string $path)
    {
        $json = @file_get_contents($path);
        if ($json === false) {
            throw new RuntimeException("Status transitions not found: {$path}");
        }
        $this->table = json_decode($json, true, flags: JSON_THROW_ON_ERROR);
    }

    /** @return list<string> the statuses a return may move to from $status (empty if unknown or final) */
    public function allowedFrom(string $status): array
    {
        return $this->table[$status] ?? [];
    }

    /** @return list<string> every known status */
    public function statuses(): array
    {
        return array_keys($this->table);
    }
}
