# Sprint 2 Demo: Steps

**How to use:** do the steps in order. Copy **one** code block, paste it into the named window, press **Enter**, wait until the expected output appears, then go to the next step. Never paste two blocks at once: the second command may be lost or run together with the first.

Open a new Terminal tab with Cmd+T (Terminal app: Cmd+Space, `Terminal`). Stop a server with Ctrl+C. Needs Java 21+ and Docker Desktop (running).

- Terminal 1: the command window. Commands there finish.
- Terminals 2, 3, 4, 5, 6: servers. They never finish. Leave them open.

## Start

**Step 1 – Terminal 1:** Go to the project folder.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
```
Expected: no output. `pwd` must end with `/sprint2-integration`.

**Step 2 – Terminal 1:** Start RabbitMQ (the broker) and wait until it is ready.
```
docker compose up -d --wait
```
Expected: last line `Container shopeasy-rabbitmq  Healthy`.

**Step 3 – Terminal 1:** Build and test once. First time: needs internet, a few minutes.
```
./gradlew build
```
Expected: `BUILD SUCCESSFUL`.

**Step 4 – Terminal 2:** Open a new Terminal tab (Cmd+T) and go to the project folder.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
```
Expected: no output.

**Step 5 – Terminal 2:** Start the REST mock. The command never finishes: leave the window open.
```
./gradlew :rest-mock:bootRun --console=plain
```
Expected: `Started RestMockApplication`.

**Step 6 – Terminal 3:** Open a new tab (Cmd+T) and go to the project folder.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
```
Expected: no output.

**Step 7 – Terminal 3:** Start the gRPC service. Leave the window open.
```
./gradlew :grpc-service:run --console=plain
```
Expected: `is running on port 9090`.

**Step 8 – Terminal 4:** Open a new tab (Cmd+T) and go to the project folder.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
```
Expected: no output.

**Step 9 – Terminal 4:** Start the Java consumer. Leave the window open.
```
./gradlew :messaging-consumer:bootRun --console=plain
```
Expected: `Started ConsumerApplication`.

**Step 10 – Terminal 1:** Empty the dead letter queue (practice runs fill it).
```
docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.dlq
```
Expected: `Purging queue 'returns.status.dlq' in vhost '/' ...`.

**Step 11 – Terminal 1:** Open the RabbitMQ web page in the browser.
```
open http://localhost:15672
```
Expected: the RabbitMQ login. User `guest`, password `guest`.

**Step 12 – Terminal 1:** Open Swagger UI of the REST mock in the browser.
```
open http://localhost:8080/swagger-ui.html
```
Expected: the page "ShopEasy Return Reports (Mock)".

## 1. REST mock

**Step 13 – Terminal 1:** List the returns of one marketplace.
```
./gradlew :rest-mock:runClient --args="list status=NEW marketplace=ZALANDO" --console=plain -q
```
Expected: `-> 200` and `{"data":[{"returnId":"RET-2026-00457", ...`.

**Step 14 – Terminal 1:** Fetch one return.
```
./gradlew :rest-mock:runClient --args="get RET-2026-00457" --console=plain -q
```
Expected: `-> 200` and the return.

**Step 15 – Terminal 1:** Fetch its order.
```
./gradlew :rest-mock:runClient --args="order ORD-2026-118932" --console=plain -q
```
Expected: `-> 200` and the order data.

**Step 16 – Terminal 1:** Change the status of the return.
```
./gradlew :rest-mock:runClient --args="status RET-2026-00457 APPROVED" --console=plain -q
```
Expected: `"status":"APPROVED"` and a new `updatedAt`.

**Step 17 – Terminal 1:** Error case: unknown return.
```
./gradlew :rest-mock:runClient --args="get RET-DOES-NOT-EXIST" --console=plain -q
```
Expected: `-> 404`.

**Step 18 – Terminal 1:** Error case: invalid status.
```
./gradlew :rest-mock:runClient --args="status RET-2026-00457 NONSENSE" --console=plain -q
```
Expected: `-> 400`.

