# Sprint 4 Demo: Steps

**How to use:** do the steps in order. Copy **one** code block, paste it into the named window, press **Enter**, wait until the expected output appears, then go to the next step. Never paste two blocks at once.

Open a new Terminal tab with Cmd+T. Stop a server with Ctrl+C. Needs Java 21+, `c8ctl` and Docker (Colima or Docker Desktop, running).

- Terminal 1: the command window. Commands there finish.
- Terminals 2, 3, 4, 5: servers. They never finish. Leave them open.
- Browser: Camunda **Tasklist** (http://localhost:8080/tasklist, login `demo` / `demo`) for the human tasks, **Operate** (http://localhost:8080/operate) for instances and incidents.

The human tasks can be done in Tasklist (claim the task, fill the form, **Complete task**). Each case below also shows the same step as a command (`c8ctl complete ut <KEY>`), which is faster for a live demo. `<KEY>` is the number in the first column of `c8ctl list ut --profile=local`; replace it by the number of the task you need.

## Start

**Step 1 – Terminal 1:** Go to the Sprint 2 folder and start RabbitMQ (the broker).
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration && docker compose up -d --wait
```
Expected: last line `Container shopeasy-rabbitmq  Healthy`.

**Step 2 – Terminal 2:** Open a new tab. Start the REST mock on port **8081** (Camunda uses 8080). Leave the window open.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration && ./gradlew :rest-mock:bootRun --args='--server.port=8081' --console=plain
```
Expected: `Started RestMockApplication`.

**Step 3 – Terminal 3:** Open a new tab. Start the gRPC condition service. Leave the window open.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration && ./gradlew :grpc-service:run --console=plain
```
Expected: `ArticleConditionService is running on port 9090`.

**Step 4 – Terminal 4:** Open a new tab. Start the Java consumer (it creates the queues for the status events). Leave the window open.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration && ./gradlew :messaging-consumer:bootRun --console=plain
```
Expected: `Started ConsumerApplication`.

**Step 5 – Terminal 1:** Start the local Camunda cluster (skip if it already runs: `c8ctl cluster status`). Wait until it is ready.
```
c8ctl cluster start
```
Expected: `All processes are running and healthy`. Operate and Tasklist are now at http://localhost:8080.

**Step 6 – Terminal 1:** Go to the Sprint 4 folder.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint4-workflow
```
Expected: no output.

**Step 7 – Terminal 1:** Deploy the process, the two rule tables and the eight forms.
```
c8ctl deploy process/returns-process.bpmn process/registration-check.dmn process/acceptance-decision.dmn process/forms/*.form --profile=local
```
Expected: `Deployment successful`, a table with `shopeasy-returns` (Process), two Decisions, eight Forms.

**Step 8 – Terminal 5:** Open a new tab. Start the workers. They start the marketplace returns by themselves (the poller). Leave the window open.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint4-workflow && ./gradlew bootRun --console=plain
```
Expected: six lines `Starting job worker: ... with type ...`, then `Started WorkflowApplication`. Within seconds: `Event sent: RET-2026-00457 NEW -> APPROVED` and `... APPROVED -> LABEL_SENT`, and the same for `RET-2026-00458`. Two process instances run now (Operate); both wait for their parcel.

## Case A: marketplace return (REST), accepted

The poller found `RET-2026-00457` (Zalando, blazer, size too small) in the REST mock. The model checked the registration (rule: 6 days after delivery, order and item known, no suspicion), created the label and sent "approved" and "label sent".

**Step 9 – Terminal 1:** The warehouse scans the parcel: send the message "parcel arrived".
```
c8ctl publish msg parcel-arrived --correlationKey RET-2026-00457 --profile=local
```
Expected: `Message 'parcel-arrived' published`. In Terminal 5: `Event sent: RET-2026-00457 LABEL_SENT -> RECEIVED`.

**Step 10 – Terminal 1:** Show the open human tasks. The warehouse task is "Inspect and record condition".
```
c8ctl list ut --profile=local
```
Expected: a task `Inspect and record condition` with a key.

**Step 11 – Terminal 1:** Complete it (in Tasklist: condition `AS_NEW`, tag and original packaging checked). Replace `<KEY>` by the key of that task.
```
c8ctl complete ut <KEY> --variables '{"inspections":[{"sku":"SH-BLZ-042-M","quantity":1,"condition":"AS_NEW","tagsPresent":true,"originalPackaging":true,"inspectorNote":""}]}' --profile=local
```
Expected: `User task ... completed`. Terminal 3: `Check RET-2026-00457 / SH-BLZ-042-M: ACCEPT (0.95)`. Terminal 5: `RECEIVED -> INSPECTED`. The rule `acceptance-decision` says ACCEPT, so refund and stock booking start in parallel.

**Step 12 – Terminal 1:** Show the open tasks again: now "Book stock in ERP". (In Tasklist the form has a link to the ERP page with the data filled in: press **Buchen** there and copy the document number.)
```
c8ctl list ut --profile=local
```
Expected: a task `Book stock in ERP` with a key.

**Step 13 – Terminal 1:** Complete it with the document number from the ERP page. Replace `<KEY>`.
```
c8ctl complete ut <KEY> --variables '{"erpDocumentNumber":"4900001234"}' --profile=local
```
Expected: `User task ... completed`. Terminal 5: `INSPECTED -> ACCEPTED`, `ACCEPTED -> REFUNDED`, `REFUNDED -> CLOSED`. Terminal 4 (consumer): `Customer info: customer informed, return RET-2026-00457 is now ACCEPTED` and `... REFUNDED`.

**Step 14 – Terminal 1:** The return in the store is closed.
```
curl -s http://localhost:8081/api/v1/returns/RET-2026-00457 | grep -o '"status":"[A-Z_]*"'
```
Expected: `"status":"CLOSED"`. In Operate the instance is completed.

## Case B: web shop return after the period, rejected

Order `ORD-2026-119002` was delivered 40 days ago; the period is 14 days.

**Step 15 – Terminal 1:** The web shop reports the return (start message with the data).
```
c8ctl publish msg webshop-return-registered --variables '{"orderId":"ORD-2026-119002","customerEmail":"k.hoffmann@example.com","items":[{"sku":"SH-JKT-088-L","quantity":1}],"reason":"CHANGED_MIND"}' --profile=local
```
Expected: `Message 'webshop-return-registered' published`. Terminal 5: `Event sent: RET-2026-00463 NEW -> REJECTED`, `REJECTED -> CLOSED`. Terminal 4: `Customer info: customer informed, return RET-2026-00463 is now REJECTED`. In Operate the instance ended at "Return rejected" (rule `registration-check`: `NOT_ELIGIBLE`, 40 days).

## Case C: e-mail return, unclear, two manual reviews, accepted

**Step 16 – Terminal 1:** An e-mail arrives (simulated: the start message carries the mail text).
```
c8ctl publish msg email-return-received --variables '{"emailFrom":"l.neumann@example.com","emailSubject":"Retoure Bestellung ORD-2026-119003","emailBody":"Hallo, ich moechte beide Artikel zuruecksenden. Gruss L. Neumann"}' --profile=local
```
Expected: `Message 'email-return-received' published`. The task "Capture return from e-mail" appears (the step that the AI agent takes over in Sprint 5).

**Step 17 – Terminal 1:** The clerk reads the mail and types in order, e-mail and items. The mail names no reason: the field stays empty. Take the key of the task `Capture return from e-mail` from `c8ctl list ut --profile=local` and replace `<KEY>`.
```
c8ctl complete ut <KEY> --variables '{"orderId":"ORD-2026-119003","customerEmail":"l.neumann@example.com","items":[{"sku":"SH-SNK-120-42","quantity":1},{"sku":"SH-PNT-031-32","quantity":1}]}' --profile=local
```
Expected: Terminal 5: `Event sent: RET-2026-00464 NEW -> IN_REVIEW`. The rule says `UNCLEAR` (reason missing), so the task "Review registration" appears.

**Step 18 – Terminal 1:** The clerk phones the customer and lets the return continue. Take the key of `Review registration`.
```
c8ctl complete ut <KEY> --variables '{"rejectRegistration":false,"reviewNote":"Customer confirmed by phone"}' --profile=local
```
Expected: Terminal 5: `IN_REVIEW -> APPROVED`, `APPROVED -> LABEL_SENT`.

**Step 19 – Terminal 1:** The parcel arrives.
```
c8ctl publish msg parcel-arrived --correlationKey RET-2026-00464 --profile=local
```
Expected: `Message 'parcel-arrived' published`.

**Step 20 – Terminal 1:** The warehouse finds minor wear at one item. Take the key of `Inspect and record condition` (the one of this return: the newest).
```
c8ctl complete ut <KEY> --variables '{"inspections":[{"sku":"SH-SNK-120-42","quantity":1,"condition":"AS_NEW","tagsPresent":true,"originalPackaging":true,"inspectorNote":""},{"sku":"SH-PNT-031-32","quantity":1,"condition":"MINOR_WEAR","tagsPresent":true,"originalPackaging":true,"inspectorNote":"Slight wear at the hem"}]}' --profile=local
```
Expected: Terminal 3: `... SH-SNK-120-42: ACCEPT (0.95)` and `... SH-PNT-031-32: MANUAL_REVIEW (0.55)`. Terminal 5: `INSPECTED -> MANUAL_REVIEW`. The task "Review borderline case" appears.

**Step 21 – Terminal 1:** The clerk accepts and documents the reason. Take the key of `Review borderline case`.
```
c8ctl complete ut <KEY> --variables '{"accepted":true,"justification":"Wear is minimal, accept"}' --profile=local
```
Expected: Terminal 5: `MANUAL_REVIEW -> ACCEPTED`, `ACCEPTED -> REFUNDED`. The task "Book stock in ERP" appears.

**Step 22 – Terminal 1:** Book the stock (ERP page) and finish. Take the key of `Book stock in ERP`.
```
c8ctl complete ut <KEY> --variables '{"erpDocumentNumber":"4900001235"}' --profile=local
```
Expected: Terminal 5: `REFUNDED -> CLOSED`. `RET-2026-00464` is closed.

## Case D: the gRPC service is not reachable

**Step 23 – Terminal 1:** A normal web shop return on order `ORD-2026-119001`.
```
c8ctl publish msg webshop-return-registered --variables '{"orderId":"ORD-2026-119001","customerEmail":"j.mueller@example.com","items":[{"sku":"SH-BLZ-042-M","quantity":1}],"reason":"SIZE_TOO_SMALL"}' --profile=local
```
Expected: Terminal 5: `Event sent: RET-2026-00465 NEW -> APPROVED`, `APPROVED -> LABEL_SENT`.

**Step 24 – Terminal 1:** The parcel arrives.
```
c8ctl publish msg parcel-arrived --correlationKey RET-2026-00465 --profile=local
```
Expected: `Message 'parcel-arrived' published`.

**Step 25 – Terminal 3:** Stop the gRPC service: click into the window and press **Ctrl+C**. Expected: the prompt returns.

**Step 26 – Terminal 1:** The warehouse records the condition. Take the key of `Inspect and record condition` (the newest).
```
c8ctl complete ut <KEY> --variables '{"inspections":[{"sku":"SH-BLZ-042-M","quantity":1,"condition":"AS_NEW","tagsPresent":true,"originalPackaging":true,"inspectorNote":""}]}' --profile=local
```
Expected: Terminal 5 shows `Service unavailable, attempt 2 of 3`, `attempt 3 of 3` (the Sprint 2 client), then the engine repeats the job twice (every 5 seconds). After about 15 seconds the job has no retries left.

**Step 27 – Terminal 1:** Show the incident (in Operate: the instance has an incident at "Check condition (gRPC)").
```
c8ctl list inc --profile=local
```
Expected: one incident, type `JOB_NO_RETRIES`, message `JobError: Service not ...`.

**Step 28 – Terminal 3:** Start the gRPC service again.
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration && ./gradlew :grpc-service:run --console=plain
```
Expected: `ArticleConditionService is running on port 9090`.

**Step 29 – Terminal 1:** Give the job new retries and resolve the incident (in Operate this is the button **Retry**). Replace `<JOB>` by the job key (Operate, or `c8ctl list jobs --profile=local`) and `<INCIDENT>` by the key from step 27.
```
c8ctl update job <JOB> --retries 3 --profile=local && c8ctl resolve inc <INCIDENT> --profile=local
```
Expected: `Job ... updated`, `Incident ... resolved`. Terminal 5: `RECEIVED -> INSPECTED`; the case continues with the stock booking and the refund (finish it as in steps 12 and 13).

## Case E: shipping API and payment provider (simulated)

The label and the payment are simulated; the e-mail address of the customer switches the behaviour (like `RET-FAIL` in Sprint 2): `label-down@example.com` (shipping API down), `psp-timeout@example.com` (payment provider does not answer), `no-iban@example.com` and `card-expired@example.com` (refund fails at once), `suspect@example.com` (high fraud score), `ai-down@example.com` (AI assessment down).

**Step 30 – Terminal 1:** A return with the address `label-down@example.com`.
```
c8ctl publish msg webshop-return-registered --variables '{"orderId":"ORD-2026-119001","customerEmail":"label-down@example.com","items":[{"sku":"SH-BLZ-042-M","quantity":1}],"reason":"SIZE_TOO_SMALL"}' --profile=local
```
Expected: after about 10 seconds (three attempts) the task "Create label manually" appears: the error `LABEL_SERVICE_UNAVAILABLE` ran into the boundary event and a clerk takes over.

**Step 31 – Terminal 1:** The clerk enters the label URL. Take the key of `Create label manually`.
```
c8ctl complete ut <KEY> --variables '{"labelUrl":"https://labels.example.com/manual-RET-2026-00466.pdf"}' --profile=local
```
Expected: Terminal 5: `Event sent: RET-2026-00466 NEW -> APPROVED`, `APPROVED -> LABEL_SENT`: the case continues as normal.

**Step 32 – Terminal 1:** A return with the address `psp-timeout@example.com`.
```
c8ctl publish msg webshop-return-registered --variables '{"orderId":"ORD-2026-119001","customerEmail":"psp-timeout@example.com","items":[{"sku":"SH-BLZ-042-M","quantity":1}],"reason":"DEFECTIVE"}' --profile=local
```
Expected: Terminal 5: `Event sent: RET-2026-00467 NEW -> APPROVED`, `APPROVED -> LABEL_SENT`.

**Step 33 – Terminal 1:** The parcel arrives.
```
c8ctl publish msg parcel-arrived --correlationKey RET-2026-00467 --profile=local
```
Expected: `Message 'parcel-arrived' published`.

**Step 34 – Terminal 1:** The warehouse records `AS_NEW`. Take the key of `Inspect and record condition` (the newest).
```
c8ctl complete ut <KEY> --variables '{"inspections":[{"sku":"SH-BLZ-042-M","quantity":1,"condition":"AS_NEW","tagsPresent":true,"originalPackaging":true,"inspectorNote":""}]}' --profile=local
```
Expected: the rule says ACCEPT; stock booking and refund start in parallel. After about 10 seconds (three attempts of the refund) the task "Resolve refund manually" appears next to "Book stock in ERP": the error `REFUND_FAILED` ran into the boundary event.

**Step 35 – Terminal 1:** The clerk clarifies with the payment provider and enters the reference. Take the key of `Resolve refund manually`.
```
c8ctl complete ut <KEY> --variables '{"refundReference":"MANUAL-7"}' --profile=local
```
Expected: `User task ... completed`.

**Step 36 – Terminal 1:** Book the stock (ERP page). Take the key of `Book stock in ERP`.
```
c8ctl complete ut <KEY> --variables '{"erpDocumentNumber":"4900001237"}' --profile=local
```
Expected: Terminal 5: `INSPECTED -> ACCEPTED`, `ACCEPTED -> REFUNDED`, `REFUNDED -> CLOSED`: the parallel join waited for both branches.

## Case F: marketplace return after inspection rejected

`RET-2026-00458` (Zalando, dress and belt) is still waiting for its parcel from step 8.

**Step 37 – Terminal 1:** The parcel arrives.
```
c8ctl publish msg parcel-arrived --correlationKey RET-2026-00458 --profile=local
```
Expected: `Message 'parcel-arrived' published`.

**Step 38 – Terminal 1:** The dress is damaged. Take the key of `Inspect and record condition`.
```
c8ctl complete ut <KEY> --variables '{"inspections":[{"sku":"SH-DRS-017-S","quantity":1,"condition":"DAMAGED","tagsPresent":true,"originalPackaging":true,"inspectorNote":"Tear at the seam"},{"sku":"SH-BLT-003-U","quantity":1,"condition":"AS_NEW","tagsPresent":true,"originalPackaging":true,"inspectorNote":""}]}' --profile=local
```
Expected: Terminal 3: `... SH-DRS-017-S: REJECT (0.95)`. The task "Decide further use (return / donate / dispose)" appears.

**Step 39 – Terminal 1:** The warehouse decides. Take the key of `Decide further use`.
```
c8ctl complete ut <KEY> --variables '{"disposition":"DONATE"}' --profile=local
```
Expected: Terminal 5: `INSPECTED -> REJECTED`, `REJECTED -> CLOSED`. No refund, no stock booking.

## Stop

**Step 40 – Terminals 2 to 5:** Press **Ctrl+C** in each. **Terminal 1:** stop RabbitMQ (and the cluster with `c8ctl cluster stop` if you do not need it).
```
cd ~/Documents/HKA/DVG_2026-WS/shopeasy-retouren/sprint2-integration && docker compose down
```
Expected: `Container shopeasy-rabbitmq  Removed`.

## If something does not work

| Symptom | Cause and fix |
|---|---|
| Workers log `Poll skipped: ... not reachable` | the REST mock is not running on 8081 (step 2) |
| `Event sent` never appears, incident at "Send label & status" with `No queue accepted the message` | `messaging-consumer` is not running (step 4): it creates the queues. Start it, then press **Retry** in Operate |
| Nothing happens after the start message | the model is not deployed (step 7) or the workers are not running (step 8) |
| `Connection refused ... 26500` in Terminal 5 | the Camunda cluster is not running (step 5) |
| A return of a former run is not new any more | the REST mock keeps its data in memory: restart it (Ctrl+C in Terminal 2, run step 2 again) and start the instances again |
