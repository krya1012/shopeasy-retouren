<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * The event log: one row for every message the consumer handled, whatever the outcome
     * (processed, duplicate, rejected, invalid). The portal shows it, and Sprint 3 (process mining)
     * can export it. Fields of broken messages can be empty, so most columns allow null.
     */
    public function up(): void
    {
        Schema::create('status_events', function (Blueprint $table) {
            $table->id();
            $table->string('event_id')->nullable()->index();
            $table->string('return_id')->nullable()->index();
            $table->string('old_status')->nullable();
            $table->string('new_status')->nullable();
            $table->timestamp('occurred_at')->nullable();
            $table->string('outcome')->index();
            $table->text('reason')->nullable();
            $table->timestamp('handled_at')->useCurrent();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('status_events');
    }
};
