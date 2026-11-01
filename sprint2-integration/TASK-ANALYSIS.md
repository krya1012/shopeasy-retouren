# Sprint 2: Analysis of the Task

Sources: `Aufgabe DvG WS26_Sprint 2.pdf` (Prof. Heberle) and `article_condition_service.proto` (now in `grpc-service/src/main/proto/`, extended there by two Java options only). The implementation is described in the [README](README.md). Deadline: **3.11.2026, 11:30 in ILIAS**.

Everything marked "proposal" was proposed by the AI and is decided by Yaraslau Krautsou (01.11.2026). No confirmation from the team is needed (checklist in `AI-USAGE.md`). Names of process steps from Sprint 1 are in German, as in the models.

## 1. What we build

Three small programs that will be used in the process later. Each gets a **client** to try it out. The programming language is free.

| No. | Program | What it does | Link to the target process |
|---|---|---|---|
| 1 | **REST mock** for return reports | Delivers return reports like they arrive from Amazon or Zalando | "Marktplatz-Retoure eingegangen (REST)" (`s_mkt`) |
| 2 | **gRPC service** for the condition check | Judges the condition of an article and recommends accept, reject or manual review | "Annahme/Ablehnung entscheiden" (`t_decide`), "Zustand im WMS buchen" (`t_gr_rec`) |
| 3 | **Messaging** for status updates | A producer sends status updates, a consumer receives them and updates the status | message steps "Label & Status „genehmigt“ senden" (`t_notify`), "Wareneingang bestätigen" (`t_notify_in`), "Kund:in über Ergebnis informieren" (`t_end_notify`) |

## 2. Requirements per program

### 2.1 REST mock
- Fetch all return reports, with a filter (e.g. by status or marketplace).
- Fetch one report by `returnId`.
- Fetch the data of an order by `orderId`.
- Change the status of a report.
- The format of the report is in the task: `returnId`, `orderId`, `marketplace` (e.g. `ZALANDO`), `status` (e.g. `NEW`), `reason` (code, e.g. `SIZE_TOO_SMALL`), `reasonText`, `requestedAt`, `updatedAt`, `items` (with `sku`, `productName`, `quantity`, `unitPrice`, `currency`), `customer` and `returnLabelUrl`. In the example the list is inside a field `data`.

### 2.2 gRPC service
- The starting point is the provided `.proto`. Changes only after talking to the lecturer.
- Three calls: `CheckCondition` (one article), `CheckConditionBatch` (several articles) and `GetCheckResult` (fetch an earlier result).
- Answer: `decision` (`ACCEPT`, `REJECT`, `MANUAL_REVIEW`), `confidence_score`, `reason`, `requires_manual_review`, `checked_at`.
- The service **simulates** the judgement, allows **different process scenarios** and handles **errors**.
- Borderline cases (`MANUAL_REVIEW`) are decided by a person later in the workflow (Sprint 4).

### 2.3 Messaging
- A message broker is required: ActiveMQ or RabbitMQ, both also with Docker.
- We decide ourselves which status changes make sense and which topics or queues we need.
- Producer: creates status updates for returns. Consumer: picks them up and updates the status of a return.

## 3. How it is graded

From the slide "Aspects of messaging and gRPC":

| Area | What is graded |
|---|---|
| Messaging | clean definition of the events, correct producer/consumer logic, decoupling of the systems, structure of the code |
| Messaging, errors | a message cannot be delivered, the consumer does not understand the message, **duplicate events** (red on the slide) |
| gRPC | clean and sensible interface description, structure of the code |
| gRPC, errors | fitting errors in the service (e.g. duplicate request, wrong data), error handling in the client |

**Remarkable:** the slide talks about "payment" and "invoices", but our task is about returns. **Answer of the lecturer (question 1): this is an old slide, we ignore it.** We still implement the error cases (not delivered, not understood, duplicate events, duplicate request, wrong data), because they belong to messaging and gRPC in general.

**Other rules from the task:**
- Apply standards and best practices of Prof. Zimmermann.
- AI is allowed. We must understand what we hand in; the lecturer asks questions about it.
- The use of AI must be documented (`AI-USAGE.md`).

