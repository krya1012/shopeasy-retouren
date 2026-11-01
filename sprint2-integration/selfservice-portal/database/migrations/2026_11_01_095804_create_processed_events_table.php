<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * One row per event that was applied to the REST mock. The unique key on event_id is what makes
     * the consumer idempotent: an event with a known id is a duplicate and is skipped.
     */
    public function up(): void
    {
        Schema::create('processed_events', function (Blueprint $table) {
            $table->id();
            $table->uuid('event_id')->unique();
            $table->string('return_id')->index();
            $table->timestamp('processed_at')->useCurrent();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('processed_events');
    }
};
