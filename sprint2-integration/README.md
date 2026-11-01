# Sprint 2: Integration Techniques (REST, gRPC, Messaging)

Three small programs that will be used in the process later. Each has a client to try it out. A fourth part, a self-service portal (Laravel + Vue), applies the status events to the REST mock and shows the returns. The task is in `Aufgabe DvG WS26_Sprint 2.pdf`, the analysis is in [TASK-ANALYSIS.md](TASK-ANALYSIS.md). AI use: [AI-USAGE.md](AI-USAGE.md).

The code was written with Claude Code (co-author Yaraslau Krautsou). Yaraslau Krautsou chose the language (Java with Spring Boot) and the broker (RabbitMQ) on 22.10.2026, and PHP (Laravel) and Vue for the portal in `selfservice-portal/`. No confirmation from the team is needed (decision of 01.11.2026).

| Folder | Program | Link to the target process (Sprint 1, labels are German) |
|---|---|---|
| `rest-mock/` | REST mock for return reports (Amazon, Zalando) | "Marktplatz-Retoure eingegangen (REST)" |
| `grpc-service/` | gRPC service for the condition check | "Annahme/Ablehnung entscheiden", "Zustand im WMS buchen" |
| `messaging-producer/`, `messaging-consumer/` | Status updates over RabbitMQ | message steps (`t_notify`, `t_notify_in`, `t_end_notify`) |
| `selfservice-portal/` | Laravel + Vue portal: second consumer (updates the REST mock) and pages for customers and clerks | "Marktplatz-Retoure eingegangen (REST)", status updates |
| `contracts/` | Shared definitions: JSON Schema of the status event, table of allowed status changes | – |

## Requirements

- **Run every command in the folder `sprint2-integration`** (`cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration`); `./gradlew` exists only there. Which window each command needs is shown step by step in [DEMO-SCRIPT.md](DEMO-SCRIPT.md).
- Java 21 or newer. Gradle is downloaded by `./gradlew`, no own installation needed.
- For the messaging: a container program (Podman or Docker).
- For the portal only: PHP 8.4 or newer with Composer, and Node.js 22 or newer.

All tests: `./gradlew test` (with `--rerun` it runs again and shows every test by name). Build everything: `./gradlew build`. Create the Javadoc pages: `./gradlew javadoc`.

**Demo (20 minutes):** the step-by-step script is in [DEMO-SCRIPT.md](DEMO-SCRIPT.md).

## Reading the code (for beginners)

The Javadoc comment at the top of each class explains what it does and the Spring Boot, gRPC or RabbitMQ idea behind it. A good order to read:

| Module | Start with | Then |
|---|---|---|
| `rest-mock` | `RestMockApplication` (how Spring Boot starts) | `ReturnController` (URLs), `ReturnStore` (data), `ReturnApiTest` |
| `grpc-service` | the `.proto` file (the interface) | `ArticleConditionServiceImpl` (service), `ConditionRules` (decision), `ConditionClient` (client), tests |
| `messaging-producer` | `StatusEvent` (the message) | `StatusEventPublisher` (sending), `DemoScenario`, `StatusEventPublisherTest` |
| `messaging-consumer` | `RabbitConfig` (queues and bindings) | `StatusListeners` (receiving), `StatusUpdateHandler` (logic), `StatusTransitions`, tests |

## 1. REST mock

```
./gradlew :rest-mock:bootRun       # server on port 8080
./gradlew :rest-mock:runClient     # client without arguments: demo, calls all endpoints
```

The client can also make single calls (the server must be running; use the environment variable `MOCK_URL` for another address):

```
./gradlew :rest-mock:runClient --args="list status=NEW marketplace=ZALANDO"
./gradlew :rest-mock:runClient --args="get RET-2026-00457"
./gradlew :rest-mock:runClient --args="order ORD-2026-118932"
./gradlew :rest-mock:runClient --args="status RET-2026-00457 APPROVED"
```

Description of the interface (OpenAPI): http://localhost:8080/v3/api-docs, page to try it out: http://localhost:8080/swagger-ui.html.

