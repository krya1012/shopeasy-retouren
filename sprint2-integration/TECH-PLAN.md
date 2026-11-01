# Plan v2: Sprint 2 (ShopEasy Retouren) aligned with the FZI HiWi role — analysis and revision

## Context
Sprint 2 lives in `sprint2-integration/`: four Java 21 / Spring Boot 4 modules with 69 green tests.
- Due: 3.11. 11:30, ILIAS.
- Graded: messaging, gRPC and code quality. A UI is not graded.

Plan v1 proposed a Laravel + Vue portal, a Laravel RabbitMQ consumer, Keycloak and CI/CD. This v2 records the review of v1 against the code and the posting, and the corrected plan. Decision made by the user: **Keycloak moves to Sprint 4.**

## Analysis of plan v1

### Blocking defects (v1 would not work as written)
| # | Defect | Evidence | Fix in v2 |
|---|---|---|---|
| B1 | The producer's events use random return IDs (`"RET-2026-" + millis % 100000`) that the REST mock does not know. Every PATCH from the Laravel consumer would return 404, so v1 verification step 3 fails. | `messaging-producer/.../DemoScenario.java:29`; seed data `rest-mock/.../ReturnStore.java` (`RET-2026-00457`…`00462`) | Add a demo scene that uses seeded IDs. The consumer treats 404 as a poison message (reject, goes to the DLQ). |
| B2 | The DLX `returns.status.dlx` is a *direct* exchange bound with key `dead-letter`. A portal queue without `x-dead-letter-routing-key=dead-letter` keeps the routing key `return.status.X`, so RabbitMQ **silently drops** rejected messages. Declaring the queue with different arguments also fails with `PRECONDITION_FAILED`. | `messaging-consumer/.../RabbitConfig.java` (`DLQ_ROUTING_KEY = "dead-letter"`, `withDeadLetter`) | Declare `returns.status.portal` with exactly `x-dead-letter-exchange=returns.status.dlx` and `x-dead-letter-routing-key=dead-letter`. Also declare the DLX exchange idempotently, so start order does not matter. |
| B3 | The PATCH ignores the event's `oldStatus`. The mock checks only its own current state. A stale event can apply when it should not, unlike the Java consumer, which checks `oldStatus == current`. | `ReturnController.java:60-70`, `ReturnStore.updateStatus`, `StatusUpdateHandler.java:40-65` | Add an optional `expectedStatus` to the PATCH body. A mismatch returns 409 (optimistic concurrency). This is a small Java change and existing calls still work. |
| B4 | "Nack with requeue on 5xx" loops hot. Classic queues have no delivery limit, so one bad message blocks the queue forever. | queue type is classic (`QueueBuilder.durable`) | Retry in-process 3 times with backoff (the same pattern as `ConditionClient.java:69-82`), then reject to the DLQ. |
| B5 | A late duplicate (A→B arrives again after B→C) would get a 409 and land in the DLQ by mistake. The mock's "same status = 200" is not enough for idempotency. | `ReturnStore.updateStatus` same-status branch | Check the persistent `processed_events` table *before* the PATCH (v1 had the table but not the order). |

### Feasibility
- **PHP and Composer are not installed locally** (`php: missing`, `composer: missing`). Node 26, Docker and Podman are present. Fix: `brew install php composer`, about 10 min, which is step 0.
- **Effort:** v1 added up to 2 h + ½ d + ½ d + 3 h + Phase 5, about 1.5–2 days. That leaves no time for team review, AI-USAGE entries and demo rehearsal. v2 cuts Keycloak and most of Phase 5.
- **Phase order bug:** the v1 CI "docker" job needs Dockerfiles, but those only came in Phase 5. v2 runs CI as build and test only. Image builds move to Sprint 3.
- **Versions in v1 were outdated:** Laravel 13 shipped on 17 March 2026, and the Vue starter kits need PHP ≥ 8.4. v2 uses Laravel 13 with PHP 8.4.
- **Team risk:** the stack is still "not agreed" (README:5, DoD point 1). The lecturer asks questions about AI-generated code, so every member must be able to explain the PHP consumer. That needs a 30-min walkthrough in the plan.

