# Sprint 2 Demo: Steps

Every step: the window, the code to copy, the expected output. Open a new Terminal tab with Cmd+T (Terminal app: Cmd+Space, `Terminal`). Stop a server with Ctrl+C. Needs Java 21+ and Docker Desktop (running).

- Terminal 1: the command window. Commands there finish.
- Terminals 2, 3, 4, 5, 6: servers. They never finish. Leave them open.

## Start

**Step 1 – Terminal 1:** start RabbitMQ.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
docker compose up -d --wait
```
Expected: last line `Container shopeasy-rabbitmq  Healthy`.

**Step 2 – Terminal 1:** build.
```
./gradlew build
```
Expected: `BUILD SUCCESSFUL`.

**Step 3 – Terminal 2:** REST mock.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
./gradlew :rest-mock:bootRun --console=plain
```
Expected: `Started RestMockApplication`.

**Step 4 – Terminal 3:** gRPC service.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
./gradlew :grpc-service:run --console=plain
```
Expected: `is running on port 9090`.

**Step 5 – Terminal 4:** Java consumer.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration
./gradlew :messaging-consumer:bootRun --console=plain
```
Expected: `Started ConsumerApplication`.

**Step 6 – Terminal 1:** empty the dead letter queue and open the two web pages.
```
docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.dlq
open http://localhost:15672
open http://localhost:8080/swagger-ui.html
```
Expected: `Purging queue 'returns.status.dlq' in vhost '/' ...`. The browser shows the RabbitMQ login (user `guest`, password `guest`) and the page "ShopEasy Return Reports (Mock)".

## 1. REST mock

**Step 7 – Terminal 1:** list returns.
```
./gradlew :rest-mock:runClient --args="list status=NEW marketplace=ZALANDO" --console=plain -q
```
Expected: `-> 200` and `{"data":[{"returnId":"RET-2026-00457", ...`.

**Step 8 – Terminal 1:** one return and its order.
```
./gradlew :rest-mock:runClient --args="get RET-2026-00457" --console=plain -q
./gradlew :rest-mock:runClient --args="order ORD-2026-118932" --console=plain -q
```
Expected: the return, then the order data.

**Step 9 – Terminal 1:** change a status.
```
./gradlew :rest-mock:runClient --args="status RET-2026-00457 APPROVED" --console=plain -q
```
Expected: `"status":"APPROVED"` and a new `updatedAt`.

**Step 10 – Terminal 1:** error cases.
```
./gradlew :rest-mock:runClient --args="get RET-DOES-NOT-EXIST" --console=plain -q
./gradlew :rest-mock:runClient --args="status RET-2026-00457 NONSENSE" --console=plain -q
./gradlew :rest-mock:runClient --args="status RET-2026-00462 APPROVED" --console=plain -q
```
Expected: `-> 404`, `-> 400`, `-> 409`.

## 2. gRPC service