| Call | Purpose |
|---|---|
| `GET /api/v1/returns?status=NEW&marketplace=ZALANDO` | list, both filters are optional |
| `GET /api/v1/returns/{returnId}` | one report (404 if unknown) |
| `GET /api/v1/orders/{orderId}` | order data (404 if unknown) |
| `PATCH /api/v1/returns/{returnId}` with `{"status":"APPROVED"}` | change the status, `updatedAt` is set again. A forbidden change (e.g. `REJECTED` → `APPROVED`) gives 409. The same status again gives 200 and changes nothing (the request may be repeated) |

The format is the one from the task (slide 9): the list is in the field `data`, and the first test return `RET-2026-00457` is exactly the example from the slide. The single call and the order return one object without `data`. Errors come as `ProblemDetail` (RFC 9457) with a matching status code: 404 for unknown things, 400 for an invalid status or filter, 409 for a forbidden status change (same table as in the messaging, see section 3). There are six test returns in memory (`ReturnStore`); after a restart the data is back to the start state. The order data (`orderId`) has our own format. The task does not name one, and the lecturer confirmed that we may choose our own sensible format.

## 2. gRPC service

```
./gradlew :grpc-service:run          # server on port 9090
./gradlew :grpc-service:runClient    # client without arguments: demo, plays all scenarios
```

The client can also make single calls (the server must be running; use the environment variable `GRPC_TARGET` for another address, e.g. `myhost:9090`):

```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW"
./gradlew :grpc-service:runClient --args="check RET-2026-00458 SH-DRS-017-S AS_NEW tags=false"
./gradlew :grpc-service:runClient --args="check RET-2026-00459 SH-JKT-088-L AS_NEW packaging=false"
./gradlew :grpc-service:runClient --args="get RET-2026-00457 SH-BLZ-042-M"
```

The interface is the lecturer's `.proto` (`grpc-service/src/main/proto/article_condition_service.proto`). **Only change:** two `option java_...` lines, so the generated code is placed in a package. Service, messages and fields are unchanged. The lecturer confirmed this is fine. (The comments in the `.proto` are the lecturer's German originals.)

Rules of the check (decided by Yaraslau Krautsou, 01.11.2026):

| Condition | Other data | Decision | Score |
|---|---|---|---|
| `AS_NEW` | tag and original packaging present | `ACCEPT` | 0.95 |
| `AS_NEW` | tag present, original packaging missing | `ACCEPT` | 0.85 |
| `AS_NEW` | tag missing | `MANUAL_REVIEW` | 0.50 |
| `MINOR_WEAR` | – | `MANUAL_REVIEW` | 0.55 |
| `MISSING_TAGS` | – | `MANUAL_REVIEW` | 0.50 |
| `VISIBLY_WORN` | – | `REJECT` | 0.90 |
| `DAMAGED` | – | `REJECT` | 0.95 |
| `MISSING_PARTS` | – | `REJECT` | 0.90 |

Errors (gRPC status codes):

| Case | Status code |
|---|---|
| `return_id` or `sku` empty, condition not set, `inspected_at` not an ISO-8601 value | `INVALID_ARGUMENT` |
| The same check (`return_id` + `sku`) again | `ALREADY_EXISTS` |
| `GetCheckResult` for a check that does not exist | `NOT_FOUND` |
| Simulated outage: a `return_id` with the prefix `RET-FAIL` fails on the first call | `UNAVAILABLE` |
| Simulated slowness: a `return_id` with the prefix `RET-SLOW` is answered only after 5 seconds | the client gives up after its deadline of 3 seconds (`DEADLINE_EXCEEDED`) |
| Unexpected error in the service | `INTERNAL` (no details to the outside) |

**Batch check:** the `.proto` has no error field per item. So it is "all or nothing": if one item is wrong or a duplicate, the whole batch is rejected and nothing is stored. The message names the position (`items[1]: ...`).

