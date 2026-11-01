# Sprint 2 Demo (20 Minutes)

Step by step: how we show the REST mock, the gRPC service and the messaging. Every step has **Say** (what we tell), the **Command** and **You see** (what must appear). **Which window each command goes into** (Terminal 1 to 6, Browser) and **in which folder** is explained in "Where and what to run" below. Read that section first.

The code was written with Claude Code (see `AI-USAGE.md`). Everyone reads their part before and must be able to explain it.

## Time plan

| Part | Minutes | Who |
|---|---|---|
| Intro | 1 | Eduard Ede Moser |
| 1. REST mock | 3 | Eduard Ede Moser |
| 2. gRPC service | 4 | Christian Sticker |
| 3. Messaging | 5 | Yaraslau Krautsou |
| 3b. Portal (optional) | 3 | Yaraslau Krautsou |
| 4. Tests and code | 1 | Yaraslau Krautsou |
| Questions and buffer | 6 (3 if 3b is shown) | everyone |

Split decided on 01.11.2026 (even split). No confirmation from the team is needed.

## Intro (1 minute)

```
REST mock (8080)        gRPC service (9090)       Producer --> RabbitMQ --> Consumer
marketplace entry       condition check           status updates
"Marktplatz-Retoure     "Annahme/Ablehnung        (message steps in the target process)
 eingegangen (REST)"     entscheiden"
```

**Say:** We show three small systems that come together in the workflow later (Sprint 4). Each has a client to try it out. The three systems are Java with Spring Boot; the broker is RabbitMQ. The optional portal at the end (3b) is PHP with Laravel and Vue. In each part we first show the normal case and then the error cases.

## Where and what to run (read this first)

### The one rule: the folder

**Every command runs in the folder `sprint2-integration`.** The starter `gradlew` (you type `./gradlew`) exists only there, not in the main folder of the repository (`shopeasy-retouren`). Start every Terminal window like this:

```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
pwd      # must end with /sprint2-integration
ls       # must list gradlew, compose.yaml, README.md, rest-mock, grpc-service ...
```

If you see `-bash: ./gradlew: No such file or directory` (or `zsh: no such file or directory: ./gradlew`), you are in the wrong folder, usually `shopeasy-retouren` instead of `shopeasy-retouren/sprint2-integration`. Run the `cd` line above. Only the portal scene (3b) uses a second folder, `sprint2-integration/selfservice-portal`.

### Programs you need

Check once in Terminal 4 (see below):

| Program | Needed for | Check | You see |
|---|---|---|---|
| Java 21 or newer | everything | `java -version` | version 21 or higher |
| Docker Desktop (the app must be **running**) or Podman | RabbitMQ (part 3) | `docker info` | server information, no error |
| PHP 8.4+, Composer, Node.js 22+ | only the portal (3b) | `php -v`, `composer -V`, `node -v` | versions |

Gradle itself is not needed: `./gradlew` downloads it the first time.

### The windows

**Terminal** is the macOS app "Terminal" (Cmd+Space, type `Terminal`, Enter) or the terminal in VS Code. **Browser** is Chrome or Safari.

| Window | Folder | What runs there | Ready when you see | Used in part |
|---|---|---|---|---|
| **Terminal 1** | `sprint2-integration` | `./gradlew :rest-mock:bootRun --console=plain` (REST mock, port 8080) | `Started RestMockApplication` | 1, 3b |
| **Terminal 2** | `sprint2-integration` | `./gradlew :grpc-service:run --console=plain` (gRPC service, port 9090) | `is running on port 9090` | 2 |
| **Terminal 3** | `sprint2-integration` | `./gradlew :messaging-consumer:bootRun --console=plain` (Java consumer) | `Started ConsumerApplication` | 3 |
| **Terminal 4** | `sprint2-integration` | **Your command window:** clients, producer, tests, docker commands | (you type here) | 1, 2, 3, 4 |
| Terminal 5 (3b) | `sprint2-integration/selfservice-portal` | `php artisan serve` (portal, port 8000) | `Server running on [http://127.0.0.1:8000]` | 3b |
| Terminal 6 (3b) | `sprint2-integration/selfservice-portal` | `php artisan returns:consume` (PHP consumer) | `Waiting for events on queue returns.status.portal` | 3b |
| **Browser tab A** | | http://localhost:8080/swagger-ui.html (REST mock, needs Terminal 1) | the Swagger page | 1 |
| **Browser tab B** | | http://localhost:15672 (RabbitMQ web page; user `guest`, password `guest`; needs the broker) | login, then "Overview" | 3 |
| Browser tab C (3b) | | http://localhost:8000 (portal, needs Terminal 5) | the login page | 3b |

