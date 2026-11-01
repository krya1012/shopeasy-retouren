<?php

namespace App\Policies;

use App\Models\User;

/**
 * Who may do what with a return. A return is not a database model here (it lives in the REST mock), so the
 * methods are registered by name in AppServiceProvider instead of being found automatically.
 *
 * - Customers see only their own returns (the e-mail of the return matches their account).
 * - Clerks see all returns and are the only ones who may change a status.
 */
class ReturnPolicy
{
    /** @param  array<string, mixed>  $return  a return as the REST mock delivers it */
    public function view(User $user, array $return): bool
    {
        if ($user->isClerk()) {
            return true;
        }

        $owner = $return['customer']['email'] ?? '';

        return is_string($owner) && strcasecmp($owner, $user->email) === 0;
    }

    public function changeStatus(User $user): bool
    {
        return $user->isClerk();
    }

    public function viewEventLog(User $user): bool
    {
        return $user->isClerk();
    }
}