**Client** (`ConditionClient`): every call has a deadline of 3 seconds. On `UNAVAILABLE` it retries up to three times; other errors are not retried (wrong data stays wrong, a duplicate must not be sent again). After a missed deadline it does **not** retry, because the service may have stored the check anyway (a retry would then give `ALREADY_EXISTS`). At the end there is a `ConditionClientException` with a readable message.

## 3. Messaging with RabbitMQ

### Start the broker

Podman (create the virtual machine once, then start the container):

```
podman machine init && podman machine start
podman run -d --name shopeasy-rabbitmq -p 5672:5672 -p 15672:15672 docker.io/library/rabbitmq:4-management
```

With Docker, `docker compose up -d` also works (file `compose.yaml`). Web page: http://localhost:15672 (user `guest`, password `guest`). Later `podman start shopeasy-rabbitmq` and `podman stop shopeasy-rabbitmq`.

### Start the programs (two terminals)

```
./gradlew :messaging-consumer:bootRun    # first: creates the queues and waits for messages
./gradlew :messaging-producer:bootRun    # plays the demo scenario and ends
```

### Layout

```
Producer --> exchange returns.status (topic)
              |-- return.status.#  --> queue management    (all status changes, updates the status)
              |-- ...APPROVED / REJECTED / ACCEPTED / REFUNDED --> queue customer-info (informs customers)
              |-- return.status.#  --> queue portal        (PHP consumer: updates the REST mock, see section 4)
            rejected messages --> exchange returns.status.dlx --> queue dlq (dead letter queue)
```

The queue names are `returns.status.management`, `returns.status.customer-info`, `returns.status.portal` and `returns.status.dlq`. The producer only knows the exchange and the routing key `return.status.<NEW_STATUS>`, no queues and no receivers. A third receiver needs no change in the producer.

### Event `ReturnStatusChanged` (JSON, the contract between producer and consumer)

```json
{
  "eventId": "3d70997d-0378-46d7-ae36-c9d8672865dc",
  "eventType": "ReturnStatusChanged",
  "returnId": "RET-2026-00457",
  "oldStatus": "NEW",
  "newStatus": "APPROVED",
  "occurredAt": "2026-10-22T11:47:25.123Z",
  "source": "ShopEasy-Returns-Management"
}
```

Required fields are `eventId`, `returnId`, `oldStatus`, `newStatus` and `occurredAt`. The consumer ignores unknown extra fields, so the event can be extended later. Producer and consumer each have their own copy of the classes; there is deliberately no shared code.

The formal definition is the JSON Schema [`contracts/status-event.schema.json`](contracts/status-event.schema.json). It lists the allowed status values and has an optional `schemaVersion` for later changes. The PHP consumer of the self-service portal validates every message against this file.

### Allowed status changes (decided by Yaraslau Krautsou, 01.11.2026)

```
NEW → APPROVED | REJECTED | IN_REVIEW
IN_REVIEW → APPROVED | REJECTED
APPROVED → LABEL_SENT → RECEIVED | EXPIRED
RECEIVED → INSPECTED → ACCEPTED | REJECTED | MANUAL_REVIEW
MANUAL_REVIEW → ACCEPTED | REJECTED
ACCEPTED → REFUNDED → CLOSED
REJECTED, EXPIRED → CLOSED
```

### Error handling

| Case | What happens |
|---|---|
| Broker not reachable | the producer tries three times, then `EventNotDeliveredException` and exit code 1 |
| Broker does not confirm (nack) or not at all | the producer reports the error (publisher confirms) |
| No queue accepts the message | the broker returns it (`mandatory`), the producer reports the error |
| The consumer does not understand the message (no JSON, required field missing, unknown status) | the consumer rejects it (reject, no redelivery), the message lies in the dead letter queue |
| Forbidden status change, or `oldStatus` does not match the known status | as above |
| **Duplicate event** (same `eventId`) | the consumer recognises it by the `eventId`, ignores it and confirms the message |
| Broker restarts | queues and messages are durable (durable, persistent) |