**Step 11 – Terminal 1:** three checks.
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00458 SH-DRS-017-S DAMAGED" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00459 SH-JKT-088-L AS_NEW tags=false" --console=plain -q
```
Expected: `ACCEPT (score 0.95`, `REJECT (score 0.95`, `MANUAL_REVIEW (score 0.5, manual review: true)`. Terminal 3 logs each request.

**Step 12 – Terminal 1:** fetch a stored result.
```
./gradlew :grpc-service:runClient --args="get RET-2026-00457 SH-BLZ-042-M" --console=plain -q
```
Expected: the same result as in Step 11.

**Step 13 – Terminal 1:** error cases.
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00460 SH-SNK-120-42 UNSPECIFIED" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-FAIL-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
Expected: `ERROR [ALREADY_EXISTS] Duplicate request`, then `ERROR [INVALID_ARGUMENT] Invalid data: observed_condition must be set`, then `Service unavailable, attempt 2 of 3` followed by the result. On a second run use `RET-FAIL-2` (`RET-FAIL-1` fails only the first time), or restart Terminal 3.

**Step 14 – Terminal 1:** deadline (takes 3 seconds).
```
./gradlew :grpc-service:runClient --args="check RET-SLOW-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
Expected: `ERROR [DEADLINE_EXCEEDED] Timeout ...`. On a second run use `RET-SLOW-2`.

## 3. Messaging

**Step 15 – Terminal 1:** send the events (about 5 seconds).
```
./gradlew :messaging-producer:bootRun --console=plain
```
Expected in Terminal 1: `Sent: ... -> return.status.APPROVED (broker confirmed)` for every message.
Expected in Terminal 4 (consumer):
- `Status updated: RET-... NEW -> APPROVED` up to `REFUNDED -> CLOSED`
- `Duplicate ignored: event ... was already processed`
- `INSPECTED -> MANUAL_REVIEW -> ACCEPTED`
- `WARN Management rejects message: Status change not allowed: ... NEW -> INSPECTED`
- `Message is not valid event JSON` and `Required field is missing: returnId`
- `Customer info: customer informed, ...` (only for `APPROVED`, `ACCEPTED`, `REFUNDED`)

**Step 16 – Terminal 1:** look at the dead letter queue.
```
docker exec shopeasy-rabbitmq rabbitmqctl list_queues name messages
```
Expected: `returns.status.dlq` has `3` messages (the forbidden change and the two invalid messages). In the browser: RabbitMQ, menu **Queues and Streams**, queue `returns.status.dlq`, section **Get messages**, button **Get Message(s)**.

**Step 17 – Terminal 4:** broker not reachable. Press Ctrl+C in Terminal 4 (consumer), then in Terminal 1:
```
docker compose stop
./gradlew :messaging-producer:bootRun --console=plain
docker compose start
```
Expected in Terminal 1: `Sending failed ... attempt 2 of 3`, `attempt 3 of 3`, `ERROR: event not delivered. Broker not reachable after 3 attempts`.

**Step 18 – Terminal 4:** start the consumer again.
```
./gradlew :messaging-consumer:bootRun --console=plain
```
Expected: `Started ConsumerApplication`.

## 3b. Portal (optional)

**Step 19 – Terminal 5:** first time only. Skip if the folder `selfservice-portal/vendor` exists. Needs internet, a few minutes.
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
Expected: `Application key set successfully`, migrations with `DONE`, and at the end `built in ...`.

**Step 20 – Terminal 2:** restart the REST mock so the statuses are as at the start. Press Ctrl+C, then:
```
./gradlew :rest-mock:bootRun --console=plain
```
Expected: `Started RestMockApplication`.

**Step 21 – Terminal 1:** empty the two queues.
```
docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.portal
docker exec shopeasy-rabbitmq rabbitmqctl purge_queue returns.status.dlq
```
Expected: `Purging queue ... ...` twice.

**Step 22 – Terminal 5:** portal web server. The folder is `selfservice-portal`, not `sprint2-integration`.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration/selfservice-portal
php artisan migrate:fresh --seed
php artisan serve
```
Expected: `Server running on [http://127.0.0.1:8000]`.

**Step 23 – Terminal 6:** PHP consumer.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration/selfservice-portal
php artisan returns:consume
```
Expected: `Waiting for events on queue returns.status.portal`.

**Step 24 – Terminal 1:** open the portal.
```
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8000/login
open http://localhost:8000
```
Expected: `200` and the login page. Log in as the customer `j.mueller@example.com` (the passwords are in `selfservice-portal/database/seeders/DatabaseSeeder.php`): only `RET-2026-00457`, status "Neu". Log out. Log in as the clerk `clerk@shopeasy.test`: six returns and the link "Ereignisprotokoll".

**Step 25 – Terminal 1:** send the events for the portal.
```
./gradlew :messaging-producer:bootRun --args='--scenario=portal' --console=plain
```
Expected in Terminal 6: `processed` for `RET-2026-00457` and `00458`, then `duplicate`, then `rejected` (`RET-2026-99999`).

**Step 26 – Terminal 1:** check the REST mock.
```
curl localhost:8080/api/v1/returns/RET-2026-00457
```
Expected: `"status":"CLOSED"`. In the browser reload the list: `RET-2026-00457` "Abgeschlossen", `RET-2026-00458` "Angenommen". Menu "Ereignisprotokoll": "Verarbeitet 13", "Duplikat 1", "Abgelehnt 1", reason `Return RET-2026-99999 not found (404)`.

**Step 27 – Terminal 1:** conflict. In the browser open `RET-2026-00459` as the clerk (keep the page open), then:
```
curl -X PATCH localhost:8080/api/v1/returns/RET-2026-00459 -H 'Content-Type: application/json' -d '{"status":"LABEL_SENT"}'
curl -X PATCH localhost:8080/api/v1/returns/RET-2026-00459 -H 'Content-Type: application/json' -d '{"status":"RECEIVED"}'
```
Then in the page choose "Retourenlabel versendet" and press the button. Expected: the refusal `Status is RECEIVED, not the expected APPROVED`, and the page shows the real status.

**Step 28 – Terminals 5 and 6:** press Ctrl+C in both. Expected: the portal address no longer loads.

## 4. Tests

**Step 29 – Terminal 1:**
```
./gradlew test --rerun --console=plain
```
Expected: every test with `PASSED`, at the end `BUILD SUCCESSFUL` (74 tests, about 8 seconds).

## Stop

**Step 30:** press Ctrl+C in Terminals 2, 3, 4. Then in Terminal 1:
```
docker compose down
```
Expected: `Container shopeasy-rabbitmq  Removed`. After this http://localhost:15672 does not load until Step 1 is run again.

## If something fails

- `./gradlew: No such file or directory`: wrong folder. Run the `cd` line from Step 1. `pwd` must end with `/sprint2-integration`.
- `Could not open input file: artisan`: wrong folder. `artisan` exists only in `sprint2-integration/selfservice-portal` (Step 22).
- `no configuration file provided: not found`: wrong folder for `docker compose`. Run the `cd` line from Step 1.
- `Cannot connect to the Docker daemon`: start Docker Desktop, wait until it is running, repeat the step.
- Browser "This site can't be reached": the program behind the address is not running. `http://localhost:15672` needs Step 1, `http://localhost:8080` needs Step 3, `http://localhost:8000` needs Step 22.
- `Port ... was already in use`: `lsof -i :8080` (or 9090, 5672, 8000) shows the old process. Stop it with Ctrl+C in its window or `kill <PID>`.
- A second run shows other results (for example `ALREADY_EXISTS`): the servers keep data in memory. Restart Terminals 2, 3 and 4.