**How a terminal behaves:** Terminals 1, 2, 3, 5 and 6 run a server. The command **never finishes**: the window shows the log and is "used up" until you press Ctrl+C. You cannot type another command there, so every server gets its own window. **Terminal 4** is the window where commands finish by themselves (clients, producer, tests) and give the prompt back.

**New window:** in the macOS Terminal press Cmd+T for a new tab. It normally starts in the same folder as the tab before. Check with `pwd`; if it is wrong, run the `cd` line again. The broker (Docker) must run **before** Terminal 3 is started.

### Stop and start again

Press Ctrl+C in each server window, then in Terminal 4 `docker compose down`. Do that after every practice run and start everything **fresh** (the servers keep their data in memory, so a second run on the old servers shows different results).

## Preparation (before the meeting, about 10 minutes)

1. **Terminal 4:** go to the folder (`cd` line above) and run `./gradlew build`. The first time it downloads libraries and Gradle and needs internet (a few minutes); afterwards it takes seconds. The last line must be `BUILD SUCCESSFUL`.
2. **Terminal 4, start RabbitMQ.** Start the Docker Desktop app first and wait until it says it is running. Then:
   ```
   docker compose up -d
   docker ps        # shopeasy-rabbitmq must say "Up ... (healthy)", wait up to 20 seconds
   ```
   With **Podman** instead: the first time `podman machine init && podman machine start` and `podman run -d --name shopeasy-rabbitmq -p 5672:5672 -p 15672:15672 docker.io/library/rabbitmq:4-management`; later `podman machine start` and `podman start shopeasy-rabbitmq`.
3. **Terminals 1, 2 and 3:** open three new windows (Cmd+T), one per server. In each: `cd` line, then its command from the table above. Start them one after the other, Terminal 3 last. Wait for the "Ready when" text of each.
4. **Terminal 4:** make the font large (menu View, "Bigger", or Cmd and +). Check the folder with `pwd`.
5. **Browser:** open tab A (Swagger UI) and tab B (RabbitMQ, login `guest` / `guest`).
6. **Browser tab B, empty the dead letter queue** (every practice run puts 3 messages there, before the meeting it must be empty): menu **Queues and Streams** → click `returns.status.dlq` → scroll down to the section **Purge** → button **Purge Messages** → confirm. Or in Terminal 4: `docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.dlq` (Podman: `podman exec ...`). The queue exists only after Terminal 3 has started.

Ready when Terminal 1 shows "Started RestMockApplication", Terminal 2 shows "is running on port 9090" and Terminal 3 shows "Started ConsumerApplication".

## 1. REST mock (3 minutes)

**Window:** all commands go into **Terminal 4**. Terminal 1 (the REST mock server) must be running.

**Say:** The REST mock plays the marketplace (Amazon, Zalando). The format of the reports is the one from the task. The list can be filtered by status and marketplace.

**Terminal 4:**
```
./gradlew :rest-mock:runClient --args="list status=NEW marketplace=ZALANDO" --console=plain -q
```
**You see:** `-> 200` and `{"data":[{"returnId":"RET-2026-00457", ...` (the list is in the field `data`).

**Terminal 4:**
```
./gradlew :rest-mock:runClient --args="get RET-2026-00457" --console=plain -q
./gradlew :rest-mock:runClient --args="order ORD-2026-118932" --console=plain -q
```
**You see:** the single report and the order data (we designed the order format ourselves, the lecturer confirmed it).

