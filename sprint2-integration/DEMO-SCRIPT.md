# Sprint 2 Demo (20 Minutes)

Step by step: how we show the REST mock, the gRPC service and the messaging. Every step has **Say** (what we tell), the **Command** and **You see** (what must appear). All commands run in the folder `sprint2-integration`.

The code was written with Claude Code (see `AI-USAGE.md`). Everyone reads their part before and must be able to explain it.

## Time plan

| Part | Minutes | Who |
|---|---|---|
| Intro | 1 | ___ |
| 1. REST mock | 3 | ___ |
| 2. gRPC service | 4 | ___ |
| 3. Messaging | 5 | ___ |
| 3b. Portal (optional) | 3 | ___ |
| 4. Tests and code | 1 | ___ |
| Questions and buffer | 6 (3 if 3b is shown) | everyone |

The team decides who presents what and writes it here.

## Intro (1 minute)

```
REST mock (8080)        gRPC service (9090)       Producer --> RabbitMQ --> Consumer
marketplace entry       condition check           status updates
"Marktplatz-Retoure     "Annahme/Ablehnung        (message steps in the target process)
 eingegangen (REST)"     entscheiden"
```

**Say:** We show three small systems that come together in the workflow later (Sprint 4). Each has a client to try it out. Everything is Java with Spring Boot; the broker is RabbitMQ. In each part we first show the normal case and then the error cases.

## Preparation (before the meeting, about 10 minutes)

1. Run `./gradlew build` once with internet. After that all libraries are on the computer.
2. Start RabbitMQ: `podman machine start` (if the VM is not running), then `podman start shopeasy-rabbitmq`. Check: http://localhost:15672 (user `guest`, password `guest`).
3. Start three terminals **fresh** (whoever practised before restarts everything, otherwise the memory is not empty any more):

```
./gradlew :rest-mock:bootRun --console=plain          # terminal 1
./gradlew :grpc-service:run --console=plain           # terminal 2
./gradlew :messaging-consumer:bootRun --console=plain # terminal 3
```

4. A fourth terminal for the client commands. Make the font large.
5. Browser tabs: http://localhost:8080/swagger-ui.html and the RabbitMQ web page. On the web page go to "Queues" → `returns.status.dlq` → press "Purge Messages" (every practice run puts 3 messages there; before the meeting it should be empty).

Ready when terminal 1 shows "Started RestMockApplication", terminal 2 shows "is running on port 9090" and terminal 3 shows "Started ConsumerApplication".

## 1. REST mock (3 minutes)

**Say:** The REST mock plays the marketplace (Amazon, Zalando). The format of the reports is the one from the task. The list can be filtered by status and marketplace.

```
./gradlew :rest-mock:runClient --args="list status=NEW marketplace=ZALANDO" --console=plain -q
```
**You see:** `-> 200` and `{"data":[{"returnId":"RET-2026-00457", ...` (the list is in the field `data`).

```
./gradlew :rest-mock:runClient --args="get RET-2026-00457" --console=plain -q
./gradlew :rest-mock:runClient --args="order ORD-2026-118932" --console=plain -q
```
**You see:** the single report and the order data (we designed the order format ourselves, the lecturer confirmed it).

```
./gradlew :rest-mock:runClient --args="status RET-2026-00457 APPROVED" --console=plain -q
```
**You see:** `"status":"APPROVED"` and a new `updatedAt`.

**Error cases:**
```
./gradlew :rest-mock:runClient --args="get RET-DOES-NOT-EXIST" --console=plain -q
./gradlew :rest-mock:runClient --args="status RET-2026-00457 NONSENSE" --console=plain -q
./gradlew :rest-mock:runClient --args="status RET-2026-00462 APPROVED" --console=plain -q
```
**You see:** `-> 404`, `-> 400` and `-> 409` (the return `RET-2026-00462` is rejected and must not be approved any more). Errors come as ProblemDetail (RFC 9457) with the matching status code.

**If there is time:** open Swagger UI in the browser and press "Try it out" at `GET /api/v1/returns`. The description of the interface is created automatically (OpenAPI).

## 2. gRPC service (4 minutes)

**Say:** The service judges the condition of an article and recommends acceptance, rejection or manual review. The interface is the lecturer's `.proto` (we only added Java options). The rules are in `ConditionRules`. In terminal 2 you can see what the server does.