**From our Definition of Done** (`Definition of Done.pdf`): everyone agrees with the final state, the results are accessible for the team and the lecturer, old versions are deleted from the repo, it is clear who presents what, and we use AI only as far as we understand it.

## 4. The `.proto` in detail

**Input** (`ConditionCheckRequest`): `return_id`, `sku`, `observed_condition`, `image_urls`, `tags_present`, `original_packaging`, `inspector_note`, `inspected_at`.

**Conditions** (`ObservedCondition`): `AS_NEW`, `MINOR_WEAR`, `VISIBLY_WORN`, `DAMAGED`, `MISSING_TAGS`, `MISSING_PARTS` (and `OBSERVED_CONDITION_UNSPECIFIED`).

**Result** (`Decision`): `ACCEPT`, `REJECT`, `MANUAL_REVIEW` (and `DECISION_UNSPECIFIED`).

**What the file leaves open:**
- Which condition leads to which decision. We define that (proposal in 5.1).
- Which errors exist. gRPC has status codes for this (proposal in 5.2).
- `CheckConditionBatch` has no field for an error of single articles.
- There is no `package` and there are no language options. Both can be needed, depending on the language.

**Mapping to our condition scale from Sprint 1** (for the KPIs K6–K8): Sprint 1 only knows "neuwertig" (as new), "beschädigt" (damaged) and "getragen" (worn). Proposal: `AS_NEW` = as new, `DAMAGED` and `MISSING_PARTS` = damaged, `VISIBLY_WORN` and `MINOR_WEAR` = worn, `MISSING_TAGS` is a special case. The team has to agree on this.

## 5. Proposals for the implementation

### 5.1 Rules of the condition check (proposal)

| Observed condition | Other data | Decision |
|---|---|---|
| `AS_NEW` | tag present, original packaging present | `ACCEPT` |
| `AS_NEW` | tag present, original packaging missing | `ACCEPT` (with a lower score) |
| `AS_NEW` | tag missing | `MANUAL_REVIEW` |
| `MINOR_WEAR` | – | `MANUAL_REVIEW` |
| `MISSING_TAGS` | – | `MANUAL_REVIEW` |
| `VISIBLY_WORN` | – | `REJECT` |
| `DAMAGED` | – | `REJECT` |
| `MISSING_PARTS` | – | `REJECT` |

The `confidence_score` is high for clear cases and low for borderline cases. For `MANUAL_REVIEW`, `requires_manual_review` is `true`. This way the lecturer can see different scenarios: acceptance, rejection, borderline case, error.

### 5.2 Errors in the gRPC service (proposal)

| Case | gRPC status code |
|---|---|
| `return_id` or `sku` empty, condition `UNSPECIFIED` | `INVALID_ARGUMENT` |
| The same check is sent twice | `ALREADY_EXISTS` |
| `GetCheckResult` for an unknown check | `NOT_FOUND` |
| Simulated outage of the service | `UNAVAILABLE` (the client repeats the call) |

### 5.3 Status changes (proposal, derived from the target process)

```
NEW → APPROVED | REJECTED | IN_REVIEW
APPROVED → LABEL_SENT → RECEIVED | EXPIRED
RECEIVED → INSPECTED → ACCEPTED | REJECTED | MANUAL_REVIEW
ACCEPTED → REFUNDED → CLOSED
```

Added during the implementation (also a proposal): `IN_REVIEW → APPROVED | REJECTED`, `MANUAL_REVIEW → ACCEPTED | REJECTED`, `REJECTED | EXPIRED → CLOSED`. Otherwise there would be no way to the end for borderline cases and rejected returns. The REST mock uses the same table for `PATCH` (a forbidden change gives 409).

- One message per status change, with `eventId`, `returnId`, `oldStatus`, `newStatus`, `occurredAt`.
- One topic for status updates that several receivers can read from (customer information, reporting). This separates the systems from each other.
- Errors: messages the consumer does not understand end up in a separate dead letter queue. The producer repeats sending if the broker is not reachable.
- **Duplicate events:** the consumer remembers the `eventId` and ignores a message it has already processed. It also checks whether the status change is allowed.
- The status changes with timestamps are also the event log for Sprint 3 (process mining).

## 6. Comparison with the target process from Sprint 1