### Fit to the posting
| Posting item | v1 | v2 |
|---|---|---|
| SelfService System, Laravel + Vue | Strong | Strong. Make it truly *self-service*: a `customer` role sees only their own returns (matched by email) and tracks the status. `clerk` handles the back office. |
| Identity Management System | Weak. Keycloak *login* only consumes an IdM; it does not build one. | Laravel-native auth from the starter kit, plus roles and Policies/Gates (RBAC, authN vs authZ, data-level filtering). Keycloak SSO follows in Sprint 4. |
| CI/CD into the dev process | Good | Add process proof: PR checks required on `main`, Dependabot, a CI badge, and work through PRs reviewed by teammates. |
| PHP, HTML/CSS/JS | Yes | Yes. Pest tests and Pint (code style) show quality habits. |
| German | Not addressed | Portal UI in German (`lang/de`). Docs stay English. |
| Working independently and in a structured way | Implicit | Visible in a clean commit history, PRs, issues per phase, and the plan in the repo. |

### Fit to grading
- The graded criteria favour the Laravel *consumer*: it is part of messaging and covers "doppelte Events?", error handling and decoupling. Do it before the portal UI.
- "saubere Eventdefinition" calls for a JSON Schema for `StatusEvent`. This stays in v2 (cheap, graded).
- Testcontainers, Actuator, gRPC health and correlation IDs are useful but ungraded. They move to Sprint 3/4.

## Revised plan (in priority order; any cut-off still leaves a hand-in)

### Step 0 — Prerequisites (≈30 min)
- `brew install php composer`, then `composer global require laravel/installer`.
- Get team OK for PHP/Laravel in the chat (DoD point 1).
- Create a feature branch `feature/laravel-portal`. Do not work on `main`.

### Step 1 — CI (≈1.5 h)
- `.github/workflows/ci.yml`, on push and PR, with `paths` filters:
  - **java:** `actions/setup-java@v4` (Temurin 21) and `gradle/actions/setup-gradle`. Run `./gradlew test` with `working-directory: sprint2-integration`, then upload the test reports.
  - **php:** `shivammathur/setup-php` (8.4). Run `composer install`, `php artisan test` and `vendor/bin/pint --test` in `sprint2-integration/selfservice-portal`.
  - **frontend:** `npm ci` and `npm run build` in the same folder.
- `.github/dependabot.yml` (gradle, composer, npm, github-actions). Add a CI badge in `README.md`.
- After the merge: require the status checks on `main` (GitHub branch protection, done by the user in Settings).

### Step 2 — Event contract + small Java changes (≈1.5 h)
- `sprint2-integration/contracts/status-event.schema.json` (JSON Schema 2020-12). Fields: the `StatusEvent` fields plus `schemaVersion`. Required fields as in `EventParser.java:26-42`.
- REST mock: an optional `expectedStatus` in `StatusUpdate`. `ReturnStore.updateStatus` throws on mismatch, which gives 409 (B3). Add 2 tests in `ReturnApiTest.java`.
- Producer: a new scene in `DemoScenario` that uses seeded IDs (B1):
  - `RET-2026-00457`: NEW→APPROVED→LABEL_SENT→RECEIVED→INSPECTED→ACCEPTED→REFUNDED→CLOSED
  - `RET-2026-00461`: RECEIVED→INSPECTED→MANUAL_REVIEW→ACCEPTED
  - a late duplicate of the `00457` APPROVED event
  - an unknown ID (goes to the DLQ through the 404)
- Update the README event-contract section so it points to the schema.

### Step 3 — Laravel consumer (≈4 h; graded, closes Q4)
- `laravel new selfservice-portal` (Laravel 13, official Vue starter kit with Inertia, Vue 3, TS and Tailwind, plus built-in auth). SQLite.
- `app/Services/ReturnsApiClient.php` wraps `Http::baseUrl(config('services.rest_mock.url'))->timeout(3)`. It provides `list`, `get`, `order` and `updateStatus(id, status, expected)`, and maps 400/404/409 to domain exceptions.
- `app/Console/Commands/ConsumeStatusEvents.php` (`php artisan returns:consume`) uses `php-amqplib/php-amqplib`. It does not use Laravel's queue driver, because the payloads are raw JSON from Java, not Laravel jobs. For each message:
  1. Declare `returns.status.dlx` (direct) and `returns.status.portal` with the exact DLX arguments from B2. Bind `return.status.#`. Set `basic_qos` prefetch to 1 and use manual ack.
  2. Parse and validate against `contracts/status-event.schema.json` (`opis/json-schema`). If invalid: reject, no requeue, goes to the DLQ.
  3. Dedup: insert into `processed_events` (unique `event_id`) inside a DB transaction, **before** the PATCH (B5). If it is a duplicate: ack and log.
  4. PATCH with `expectedStatus = oldStatus`. Outcomes:
     - 200: commit, then ack.
     - 404 or 409: roll back the dedup row, write the event with outcome `rejected` to `status_events`, then reject to the DLQ.
     - 5xx or timeout: retry 3 times (200/400/800 ms), then roll back and reject to the DLQ (B4).
  5. Write every outcome to `status_events` (eventId, returnId, old, new, occurredAt, outcome, reason). This is the event log for Sprint 3.
