<?php

use App\Http\Controllers\EventLogController;
use App\Http\Controllers\ReturnController;
use Illuminate\Support\Facades\Route;

Route::inertia('/', 'Welcome')->name('home');

Route::middleware(['auth', 'verified'])->group(function () {
    // After the login everybody lands on the returns list (the route name 'dashboard' is used by the login redirect).
    Route::redirect('dashboard', '/returns')->name('dashboard');

    Route::get('returns', [ReturnController::class, 'index'])->name('returns.index');
    Route::get('returns/{returnId}', [ReturnController::class, 'show'])->name('returns.show');
    Route::post('returns/{returnId}/status', [ReturnController::class, 'updateStatus'])->name('returns.status');

    Route::get('events', [EventLogController::class, 'index'])->name('events.index');
});

require __DIR__.'/settings.php';