The demo scenario shows: the complete path of a return (seven status changes), a duplicate event, the borderline case `MANUAL_REVIEW`, a forbidden change, a message without JSON and a message without `returnId`. The consumer's log shows what happened each time.

**Limits:** the current status and the list of processed `eventId`s are in memory and empty after a restart. In production both belong in a database. The customer info does not check status changes; it trusts the events (the management receiver does the check). Nobody reads from the dead letter queue: the messages stay there until a person checks them (RabbitMQ web page, "Get messages").

## 4. Self-service portal (Laravel + Vue)

The portal in `selfservice-portal/` is a web application (PHP 8.4, Laravel 13, Vue 3 with plain JavaScript, SQLite). It does two jobs:

1. **Second consumer.** `php artisan returns:consume` has its own queue `returns.status.portal` on the same exchange. For each event it calls `PATCH` on the REST mock, so the status in the mock really changes. This answers open question 4 (the Java consumer still only keeps its own memory).
2. **Pages.** Customers see their own returns and their history. Clerks see all returns, may change a status (only the allowed next statuses are offered) and see the event log.

```
Producer --> exchange returns.status --> queue portal --> php artisan returns:consume --PATCH--> REST mock (Java)
                                                                  |                                  ^
                                                                  v                                  |
                                                       SQLite: processed_events, status_events       |
Browser --> Laravel + Vue (http://localhost:8000) -------------- GET / PATCH ------------------------+
```

### Start (order matters)

```
# terminal 1, in sprint2-integration: broker and REST mock
docker compose up -d                       # or the podman command from section 3
./gradlew :rest-mock:bootRun --console=plain

# terminal 2, in sprint2-integration/selfservice-portal (first time only: install and set up)
composer install && npm install
cp .env.example .env && php artisan key:generate
php artisan migrate --seed                 # creates the database and two demo users (see DatabaseSeeder)
npm run build                              # or "npm run dev" while working on the pages
php artisan serve                          # http://localhost:8000

# terminal 3, in selfservice-portal: the consumer
php artisan returns:consume

# terminal 4, in sprint2-integration: send the events for the portal
./gradlew :messaging-producer:bootRun --args='--scenario=portal' --console=plain
```

The scenario `portal` uses the returns that exist in the REST mock (`RET-2026-00457` and `00458`). **Start the REST mock fresh** before it, otherwise the statuses no longer fit. The normal scenario (without `--args`) uses made-up return ids: the portal would answer them with 404, so they would end up in the dead letter queue. Start the PHP consumer only for the portal demo. If the portal queue already holds old messages, empty it first: `docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.portal` (Podman: `podman exec ...`).

Demo users (created by `database/seeders/DatabaseSeeder.php`, the password is in `database/factories/UserFactory.php`): the clerk `clerk@shopeasy.test` and the customer `j.mueller@example.com`, who owns `RET-2026-00457`.

### What the consumer does with a message (`app/Messaging/StatusEventHandler.php`)

| Case | Result |
|---|---|
| Not JSON, or it breaks the JSON Schema (`contracts/status-event.schema.json`) | rejected, goes to the dead letter queue, logged as `invalid` |
| `eventId` already processed (checked **before** the call to the mock) | acknowledged and ignored, logged as `duplicate` |
| Mock answers `404` (unknown return) or `409` (forbidden change, or the return has not the status `oldStatus`) | rejected, dead letter queue, logged as `rejected` |
| Mock answers `5xx` or does not answer | retried 3 times (0.2, 0.4, 0.8 s), then rejected like above |
| Mock answers `200` | `eventId` remembered in `processed_events`, logged as `processed` |

The consumer sends `expectedStatus = oldStatus` with the `PATCH`. The mock then refuses the change if the return has another status, so an old event cannot overwrite a newer state. A repeated request for a status that is already set still gives `200`, so a message that is delivered twice after a crash does no harm.

### Who may do what

| | Customer | Clerk |
|---|---|---|
| List and open returns | only own returns (the e-mail of the return equals the account e-mail); another return gives `404` | all |
| Change a status | `403` | yes, only the next statuses allowed by `contracts/status-transitions.json` |
| Event log | `403` | yes |