- Pest tests: `Http::fake()` covering 200 / 404 / 409 / 5xx-then-200 / 5xx×3, invalid JSON, schema failure, and a duplicate eventId. Keep the handler logic in `app/Messaging/StatusEventHandler.php` so the tests need no broker.

### Step 4 — Vue self-service portal with roles (≈4 h)
- Roles: a `role` column on `users` (`customer`, `clerk`). Seeder: `clerk@shopeasy.test`, plus `j.mueller@example.com` as a customer, matching seed return 00457.
- `app/Policies/ReturnPolicy.php`: a customer may view only returns whose `customer.email` equals their own; only a clerk may change the status. Filtering happens server-side in the controller.
- Inertia pages in `resources/js/pages/Returns/`:
  - `Index.vue`: filters for status and marketplace (query parameters of `GET /api/v1/returns`).
  - `Show.vue`: items, customer and order, plus the event history from `status_events`. A clerk also sees a dropdown of the allowed next states; a 409 shows as a toast.
  - `Events.vue`: the event log with outcomes, polled every 3 s.
- German UI labels (`lang/de.json`). Pest feature tests: a customer sees only their own returns; a customer status change gives 403; a clerk change gives 200 or a 409 message.

### Step 5 — Docs and DoD (≈1.5 h, required)
- Update `sprint2-integration/README.md` with the architecture, run commands and the portal queue in the topology diagram.
- `DEMO-SCRIPT.md`: portal scene (customer login shows own returns only; clerk changes a status; the producer scene runs; the event log fills; the DLQ shows the 404, 409 and invalid messages).
- `AI-USAGE.md`: one row per step. Hold a 30-min team walkthrough of `StatusEventHandler.php` and `ReturnPolicy.php`.
- Update `sprint2-integration/TECH-PLAN.md` to this v2. Do not commit `sprint1-bpm.zip`.

## Moved to later sprints
- **Sprint 3 (Celonis):**
  - `php artisan returns:export-eventlog` writes `status_events` as CSV (case = returnId, activity = newStatus, timestamp = occurredAt).
  - Demo timestamps are seconds apart, so add a synthetic log generator for realistic durations.
  - Dockerfiles (`bootBuildImage`, Laravel multi-stage) and a GHCR push job (the "CD" part).
  - Testcontainers RabbitMQ test.
- **Sprint 4 (Camunda 8):**
  - Keycloak realm `shopeasy`. Portal SSO via `laravel/socialite` + `socialiteproviders/keycloak`. Keycloak roles map to the Step 4 roles.
  - The same Keycloak serves Camunda Identity.
  - The REST mock becomes an OAuth2 resource server.
  - The portal gets a manual-review inbox through the Tasklist API.
  - Job workers call gRPC and REST.
  - Actuator health and the gRPC health service.
- **Sprint 5 (n8n / UiPath):** a portal webhook to n8n for the fraud score (`t_ai`), shown in `Show.vue`.
- **Sprint 6:** deploy the stack from GHCR images; write a DevOps chapter.

## Verification
1. Run `./gradlew test` in `sprint2-integration` (all old tests plus the new `expectedStatus` tests green). Run `php artisan test` and `npm run build` in `selfservice-portal`. Check that the CI run is green on the feature-branch PR.
2. Run `podman compose up -d` (RabbitMQ), then `./gradlew :rest-mock:bootRun`, then `php artisan returns:consume` and `composer run dev`.
3. Run `./gradlew :messaging-producer:bootRun` and check:
   - `GET localhost:8080/api/v1/returns/RET-2026-00457` ends in `CLOSED`.
   - The late duplicate appears once in `processed_events` and is logged as a duplicate.
   - In the RabbitMQ UI (:15672), `returns.status.dlq` holds the unknown-ID, forbidden-transition and invalid messages. None are lost (B2 check).
   - The Java consumer still logs as before.
4. Portal as `j.mueller@example.com`: sees only `RET-2026-00457`; a status change returns 403. Portal as clerk: changes `RET-2026-00458` NEW→APPROVED (200). A forbidden change shows the 409 toast.
