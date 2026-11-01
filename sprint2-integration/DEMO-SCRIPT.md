# Sprint 2 Demo (20 Minutes)

Step by step: how we show the REST mock, the gRPC service and the messaging. Every step has **Say** (what we tell), the **Command** and **You see** (what must appear). **Which window each command goes into** (Terminal 1 to 6, Browser) and **in which folder** is explained in "Where and what to run" below. Read that section first.

The code was written with Claude Code (see `AI-USAGE.md`). Everyone reads their part before and must be able to explain it.

## Time plan

- Intro, 1 minute: Eduard Ede Moser
- 1. REST mock, 3 minutes: Eduard Ede Moser
- 2. gRPC service, 4 minutes: Christian Sticker
- 3. Messaging, 5 minutes: Yaraslau Krautsou
- 3b. Portal (optional), 3 minutes: Yaraslau Krautsou
- 4. Tests and code, 1 minute: Yaraslau Krautsou
- Questions and buffer, 6 minutes (3 if 3b is shown): everyone

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

Needed: Java 21 or newer, Docker Desktop (the app must be **running**) or Podman, and only for the portal (3b) PHP 8.4+, Composer and Node.js 22+. "Preparation" Step 2 checks Java and Docker. For 3b check:
```
php -v
composer -V
node -v
```
You see: three versions. If one says `command not found`, skip part 3b.

Gradle itself is not needed: `./gradlew` downloads it the first time.

### The windows

**Terminal** is the macOS app "Terminal" (Cmd+Space, type `Terminal`, Enter) or the terminal in VS Code. **Browser** is Chrome or Safari.

- **Terminal 1:** REST mock server (port 8080), folder `sprint2-integration`. Parts 1 and 3b.
- **Terminal 2:** gRPC server (port 9090), folder `sprint2-integration`. Part 2.
- **Terminal 3:** Java consumer, folder `sprint2-integration`. Part 3.
- **Terminal 4:** your command window for clients, producer, tests and docker commands, folder `sprint2-integration`. All parts.
- **Terminal 5 (3b):** portal web server (port 8000), folder `sprint2-integration/selfservice-portal`. Not in `sprint2-integration`: there `php artisan serve` fails with `Could not open input file: artisan`.
- **Terminal 6 (3b):** PHP consumer, folder `sprint2-integration/selfservice-portal`.
- **Browser tab A:** http://localhost:8080/swagger-ui.html. Works only while Terminal 1 runs (Step 7).
- **Browser tab B:** http://localhost:15672, user `guest`, password `guest`. Works only while the Docker container runs (Step 4).
- **Browser tab C (3b):** http://localhost:8000. Works only while Terminal 5 runs `php artisan serve` (part 3b).

**These web pages are not started by the browser.** Each one is served by a program that you start first. If the browser says "This site can't be reached" or `ERR_CONNECTION_REFUSED`, that program is not running. See "If something does not work".

**How a terminal behaves:** Terminals 1, 2, 3, 5 and 6 run a server. The command **never finishes**: the window shows the log and is "used up" until you press Ctrl+C. You cannot type another command there, so every server gets its own window. **Terminal 4** is the window where commands finish by themselves (clients, producer, tests) and give the prompt back.

**New window:** in the macOS Terminal press Cmd+T for a new tab. It normally starts in the same folder as the tab before. Check with `pwd`; if it is wrong, run the `cd` line again. The broker (Docker) must run **before** Terminal 3 is started.

### Stop and start again

Press Ctrl+C in each server window, then in Terminal 4 `docker compose down`. Do that after every practice run and start everything **fresh** (the servers keep their data in memory, so a second run on the old servers shows different results).

## Preparation: start everything, step by step (before the meeting, about 10 minutes)

Do the steps **in this order**. Every grey box is one thing to copy into the named window and press Enter. Do not open a web page before the step that starts it.