**Terminal 4:**
```
./gradlew :rest-mock:runClient --args="status RET-2026-00457 APPROVED" --console=plain -q
```
**You see:** `"status":"APPROVED"` and a new `updatedAt`.

**Error cases, Terminal 4:**
```
./gradlew :rest-mock:runClient --args="get RET-DOES-NOT-EXIST" --console=plain -q
./gradlew :rest-mock:runClient --args="status RET-2026-00457 NONSENSE" --console=plain -q
./gradlew :rest-mock:runClient --args="status RET-2026-00462 APPROVED" --console=plain -q
```
**You see:** `-> 404`, `-> 400` and `-> 409` (the return `RET-2026-00462` is rejected and must not be approved any more). Errors come as ProblemDetail (RFC 9457) with the matching status code.

**If there is time, Browser tab A:** in Swagger UI click `GET /api/v1/returns`, then "Try it out", then "Execute". The description of the interface is created automatically (OpenAPI).

## 2. gRPC service (4 minutes)

**Window:** all commands go into **Terminal 4**. Terminal 2 (the gRPC server) must be running; show its log to the audience, it prints what the server does.

**Say:** The service judges the condition of an article and recommends acceptance, rejection or manual review. The interface is the lecturer's `.proto` (we only added Java options). The rules are in `ConditionRules`. In Terminal 2 you can see what the server does.