**Normal cases:**
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00458 SH-DRS-017-S DAMAGED" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00459 SH-JKT-088-L AS_NEW tags=false" --console=plain -q
```
**You see:** `ACCEPT (score 0.95`, `REJECT (score 0.95` and `MANUAL_REVIEW (score 0.5, manual review: true)`. The borderline case goes to a person later in the workflow (Sprint 4).

```
./gradlew :grpc-service:runClient --args="get RET-2026-00457 SH-BLZ-042-M" --console=plain -q
```
**You see:** the same result as before, now fetched.

**Error cases:**
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00460 SH-SNK-120-42 UNSPECIFIED" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-FAIL-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
**You see:**
- `ERROR [ALREADY_EXISTS] Duplicate request` (the same check again)
- `ERROR [INVALID_ARGUMENT] Invalid data: observed_condition must be set`
- `Service unavailable, attempt 2 of 3` and then the result (simulated outage, the client retries)

**If there is time:** show the deadline. It takes 3 seconds.
```
./gradlew :grpc-service:runClient --args="check RET-SLOW-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
**You see:** `ERROR [DEADLINE_EXCEEDED] Timeout ...`. The client deliberately does not retry here, because the service may have stored the check already.

Note: `RET-FAIL-1` fails only the first time and `RET-SLOW-1` is stored only once. When practising, use `RET-FAIL-2`, `RET-SLOW-2` and so on.

**Fallback:** `./gradlew :grpc-service:runClient --console=plain -q` without arguments plays all twelve scenarios in a row (about 8 seconds).

## 3. Messaging with RabbitMQ (5 minutes)

**Say:** A producer reports status changes, the consumer updates the status. The producer only knows the exchange, no receivers. This decouples the systems.

**Show the layout** (RabbitMQ web page): under "Exchanges" `returns.status`, under "Queues" `management`, `customer-info` and `dlq` (dead letter queue; the full names start with `returns.status.`). At `returns.status` under "Bindings": `return.status.#` goes to `management`, only `APPROVED`, `REJECTED`, `ACCEPTED` and `REFUNDED` go to `customer-info`.

**Start the producer** (terminal 4, takes about 5 seconds):
```
./gradlew :messaging-producer:bootRun --console=plain
```
**You see in the producer:** `Sent: ... -> return.status.APPROVED (broker confirmed)` for every message.

**In the consumer (terminal 3) we show one after the other:**
1. `Status updated: RET-... NEW -> APPROVED` up to `REFUNDED -> CLOSED`: the whole path of a return.
2. `Duplicate ignored: event ... was already processed`: the same event came twice, it is processed only once (recognised by the `eventId`).
3. `INSPECTED -> MANUAL_REVIEW -> ACCEPTED`: the borderline case with manual review.
4. `WARN Management rejects message: Status change not allowed: ... NEW -> INSPECTED`: a forbidden status change is rejected and goes to the dead letter queue.
5. `Message is not valid event JSON` and `Required field is missing: returnId`: the consumer does not understand the message, it also goes to the dead letter queue.
6. On the RabbitMQ web page, `returns.status.dlq` now holds **3 messages** (the display updates every 5 seconds). "Get messages" shows the content and, in the header `x-death`, the reason (`rejected`).
7. `Customer info: customer informed, ...` only for `APPROVED`, `ACCEPTED`, `REFUNDED`: the second receiver only gets its statuses.

**Error case: broker not reachable** (optional, about 1 minute). First stop the consumer in terminal 3 with Ctrl+C, otherwise it fills the log with connection messages (it reconnects by itself after the broker is back, which takes up to 10 seconds):
```
podman stop shopeasy-rabbitmq
./gradlew :messaging-producer:bootRun --console=plain
podman start shopeasy-rabbitmq
```
**You see:** `Sending failed ... attempt 2 of 3`, `attempt 3 of 3` and `ERROR: event not delivered. Broker not reachable after 3 attempts`. Afterwards restart the consumer in terminal 3 with the command from the preparation.

**Cannot be shown live:** "No queue accepts the message" (the broker returns it). In the live setup every routing key finds a queue. The test `StatusEventPublisherTest` shows it.

## 3b. Self-service portal (optional, 3 minutes, only if time is left)

**Say:** A second consumer, written in PHP, receives the same events from its own queue and updates the REST mock. The producer is not changed. The portal shows the result.

**Preparation** (before the meeting; the first run of the portal needs `composer install && npm install && npm run build`, see README section 4):
1. Restart the REST mock (terminal 1), so the statuses are as at the start.
2. Empty the portal queue and the dead letter queue (RabbitMQ web page → "Purge Messages", or `docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.portal`).
3. In `selfservice-portal`: `php artisan migrate:fresh --seed`, `php artisan serve`, and in another terminal `php artisan returns:consume`.

**Steps:**
1. Log in as the customer (`j.mueller@example.com`). **You see:** only `RET-2026-00457`. Open it: status "Neu", empty history. Log out.
2. Log in as the clerk (`clerk@shopeasy.test`). **You see:** all six returns and the link "Ereignisprotokoll".
3. Send the events: `./gradlew :messaging-producer:bootRun --args='--scenario=portal' --console=plain`.
4. **You see in the PHP consumer:** `processed` for the path of `RET-2026-00457` and `00458`, then `duplicate` (the late duplicate of APPROVED), then `rejected` (the unknown `RET-2026-99999`).
5. Reload the list: `RET-2026-00457` is "Abgeschlossen", `RET-2026-00458` "Angenommen". The status in the **REST mock** really changed (`curl localhost:8080/api/v1/returns/RET-2026-00457`).
6. Event log: counts "Verarbeitet 13", "Duplikat 1", "Abgelehnt 1" and the reason `Return RET-2026-99999 not found (404)`. The unknown event is also in `returns.status.dlq`.
7. **Conflict:** open `RET-2026-00459` as the clerk, change it with `curl -X PATCH localhost:8080/api/v1/returns/RET-2026-00459 -H 'Content-Type: application/json' -d '{"status":"LABEL_SENT"}'` and again with `RECEIVED`, then choose "Retourenlabel versendet" in the page. **You see:** the refusal "Status is RECEIVED, not the expected APPROVED", and the page now shows the real status.

**Say:** The change of the status uses the status the page was showing (`expectedStatus`). The mock refuses it if someone else was faster, so nobody overwrites a newer state.

## 4. Tests and code (1 minute)

```
./gradlew test --rerun --console=plain
```
**You see:** every test with its name and `PASSED`, at the end `BUILD SUCCESSFUL` (74 tests, about 8 seconds). The test names describe the cases (`duplicateEventIsIgnored`, `slowServiceRunsIntoTheClientDeadline`).

**Say:** Each module has small classes with one job each (every class has a Javadoc comment that explains it):
- REST: `ReturnController` (endpoints), `ReturnStore` (data), `RestClientDemo` (client)
- gRPC: `ConditionRules` (rules), `RequestValidator` (check of the input), `ArticleConditionServiceImpl` (service), `ConditionClient` (client with retry and deadline)
- Messaging: `StatusEventPublisher` (sending with confirmation and retry), `StatusListeners` (queues), `StatusUpdateHandler` (logic), `StatusTransitions` (allowed changes), `EventParser` (check of the message)
- Portal (PHP): `ConsumeStatusEvents` (broker), `StatusEventHandler` (logic), `ReturnsApiClient` (REST mock), `ReturnPolicy` (who may do what); the tests run with `composer ci:check` in `selfservice-portal`

## If something does not work

- **Port in use:** `lsof -i :8080` (or 9090), stop the old process.
- **RabbitMQ not reachable:** `podman machine start`, then `podman start shopeasy-rabbitmq`.
- **Gradle downloads something:** that needs internet. So run `./gradlew build` once before.
- **Result looks different:** restart the servers (the memory is then empty again); for example gRPC otherwise reports `ALREADY_EXISTS`.
- **Fallback:** gRPC demo without arguments, then `./gradlew test --rerun`. The tests show all cases without a broker, too. A screen recording of an earlier run can serve as backup (do not put it into the repo).

## Questions that may come

- **Why a topic exchange and two queues?** The producer knows nothing about the receivers. A new receiver gets a new queue, nothing changes in the producer. (`RabbitConfig`)
- **How do you detect duplicate events?** The consumer remembers the `eventId` (`ProcessedEvents`). A duplicate is confirmed but not processed again. After a restart the list is empty; in production it belongs in a database.
- **Why is a message that is not understood not delivered again?** The second time it would not be more understandable either, and it would loop forever. It goes to the dead letter queue where a person can check it. (`StatusListeners`)
- **What do publisher confirms and `mandatory` mean?** The broker confirms that it accepted the message. With `mandatory` it also reports if no queue got the message. (`StatusEventPublisher`)
- **Why `ALREADY_EXISTS` for a duplicate check?** The task names the duplicate request as an error case. The client sees that the check exists already and can fetch it with `get`.
- **Why does the client not retry on `DEADLINE_EXCEEDED`?** The service may have stored the check already, a retry would give `ALREADY_EXISTS`. Look with `get` first. On `UNAVAILABLE` (not processed) it does retry.
- **Why is the batch check all or nothing?** The `.proto` has no error field per item. The lecturer confirmed that this is fine.
- **Why does the REST mock give 409 for a status change?** The request does not fit the state of the return. The same changes apply as in the messaging. The same status again gives 200, so a request may be repeated safely after a timeout. (`ReturnStore`, `StatusTransitions`)
- **Why is a return without original packaging not rejected?** The packaging is no condition for taking a return back. We only lower the score from 0.95 to 0.85 and say so in the reason. (`ConditionRules`)
- **Who defined the rules for acceptance and rejection?** They are a proposal from `TASK-ANALYSIS.md` (section 5.1). The team must agree on them before the meeting. Until then `AI-USAGE.md` says "open".
- **What did the AI do?** The code was written with Claude Code and is documented in `AI-USAGE.md`. We have read it and can explain it (this must be true before the meeting).
- **How does this relate to Sprint 1?** The REST mock matches "Marktplatz-Retoure eingegangen (REST)", the gRPC service "Annahme/Ablehnung entscheiden", the messaging the message steps. We adapt the models in Sprint 4 (section 6 in `TASK-ANALYSIS.md`).

## After the meeting

Stop the terminals with Ctrl+C, then `podman stop shopeasy-rabbitmq`.