**Step 1 – Terminal 4: go to the folder.** (Open the app Terminal: Cmd+Space, type `Terminal`, Enter.)
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
pwd
ls
```
You see: `pwd` ends with `/sprint2-integration`; `ls` lists `gradlew` and `compose.yaml`.

**Step 2 – Terminal 4: check Java and Docker.**
```
java -version
docker info --format '{{.ServerVersion}}'
```
You see: Java 21 or higher, then a Docker version number such as `29.5.2`. If Docker prints `Cannot connect to the Docker daemon`: start the app Docker Desktop, wait until it says it is running, run the command again.

**Step 3 – Terminal 4: build and test once** (first time: needs internet, a few minutes; afterwards seconds).
```
./gradlew build
```
You see: the last line is `BUILD SUCCESSFUL`.

**Step 4 – Terminal 4: start RabbitMQ (the broker).** This creates and starts the Docker container `shopeasy-rabbitmq` and returns when it is healthy (about 10 seconds; the first time it downloads the image).
```
docker compose up -d --wait
```
You see: the last line `Container shopeasy-rabbitmq  Healthy`.

With **Podman** instead of Docker (first time only: `podman machine init`):
```
podman machine start
podman run -d --name shopeasy-rabbitmq -p 5672:5672 -p 15672:15672 docker.io/library/rabbitmq:4-management
```
Later runs: `podman machine start`, then `podman start shopeasy-rabbitmq`.

**Step 5 – Terminal 4: check that the RabbitMQ web page answers.**
```
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:15672
```
You see: `200`. If you see `000`, the container is not running: `docker compose ps` must list `shopeasy-rabbitmq` as `Up ... (healthy)`; otherwise repeat Step 4 in the folder `sprint2-integration`.

**Step 6 – Browser tab B: open the RabbitMQ web page.** Copy the address into the browser, or use this command in Terminal 4:
```
open http://localhost:15672
```
You see: the RabbitMQ login. User `guest`, password `guest`. After the login you see "Overview".

**Step 7 – Terminal 1 (new tab with Cmd+T): the REST mock.**
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
./gradlew :rest-mock:bootRun --console=plain
```
You see (after a few seconds): `Started RestMockApplication`. The window stays busy. That is correct: leave it open.

**Step 8 – Browser tab A: open Swagger UI** (works only after Step 7).
```
open http://localhost:8080/swagger-ui.html
```
You see: the Swagger page "ShopEasy Return Reports (Mock)".

**Step 9 – Terminal 2 (new tab with Cmd+T): the gRPC service.**
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
./gradlew :grpc-service:run --console=plain
```
You see: `is running on port 9090`.

**Step 10 – Terminal 3 (new tab with Cmd+T): the Java consumer** (start it after Step 4; the broker must run).
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
./gradlew :messaging-consumer:bootRun --console=plain
```
You see: `Started ConsumerApplication`.

**Step 11 – Terminal 4: empty the dead letter queue.** Every practice run puts 3 messages there; before the meeting it must be empty. The queue exists only after Step 10.
```
docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.dlq
```
(Podman: `podman exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.dlq`.) You see: `Purging queue 'returns.status.dlq' in vhost '/' ...`. In the browser: menu **Queues and Streams** → click `returns.status.dlq` → section **Purge** → **Purge Messages** does the same.

**Step 12 – Terminal 4: final check, then make the font large** (menu View, "Bigger", or Cmd and +).
```
curl -s -o /dev/null -w 'REST mock 8080: %{http_code}\n' http://localhost:8080/api/v1/returns
curl -s -o /dev/null -w 'RabbitMQ 15672: %{http_code}\n' http://localhost:15672
docker compose ps
```
You see: `REST mock 8080: 200`, `RabbitMQ 15672: 200` and the container `Up ... (healthy)`.

Ready when Terminal 1 shows "Started RestMockApplication", Terminal 2 shows "is running on port 9090" and Terminal 3 shows "Started ConsumerApplication". The portal address http://localhost:8000 is **not** part of this preparation: it belongs to part 3b.

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
With Podman, use these two lines instead of `docker compose stop` and `docker compose start`:
```
podman stop shopeasy-rabbitmq
podman start shopeasy-rabbitmq
```

**You see in Terminal 4:** `Sending failed ... attempt 2 of 3`, `attempt 3 of 3` and `ERROR: event not delivered. Broker not reachable after 3 attempts`. Afterwards start the consumer in Terminal 3 again:
```
./gradlew :messaging-consumer:bootRun --console=plain
```

**Cannot be shown live:** "No queue accepts the message" (the broker returns it). In the live setup every routing key finds a queue. The test `StatusEventPublisherTest` shows it.

## 3b. Self-service portal (optional, 3 minutes, only if time is left)

**Window:** two more terminals in the folder `sprint2-integration/selfservice-portal` (Terminal 5 and 6), Terminal 4 for the producer, Browser tab C for the portal. Follow the blocks below in order: the portal address works only after 3b-3.

**Say:** A second consumer, written in PHP, receives the same events from its own queue and updates the REST mock. The producer is not changed. The portal shows the result.

**First time only, Terminal 5** (needs internet, a few minutes). Skip this block if the folder `selfservice-portal` already has the folders `vendor` and `node_modules`:
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration/selfservice-portal
composer install
npm install
cp .env.example .env
php artisan key:generate
touch database/database.sqlite
php artisan migrate --seed
npm run build
```
You see: `Application key set successfully`, the list of migrations with `DONE`, and at the end `built in ...`.

**Preparation before every run.** The broker (Step 4) and the Java consumer must already run.

**3b-1 – Terminal 1:** stop the REST mock with Ctrl+C and start it again, so the statuses are as at the start. Do **not** start the PHP consumer (Terminal 6) before part 3 is finished: it would answer the made-up return ids of part 3 with 404 and fill the dead letter queue.
```
./gradlew :rest-mock:bootRun --console=plain
```

**3b-2 – Terminal 4:** empty the portal queue and the dead letter queue.
```
docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.portal
docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.dlq
```
(In the browser: **Browser tab B** → **Queues and Streams** → the queue → **Purge** → **Purge Messages**.)

**3b-3 – Terminal 5 (new tab with Cmd+T): the portal web server.** Without this program the address http://localhost:8000 does not load.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration/selfservice-portal
php artisan migrate:fresh --seed
php artisan serve
```
You see: `Server running on [http://127.0.0.1:8000]`. The window stays busy: leave it open.