**Step 19 – Terminal 1:** Error case: change that is not allowed (the return was rejected).
```
./gradlew :rest-mock:runClient --args="status RET-2026-00462 APPROVED" --console=plain -q
```
Expected: `-> 409`.

## 2. gRPC service

**Step 20 – Terminal 1:** Check an article in good condition.
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
```
Expected: `ACCEPT (score 0.95`. Terminal 3 logs the request.

**Step 21 – Terminal 1:** Check a damaged article.
```
./gradlew :grpc-service:runClient --args="check RET-2026-00458 SH-DRS-017-S DAMAGED" --console=plain -q
```
Expected: `REJECT (score 0.95`.

**Step 22 – Terminal 1:** Check a borderline case (tags removed).
```
./gradlew :grpc-service:runClient --args="check RET-2026-00459 SH-JKT-088-L AS_NEW tags=false" --console=plain -q
```
Expected: `MANUAL_REVIEW (score 0.5, manual review: true)`.

**Step 23 – Terminal 1:** Fetch a stored result.
```
./gradlew :grpc-service:runClient --args="get RET-2026-00457 SH-BLZ-042-M" --console=plain -q
```
Expected: the same result as for the first check.

**Step 24 – Terminal 1:** Error case: the same check again.
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
```
Expected: `ERROR [ALREADY_EXISTS] Duplicate request`.

**Step 25 – Terminal 1:** Error case: condition missing.
```
./gradlew :grpc-service:runClient --args="check RET-2026-00460 SH-SNK-120-42 UNSPECIFIED" --console=plain -q
```
Expected: `ERROR [INVALID_ARGUMENT] Invalid data: observed_condition must be set`.

**Step 26 – Terminal 1:** Error case: simulated outage, the client retries. `RET-FAIL-1` fails only the first time; on a second run use `RET-FAIL-2`.
```
./gradlew :grpc-service:runClient --args="check RET-FAIL-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
Expected: `Service unavailable, attempt 2 of 3`, then the result.

**Step 27 – Terminal 1:** Error case: deadline (takes 3 seconds). On a second run use `RET-SLOW-2`.
```
./gradlew :grpc-service:runClient --args="check RET-SLOW-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
Expected: `ERROR [DEADLINE_EXCEEDED] Timeout ...`.

## 3. Messaging

**Step 28 – Terminal 1:** Send the status events (about 5 seconds).
```
./gradlew :messaging-producer:bootRun --console=plain
```
Expected: in Terminal 1 `Sent: ... -> return.status.APPROVED (broker confirmed)` for every message. In Terminal 4 (consumer): `Status updated: RET-... NEW -> APPROVED` up to `REFUNDED -> CLOSED`, `Duplicate ignored: event ... was already processed`, `WARN Management rejects message: Status change not allowed: ... NEW -> INSPECTED`, `Message is not valid event JSON`, `Required field is missing: returnId`, `Customer info: customer informed, ...`.

**Step 29 – Terminal 1:** Look at the queues.
```
docker exec shopeasy-rabbitmq rabbitmqctl list_queues name messages
```
Expected: `returns.status.dlq` has `3` messages (the forbidden change and the two invalid messages). In the browser: RabbitMQ, menu **Queues and Streams**, queue `returns.status.dlq`, section **Get messages**, button **Get Message(s)**.

**Step 30 – Terminal 4:** Error case, broker not reachable. First stop the consumer: press Ctrl+C.
Expected: the prompt comes back.

**Step 31 – Terminal 1:** Stop the broker.
```
docker compose stop
```
Expected: `Container shopeasy-rabbitmq  Stopped`.

**Step 32 – Terminal 1:** Send events while the broker is down.
```
./gradlew :messaging-producer:bootRun --console=plain
```
Expected: `Sending failed ... attempt 2 of 3`, `attempt 3 of 3`, `ERROR: event not delivered. Broker not reachable after 3 attempts`.

**Step 33 – Terminal 1:** Start the broker again and wait until it is ready.
```
docker compose up -d --wait
```
Expected: last line `Container shopeasy-rabbitmq  Healthy`.

**Step 34 – Terminal 4:** Start the consumer again.
```
./gradlew :messaging-consumer:bootRun --console=plain
```
Expected: `Started ConsumerApplication`.

## 3b. Portal (optional)

**Step 35 – Terminal 1:** Check that the broker runs.
```
docker compose ps
```
Expected: `shopeasy-rabbitmq` with `Up ... (healthy)`. If not: run `docker compose up -d --wait`.

**Step 36 – Terminal 5:** Open a new tab (Cmd+T) and go to the portal folder. It is `selfservice-portal`, not `sprint2-integration`.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration/selfservice-portal
```
Expected: no output.

**Step 37 – Terminal 5:** First time only (skip these setup steps if the folder `vendor` exists): install the PHP libraries. Needs internet.
```
composer install
```
Expected: the list of packages, no error.

**Step 38 – Terminal 5:** First time only: install the JavaScript libraries.
```
npm install
```
Expected: `found 0 vulnerabilities`.

**Step 39 – Terminal 5:** First time only: create the settings file (an existing `.env` stays).
```
cp -n .env.example .env
```
Expected: no output.

**Step 40 – Terminal 5:** First time only: create the application key.
```
php artisan key:generate
```
Expected: `Application key set successfully.`

**Step 41 – Terminal 5:** First time only: create the empty database file.
```
touch database/database.sqlite
```
Expected: no output.

**Step 42 – Terminal 5:** First time only: build the web pages.
```
npm run build
```
Expected: `built in ...`.

**Step 43 – Terminal 2:** Restart the REST mock so the statuses are as at the start. Press Ctrl+C.
Expected: the prompt comes back.

**Step 44 – Terminal 2:** Start the REST mock again.
```
./gradlew :rest-mock:bootRun --console=plain
```
Expected: `Started RestMockApplication`.

**Step 45 – Terminal 1:** Empty the portal queue.
```
docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.portal
```
Expected: `Purging queue 'returns.status.portal' in vhost '/' ...`. On a new broker the queue does not exist yet and you see `Object (queue, stream, exchange, etc) was not found`: that is fine, go on.

**Step 46 – Terminal 1:** Empty the dead letter queue.
```
docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.dlq
```
Expected: `Purging queue 'returns.status.dlq' in vhost '/' ...`.

**Step 47 – Terminal 5:** Create the portal database with the demo users (in the folder `selfservice-portal`).
```
php artisan migrate:fresh --seed
```
Expected: the migrations with `DONE`, last line `Seeding database.`

**Step 48 – Terminal 5:** Start the portal web server. Leave the window open.
```
php artisan serve
```
Expected: `Server running on [http://127.0.0.1:8000]`.

**Step 49 – Terminal 6:** Open a new tab (Cmd+T) and go to the portal folder.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration/selfservice-portal
```
Expected: no output.

**Step 50 – Terminal 6:** Start the PHP consumer. Leave the window open.
```
php artisan returns:consume
```
Expected: `Waiting for events on queue returns.status.portal`.

**Step 51 – Terminal 1:** Check that the portal answers.
```
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8000/login
```
Expected: `200`.

**Step 52 – Terminal 1:** Open the portal in the browser.
```
open http://localhost:8000
```
Expected: the login page.

**Step 53 – Browser:** Log in as the customer `j.mueller@example.com` (passwords: `selfservice-portal/database/seeders/DatabaseSeeder.php`). Then log out.
Expected: only `RET-2026-00457`, status "Neu".

**Step 54 – Browser:** Log in as the clerk `clerk@shopeasy.test`.
Expected: six returns and the link "Ereignisprotokoll".

**Step 55 – Terminal 1:** Send the events for the portal.
```
./gradlew :messaging-producer:bootRun --args='--scenario=portal' --console=plain
```
Expected: in Terminal 6: `processed` for `RET-2026-00457` and `00458`, then `duplicate`, then `rejected` (`RET-2026-99999`).

**Step 56 – Terminal 1:** Check the status in the REST mock.
```
curl localhost:8080/api/v1/returns/RET-2026-00457
```
Expected: `"status":"CLOSED"`.

**Step 57 – Browser:** Reload the list. Open the menu "Ereignisprotokoll".
Expected: `RET-2026-00457` "Abgeschlossen", `RET-2026-00458` "Angenommen". Counts "Verarbeitet 13", "Duplikat 1", "Abgelehnt 1", reason `Return RET-2026-99999 not found (404)`.

**Step 58 – Browser:** Conflict, part 1: open `RET-2026-00459` as the clerk and keep the page open.
Expected: the return with status "Genehmigt".

**Step 59 – Terminal 1:** Conflict, part 2: change the status behind the page's back.
```
curl -X PATCH localhost:8080/api/v1/returns/RET-2026-00459 -H 'Content-Type: application/json' -d '{"status":"LABEL_SENT"}'
```
Expected: the return with `"status":"LABEL_SENT"`.

**Step 60 – Terminal 1:** Conflict, part 3: change it again.
```
curl -X PATCH localhost:8080/api/v1/returns/RET-2026-00459 -H 'Content-Type: application/json' -d '{"status":"RECEIVED"}'
```
Expected: the return with `"status":"RECEIVED"`.

**Step 61 – Browser:** Conflict, part 4: in the open page choose "Retourenlabel versendet" and press the button.
Expected: the refusal `Status is RECEIVED, not the expected APPROVED`. The page shows the real status.

**Step 62 – Terminal 5:** Stop the portal web server: press Ctrl+C.
Expected: the prompt comes back. The address http://localhost:8000 no longer loads.

**Step 63 – Terminal 6:** Stop the PHP consumer: press Ctrl+C.
Expected: the prompt comes back.

## 4. Tests

**Step 64 – Terminal 1:** Run all tests.
```
./gradlew test --rerun --console=plain
```
Expected: every test with `PASSED`, at the end `BUILD SUCCESSFUL` (74 tests, about 10 seconds).

## Stop

**Step 65 – Terminals 2, 3, 4:** Stop the three servers: press Ctrl+C in each window.
Expected: the prompts come back.

**Step 66 – Terminal 1:** Remove the RabbitMQ container.
```
docker compose down
```
Expected: `Container shopeasy-rabbitmq  Removed`. After this http://localhost:15672 does not load until the broker is started again (Step 2).

## If something fails

- `./gradlew: No such file or directory`: wrong folder. Run the `cd` command from Step 1. `pwd` must end with `/sprint2-integration`.
- `Could not open input file: artisan`: wrong folder. `artisan` exists only in `sprint2-integration/selfservice-portal`.
- `no configuration file provided: not found`: wrong folder for `docker compose`. Run the `cd` command from Step 1.
- `Cannot connect to the Docker daemon`: start Docker Desktop, wait until it is running, repeat the step.
- `Cannot connect to RabbitMQ at localhost:5672`, `container ... is not running`, or `Connection refused` in Terminal 4: the broker is stopped. In `sprint2-integration` run `docker compose up -d --wait`.
- Two commands ran as one line (for example `docker compose startdocker exec ...`) or a line was skipped: you pasted more than one block at once. Paste one block, press Enter, wait.
- Browser "This site can't be reached": the program behind the address is not running. `http://localhost:15672` needs Step 2, `http://localhost:8080` needs Step 5, `http://localhost:8000` needs the step "Start the portal web server".
- `Port ... was already in use`: `lsof -i :8080` (or 9090, 5672, 8000) shows the old process. Stop it with Ctrl+C in its window or `kill <PID>`.
- A second run shows other results (for example `ALREADY_EXISTS`): the servers keep data in memory. Restart Terminals 2, 3 and 4.
