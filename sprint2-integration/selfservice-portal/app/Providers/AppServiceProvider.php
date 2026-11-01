<?php

namespace App\Providers;

use App\Messaging\EventValidator;
use App\Policies\ReturnPolicy;
use App\Support\StatusTransitions;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\Date;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Gate;
use Illuminate\Support\ServiceProvider;
use Illuminate\Validation\Rules\Password;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        $this->app->singleton(EventValidator::class, fn () => new EventValidator(
            config('services.rabbitmq.event_schema'),
        ));

        $this->app->singleton(StatusTransitions::class, fn () => new StatusTransitions(
            config('services.status_transitions'),
        ));
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        $this->configureDefaults();

        Gate::define('return.view', [ReturnPolicy::class, 'view']);
        Gate::define('return.changeStatus', [ReturnPolicy::class, 'changeStatus']);
        Gate::define('eventLog.view', [ReturnPolicy::class, 'viewEventLog']);
    }

    /**
     * Configure default behaviors for production-ready applications.
     */
    protected function configureDefaults(): void
    {
        Date::use(CarbonImmutable::class);

        DB::prohibitDestructiveCommands(
            app()->isProduction(),
        );

        Password::defaults(fn (): ?Password => app()->isProduction()
            ? Password::min(12)
                ->mixedCase()
                ->letters()
                ->numbers()
                ->symbols()
                ->uncompromised()
            : null,
        );
    }
}