**3b-4 – Terminal 4: check the portal answers, then open it in Browser tab C.**
```
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8000/login
open http://localhost:8000
```
You see: `200`, then the login page of the portal.

**3b-5 – Terminal 6 (new tab with Cmd+T): the PHP consumer.**
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration/selfservice-portal
php artisan returns:consume
```
You see: `Waiting for events on queue returns.status.portal`.

**Steps:**
1. **Browser tab C** (http://localhost:8000): log in as the customer (`j.mueller@example.com`). **You see:** only `RET-2026-00457`. Open it: status "Neu", empty history. Log out.
2. **Browser tab C:** log in as the clerk (`clerk@shopeasy.test`). **You see:** all six returns and the link "Ereignisprotokoll".
3. **Terminal 4:** send the events:
   ```
   ./gradlew :messaging-producer:bootRun --args='--scenario=portal' --console=plain
   ```
4. **You see in Terminal 6 (the PHP consumer):** `processed` for the path of `RET-2026-00457` and `00458`, then `duplicate` (the late duplicate of APPROVED), then `rejected` (the unknown `RET-2026-99999`).
5. **Browser tab C:** reload the list: `RET-2026-00457` is "Abgeschlossen", `RET-2026-00458` "Angenommen". The status in the **REST mock** really changed. **Terminal 4:**
   ```
   curl localhost:8080/api/v1/returns/RET-2026-00457
   ```
6. **Browser tab C:** menu "Ereignisprotokoll": counts "Verarbeitet 13", "Duplikat 1", "Abgelehnt 1" and the reason `Return RET-2026-99999 not found (404)`. The unknown event is also in `returns.status.dlq` (**Browser tab B**).
7. **Conflict:** in **Browser tab C** open `RET-2026-00459` as the clerk. In **Terminal 4** change it behind the page's back, twice:
   ```
   curl -X PATCH localhost:8080/api/v1/returns/RET-2026-00459 -H 'Content-Type: application/json' -d '{"status":"LABEL_SENT"}'
   curl -X PATCH localhost:8080/api/v1/returns/RET-2026-00459 -H 'Content-Type: application/json' -d '{"status":"RECEIVED"}'
   ```
   Then, in the page, choose "Retourenlabel versendet" and press the button. **You see:** the refusal "Status is RECEIVED, not the expected APPROVED", and the page now shows the real status.

**Say:** The change of the status uses the status the page was showing (`expectedStatus`). The mock refuses it if someone else was faster, so nobody overwrites a newer state.

After 3b stop Terminals 5 and 6 with Ctrl+C (the address http://localhost:8000 then no longer loads, as expected).

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
- **http://localhost:15672 does not load** ("This site can't be reached", `ERR_CONNECTION_REFUSED`): the RabbitMQ container is not running. Start it in Terminal 4, in the folder `sprint2-integration`:
  ```
  cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
  docker compose ps
  docker compose up -d --wait
  ```
  `docker compose ps` lists nothing when the container does not exist (after `docker compose down` or on a new computer). `no configuration file provided: not found` means you are in the wrong folder. Wait for `Healthy`, then reload the page. A Java consumer (Terminal 3) that was started before the broker connects by itself within about 10 seconds; if its log still shows connection errors, stop it with Ctrl+C and start it again. With Podman: `podman machine start` and `podman start shopeasy-rabbitmq`.
- **`Could not open input file: artisan`:** you are in `sprint2-integration`. `artisan` exists only in `sprint2-integration/selfservice-portal`. Run `cd selfservice-portal` (or the full `cd` line from part 3b) and repeat the command.
- **http://localhost:8000 does not load:** the portal exists only in part 3b and only while Terminal 5 runs the web server. Start it:
  ```
  cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration/selfservice-portal
  php artisan serve
  ```
  Wait for `Server running on [http://127.0.0.1:8000]`, then reload. If `php: command not found` or the folder `vendor` is missing, do the "First time only" block of part 3b. If port 8000 is taken, `php artisan serve` stops with `Failed to listen on 127.0.0.1:8000 (reason: Address already in use)`: `lsof -i :8000` shows the old process; stop it with Ctrl+C in its window or `kill <PID>`.
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

Press Ctrl+C in Terminals 1, 2, 3 (and 5, 6 if used). Then in Terminal 4:
```
docker compose down
```
(Podman: `podman stop shopeasy-rabbitmq`.) After this, http://localhost:15672 no longer loads until Step 4 is run again.