**Fits:**
- The REST mock is the marketplace entry (`s_mkt`).
- The three decisions of the gRPC service match the three paths after the gateway "Ergebnis?" (`gw2`): acceptance, rejection, borderline case. A borderline case goes to "Grenzfall prüfen & Entscheidung dokumentieren" (`t_man2`).
- The messaging steps (`t_notify`, `t_notify_in`, `t_end_notify`) send status messages.

**Does not fit completely** (the models stay unchanged; they are our own work and are adapted in Sprint 4 when the executable workflow is built):
- In `SOLL-PROZESS-REALISIERUNG.md` the gRPC service "books" the condition in the WMS (`t_gr_rec`) and delivers the message "Paket eingetroffen" (`ev_arrive`). According to the task it **judges** the condition and gives a recommendation. The decision is still made by the rule `t_decide`.
- According to the task the REST mock is **polled** (the workflow fetches the reports). The model shows an incoming message.
- Proposal: we change the models only in Sprint 4, when the executable workflow is built. Until then the difference is documented here.

## 7. Questions to the lecturer

The lecturer answered questions 1 to 3. Questions 4 to 6 are decided by us (01.11.2026, Yaraslau Krautsou); the lecturer can still object.

1. The grading slide mentions "payment" and "invoices". May we transfer the aspects by meaning: an event for the status of a return instead of payment, the condition check of an article instead of invoice capture?
   **Answer: this is an old slide, just ignore it.**
2. Changes to the `.proto`: we only added two Java options (`java_package`, `java_multiple_files`). Is that fine? The batch check has no error field per item. On an error we reject the whole batch and name the position in the message. Is that enough, or should we add an error field per item?
   **Answer: yes, that fits.**
3. REST mock: the example only shows return reports. For the order data (call by `orderId`) we designed our own format. Are there requirements, or is it free?
   **Answer: we can choose our own sensible format.**
4. Decoupling: the consumer keeps the status in its own memory and does not call the REST mock. The systems are connected only in the workflow (Sprint 4). Is this wanted, or should the consumer already update the status in the REST mock now?
   **Our decision: yes, the Java consumer stays decoupled.** Reasons: (a) the task asks for three components that "can be used later in the process", so each must work on its own, and joining them is the job of the workflow in Sprint 4 (job workers); (b) a consumer that calls the REST API adds a failure mode (mock down, 404, 409) and a coupling the task did not ask for, and the grading aspect is "decoupling of the systems"; (c) the graded Java parts keep all their tested error handling without extra HTTP code; (d) the joined flow is still shown, by the optional PHP consumer of the portal (README section 4, demo scene 3b). Known weak point: the state of the Java consumer is in memory. **Fallback if the lecturer wants the consumer to write the status:** a small `ReturnStatusGateway` in `messaging-consumer` (in-memory by default, REST version behind a Spring profile), about half a day.
5. Hand-in: we show the scenarios live in the 20-minute meeting (steps in `DEMO-SCRIPT.md`). What do we upload to ILIAS, is the link to the GitHub repository enough? Under which GitHub name do we invite the lecturer as a member?
   **Our decision:** we upload `AI-USAGE.md` of Sprint 2 to ILIAS. It contains the link to the GitHub repository (block "Hand-in" at the top). The repository is **private**: before the meeting the lecturer must be invited as a collaborator (GitHub name still unknown) or the repository must be made public.
6. From Sprint 1, still open: from when does the return period "8–12 days" count, and in calendar days or working days?
   **Our decision:** not needed for Sprint 2. It is resolved in a future sprint.

## 8. Decisions and what we still do

- Programming language and broker: **Java (Spring Boot) and RabbitMQ**, chosen by Yaraslau Krautsou on 22.10.2026. No confirmation from the team is needed. We must be able to explain the code.
- The code for all three programs exists (Claude Code, co-author Yaraslau Krautsou). Everyone who presents reads and tries out their part (see the checklist in `AI-USAGE.md`).
- The status changes in 5.3 and the rules in 5.1 are decided as proposed.
- Presentation in the 20-minute meeting: the split is in the table in `DEMO-SCRIPT.md` (decided 01.11.2026). Rehearse the demo once completely before.