**Normal cases, Terminal 4:**
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00458 SH-DRS-017-S DAMAGED" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00459 SH-JKT-088-L AS_NEW tags=false" --console=plain -q
```
**You see:** `ACCEPT (score 0.95`, `REJECT (score 0.95` and `MANUAL_REVIEW (score 0.5, manual review: true)`. The borderline case goes to a person later in the workflow (Sprint 4).

**Terminal 4:**
```
./gradlew :grpc-service:runClient --args="get RET-2026-00457 SH-BLZ-042-M" --console=plain -q
```
**You see:** the same result as before, now fetched.

**Error cases, Terminal 4:**
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00460 SH-SNK-120-42 UNSPECIFIED" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-FAIL-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
**You see:**
- `ERROR [ALREADY_EXISTS] Duplicate request` (the same check again)
- `ERROR [INVALID_ARGUMENT] Invalid data: observed_condition must be set`
- `Service unavailable, attempt 2 of 3` and then the result (simulated outage, the client retries)

**If there is time:** show the deadline. It takes 3 seconds. **Terminal 4:**
```
./gradlew :grpc-service:runClient --args="check RET-SLOW-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
**You see:** `ERROR [DEADLINE_EXCEEDED] Timeout ...`. The client deliberately does not retry here, because the service may have stored the check already.

Note: `RET-FAIL-1` fails only the first time and `RET-SLOW-1` is stored only once. When practising, use `RET-FAIL-2`, `RET-SLOW-2` and so on.

**Fallback, Terminal 4:** `./gradlew :grpc-service:runClient --console=plain -q` without arguments plays all twelve scenarios in a row (about 8 seconds).

## 3. Messaging with RabbitMQ (5 minutes)

**Window:** the producer is started in **Terminal 4**; the result is shown in **Terminal 3** (the consumer log) and in **Browser tab B** (RabbitMQ web page). The broker and Terminal 3 must be running.

**Say:** A producer reports status changes, the consumer updates the status. The producer only knows the exchange, no receivers. This decouples the systems.

**Show the layout, Browser tab B:** menu **Exchanges** → click `returns.status` → section **Bindings**: `return.status.#` goes to `returns.status.management`, only `APPROVED`, `REJECTED`, `ACCEPTED` and `REFUNDED` go to `returns.status.customer-info`. Menu **Queues and Streams** lists the queues `management`, `customer-info` and `dlq` (dead letter queue; the full names start with `returns.status.`).

**Start the producer, Terminal 4** (takes about 5 seconds):
```
./gradlew :messaging-producer:bootRun --console=plain
```
**You see in Terminal 4 (the producer):** `Sent: ... -> return.status.APPROVED (broker confirmed)` for every message.

**In Terminal 3 (the consumer) we show one after the other:**
1. `Status updated: RET-... NEW -> APPROVED` up to `REFUNDED -> CLOSED`: the whole path of a return.
2. `Duplicate ignored: event ... was already processed`: the same event came twice, it is processed only once (recognised by the `eventId`).
3. `INSPECTED -> MANUAL_REVIEW -> ACCEPTED`: the borderline case with manual review.
4. `WARN Management rejects message: Status change not allowed: ... NEW -> INSPECTED`: a forbidden status change is rejected and goes to the dead letter queue.
5. `Message is not valid event JSON` and `Required field is missing: returnId`: the consumer does not understand the message, it also goes to the dead letter queue.
6. **Browser tab B:** menu **Queues and Streams**, `returns.status.dlq` now holds **3 messages** (the display updates every 5 seconds). Click the queue, section **Get messages**, button **Get Message(s)** shows the content and, in the header `x-death`, the reason (`rejected`).
7. `Customer info: customer informed, ...` only for `APPROVED`, `ACCEPTED`, `REFUNDED`: the second receiver only gets its statuses.

**Error case: broker not reachable** (optional, about 1 minute). First stop the consumer in **Terminal 3** with Ctrl+C, otherwise it fills the log with connection messages (it reconnects by itself after the broker is back, which takes up to 10 seconds). Then in **Terminal 4**:
```
docker compose stop
./gradlew :messaging-producer:bootRun --console=plain
docker compose start
```
(Podman: `podman stop shopeasy-rabbitmq` and `podman start shopeasy-rabbitmq`.)

**You see in Terminal 4:** `Sending failed ... attempt 2 of 3`, `attempt 3 of 3` and `ERROR: event not delivered. Broker not reachable after 3 attempts`. Afterwards start the consumer in Terminal 3 again with its command from the table.

**Cannot be shown live:** "No queue accepts the message" (the broker returns it). In the live setup every routing key finds a queue. The test `StatusEventPublisherTest` shows it.

## 3b. Self-service portal (optional, 3 minutes, only if time is left)

**Window:** two more terminals in the folder `sprint2-integration/selfservice-portal` (Terminal 5 and 6), Terminal 4 for the producer, Browser tab C for the portal.

**Say:** A second consumer, written in PHP, receives the same events from its own queue and updates the REST mock. The producer is not changed. The portal shows the result.

**First time only, Terminal 5** (needs internet, a few minutes):
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration/selfservice-portal
composer install
npm install
cp .env.example .env
php artisan key:generate
php artisan migrate --seed
npm run build
```

**Preparation before every run:**
1. **Terminal 1:** stop the REST mock with Ctrl+C and start it again (command from the table), so the statuses are as at the start. Do **not** start the PHP consumer (Terminal 6) before part 3 is finished: it would answer the made-up return ids of part 3 with 404 and fill the dead letter queue.
2. **Terminal 4:** empty the portal queue and the dead letter queue: `docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.portal` and the same with `returns.status.dlq` (or **Browser tab B** → **Queues and Streams** → the queue → **Purge** → **Purge Messages**).
3. **Terminal 5:** `cd` into `selfservice-portal` (see above), then `php artisan migrate:fresh --seed` and `php artisan serve`. Wait for "Server running on [http://127.0.0.1:8000]".
4. **Terminal 6:** open a new window, `cd` into `selfservice-portal`, run `php artisan returns:consume`. Wait for "Waiting for events on queue returns.status.portal".

**Steps:**
1. **Browser tab C** (http://localhost:8000): log in as the customer (`j.mueller@example.com`). **You see:** only `RET-2026-00457`. Open it: status "Neu", empty history. Log out.
2. **Browser tab C:** log in as the clerk (`clerk@shopeasy.test`). **You see:** all six returns and the link "Ereignisprotokoll".
3. **Terminal 4:** send the events: `./gradlew :messaging-producer:bootRun --args='--scenario=portal' --console=plain`.
4. **You see in Terminal 6 (the PHP consumer):** `processed` for the path of `RET-2026-00457` and `00458`, then `duplicate` (the late duplicate of APPROVED), then `rejected` (the unknown `RET-2026-99999`).
5. **Browser tab C:** reload the list: `RET-2026-00457` is "Abgeschlossen", `RET-2026-00458` "Angenommen". The status in the **REST mock** really changed: **Terminal 4** `curl localhost:8080/api/v1/returns/RET-2026-00457`.
6. **Browser tab C:** menu "Ereignisprotokoll": counts "Verarbeitet 13", "Duplikat 1", "Abgelehnt 1" and the reason `Return RET-2026-99999 not found (404)`. The unknown event is also in `returns.status.dlq` (**Browser tab B**).
7. **Conflict:** in **Browser tab C** open `RET-2026-00459` as the clerk. In **Terminal 4** change it behind the page's back with `curl -X PATCH localhost:8080/api/v1/returns/RET-2026-00459 -H 'Content-Type: application/json' -d '{"status":"LABEL_SENT"}'` and again with `{"status":"RECEIVED"}`. Then, in the page, choose "Retourenlabel versendet" and press the button. **You see:** the refusal "Status is RECEIVED, not the expected APPROVED", and the page now shows the real status.

**Say:** The change of the status uses the status the page was showing (`expectedStatus`). The mock refuses it if someone else was faster, so nobody overwrites a newer state.

After 3b stop Terminals 5 and 6 with Ctrl+C.

## 4. Tests and code (1 minute)

**Terminal 4:**
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

- **`./gradlew: No such file or directory`:** you are in the wrong folder. Run the `cd` line from "The one rule" and check with `pwd` and `ls`.
- **`Permission denied: ./gradlew`:** run `chmod +x gradlew` once in `sprint2-integration`.
- **`Cannot connect to the Docker daemon`:** the Docker Desktop app is not running. Start it, wait until it says it is running, try again. (Podman: `podman machine start`.)
- **RabbitMQ web page does not open:** `docker ps` must list `shopeasy-rabbitmq` as `Up`. If not: `docker compose up -d` in Terminal 4.
- **`Connection refused` or a client hangs:** the matching server window is not running. REST commands need Terminal 1, gRPC commands Terminal 2, the producer needs the broker.
- **Port in use** (`Port 8080 was already in use`): `lsof -i :8080` (or 9090, 5672, 8000) shows the old process; stop it (Ctrl+C in its window or `kill <PID>`).
- **`command not found: php`** (or `composer`, `npm`): the portal tools are not installed. Skip part 3b.
- **Build fails right at the start with a Java message:** `java -version` must show 21 or higher.
- **Gradle downloads something:** that needs internet. So run `./gradlew build` once before the meeting.
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
- **Who defined the rules for acceptance and rejection?** They are decisions of Yaraslau Krautsou (01.11.2026) based on the proposal in `TASK-ANALYSIS.md` (section 5.1). No confirmation from the team is needed.
- **What did the AI do?** The code was written with Claude Code and is documented in `AI-USAGE.md`. We have read it and can explain it (this must be true before the meeting).
- **Why does the Java consumer not update the REST mock?** Each component works on its own; the workflow joins them in Sprint 4. The PHP consumer of the portal shows the joined flow. (`TASK-ANALYSIS.md`, section 7, question 4)
- **How does this relate to Sprint 1?** The REST mock matches "Marktplatz-Retoure eingegangen (REST)", the gRPC service "Annahme/Ablehnung entscheiden", the messaging the message steps. We adapt the models in Sprint 4 (section 6 in `TASK-ANALYSIS.md`).

## After the meeting

Press Ctrl+C in Terminals 1, 2, 3 (and 5, 6 if used). Then in Terminal 4: `docker compose down` (Podman: `podman stop shopeasy-rabbitmq`).
