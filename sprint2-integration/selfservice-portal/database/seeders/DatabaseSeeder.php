<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    use WithoutModelEvents;

    /**
     * Seed the application's database.
     */
    public function run(): void
    {
        // Demo users (password for both: "password"). The customer's e-mail is the one of the seeded return
        // RET-2026-00457 in the REST mock, so this customer sees exactly that return.
        User::factory()->create([
            'name' => 'Sachbearbeiterin Schmidt',
            'email' => 'clerk@shopeasy.test',
            'role' => User::CLERK,
        ]);

        User::factory()->create([
            'name' => 'Jonas Müller',
            'email' => 'j.mueller@example.com',
            'role' => User::CUSTOMER,
        ]);
    }
}