The rules are in `app/Policies/ReturnPolicy.php` and are checked on the server. Hiding a button in the page is only for convenience.

### Shared definitions and tests

- `contracts/status-transitions.json` is the one table of allowed status changes. The portal reads it; the Java tests `StatusTransitionsContractTest` (REST mock and Java consumer) fail if their own table differs.
- The event is defined by `contracts/status-event.schema.json`.
- Portal tests: `composer ci:check` in `selfservice-portal` (format and lint, code style, PHPStan, 72 tests). They fake the REST mock, so neither the mock nor the broker has to run.
- CI: `.github/workflows/ci.yml` runs the Java tests and `composer ci:check` on every push and pull request.

## Enterprise Integration Patterns used

The task refers to the patterns of Hohpe and Woolf. These are used in the messaging (the lecturer named them as literature):

| Pattern | Where |
|---|---|
| Event Message | `ReturnStatusChanged`: reports that something happened |
| Publish-Subscribe Channel | topic exchange `returns.status`, several receiver queues |
| Message Router | routing key `return.status.<STATUS>` and the bindings of the queues |
| Guaranteed Delivery | persistent messages, durable queues, publisher confirms |
| Invalid Message Channel / Dead Letter Channel | dead letter queue `returns.status.dlq` |
| Idempotent Receiver | the consumer ignores duplicate `eventId`s |

## Check against the task (state 22.10.2026)

Checked against `Aufgabe DvG WS26_Sprint 2.pdf`, slide by slide:

| Slide | Requirement | State |
|---|---|---|
| 8 | three components, message broker | done: four programs, RabbitMQ |
| 9 | list with filter, single call, order, change status, format, client | done, format as in the example (`data`) |
| 10 | gRPC from the `.proto`, client, simulation, scenarios, errors | done; change to the `.proto` is only Java options, confirmed by the lecturer |
| 11 | status changes, producer, consumer, topics/queues | done |
| X | messaging and gRPC: errors, duplicate events, structure | done and tested; the slide talks about "Payment" and "invoices", the lecturer said it is old and to be ignored; we implement the error cases anyway |
| 12 | best practices, document AI | REST with version path, plural, status codes, ProblemDetail and OpenAPI; AI in `AI-USAGE.md` |

Deliberate limits:
- The Java consumer keeps the status in its own memory and does not call the REST mock. The PHP consumer of the portal does (section 4). A change that a clerk makes in the portal is not sent as an event, so the Java consumer does not see it. This is deliberate (decision of 01.11.2026): each component works on its own and the workflow joins them in Sprint 4. If the lecturer wants the Java consumer to write the status, a `ReturnStatusGateway` (in-memory by default, REST behind a Spring profile) is the planned change, see [TASK-ANALYSIS.md](TASK-ANALYSIS.md) section 7, question 4.
- The gRPC service does not use `image_urls` and `inspector_note`. The rules do not need them. `original_packaging` only lowers the score (no rejection, because the original packaging is no condition for taking a return back).

## Mapping to the grading aspects

| Aspect | Where |
|---|---|
| Messaging: clean event definition | section "Event", `StatusEvent` |
| Messaging: producer/consumer logic, decoupling | `StatusEventPublisher`, `StatusListeners`, `RabbitConfig`, two receiver queues |
| Messaging: errors (not deliverable, not understood, duplicate) | table "Error handling", tests `StatusEventPublisherTest`, `StatusUpdateHandlerTest`; in PHP `StatusEventHandler` with `StatusEventHandlerTest` (also 404, 409, retries) |
| Messaging: second consumer in another language (decoupling) | `selfservice-portal/app/Console/Commands/ConsumeStatusEvents.php`, `contracts/status-event.schema.json` |
| gRPC: clean interface | `.proto` unchanged except Java options |
| gRPC: errors in service and client | table "Errors", tests `ErrorHandlingTest`, `ConditionClientTest` |
| Code structure | small classes with one job per module; rules, validation and service are separate |
