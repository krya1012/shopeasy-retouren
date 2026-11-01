<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Third Party Services
    |--------------------------------------------------------------------------
    |
    | This file is for storing the credentials for third party services such
    | as Resend, Postmark, AWS, and more. This file provides the de facto
    | location for this type of information, allowing packages to have
    | a conventional file to locate the various service credentials.
    |
    */

    'postmark' => [
        'key' => env('POSTMARK_API_KEY'),
    ],

    'resend' => [
        'key' => env('RESEND_API_KEY'),
    ],

    'ses' => [
        'key' => env('AWS_ACCESS_KEY_ID'),
        'secret' => env('AWS_SECRET_ACCESS_KEY'),
        'region' => env('AWS_DEFAULT_REGION', 'us-east-1'),
    ],

    'slack' => [
        'notifications' => [
            'bot_user_oauth_token' => env('SLACK_BOT_USER_OAUTH_TOKEN'),
            'channel' => env('SLACK_BOT_USER_DEFAULT_CHANNEL'),
        ],
    ],

    'rest_mock' => [
        'url' => env('REST_MOCK_URL', 'http://localhost:8080'),
        'timeout' => (int) env('REST_MOCK_TIMEOUT', 3),
    ],

    // Allowed status changes, shared with the Java modules (see sprint2-integration/contracts)
    'status_transitions' => env('STATUS_TRANSITIONS', base_path('../contracts/status-transitions.json')),

    'rabbitmq' => [
        'host' => env('RABBITMQ_HOST', 'localhost'),
        'port' => (int) env('RABBITMQ_PORT', 5672),
        'user' => env('RABBITMQ_USER', 'guest'),
        'password' => env('RABBITMQ_PASSWORD', 'guest'),
        // JSON Schema of the status event, shared with the Java modules (see sprint2-integration/contracts)
        'event_schema' => env('STATUS_EVENT_SCHEMA', base_path('../contracts/status-event.schema.json')),
    ],

];
