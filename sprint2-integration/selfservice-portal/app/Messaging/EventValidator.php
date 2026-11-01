<?php

namespace App\Messaging;

use Opis\JsonSchema\Errors\ErrorFormatter;
use Opis\JsonSchema\Validator;
use RuntimeException;

/**
 * Checks a decoded message against the JSON Schema in sprint2-integration/contracts. The Java modules use
 * the same event; this file is the shared definition of it.
 */
class EventValidator
{
    private readonly object $schema;

    public function __construct(string $schemaPath)
    {
        $json = @file_get_contents($schemaPath);
        if ($json === false) {
            throw new RuntimeException("Event schema not found: {$schemaPath}");
        }
        $this->schema = json_decode($json, flags: JSON_THROW_ON_ERROR);
    }

    /**
     * @param  mixed  $data  the result of json_decode WITHOUT the assoc flag (the validator needs objects)
     * @return string|null null if the message is valid, otherwise a short description of the first problems
     */
    public function validate(mixed $data): ?string
    {
        $validator = new Validator;
        $result = $validator->validate($data, $this->schema);

        if ($result->isValid()) {
            return null;
        }

        $errors = (new ErrorFormatter)->formatFlat($result->error());

        return implode('; ', array_slice($errors, 0, 3));
    }
}
