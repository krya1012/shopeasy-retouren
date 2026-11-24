# Sprint 4: Workflow (Camunda)

Period 17.11.–8.12.2026. Hand-in in ILIAS by 11:30. Task: `Aufgabe DvG WS26_Sprint 4.pdf`. All documentation is in English. AI use: [AI-USAGE.md](AI-USAGE.md).

The target process of Sprint 1 runs as an executable Camunda 8 workflow. Architecture, element mapping, error handling and test cases are in [ARCHITECTURE.md](ARCHITECTURE.md).

## State

| Part of the task | State |
|---|---|
| 1. Architecture with workflow engine | done: [ARCHITECTURE.md](ARCHITECTURE.md) |
| 2. Executable model: gateways, rules, human tasks, errors | done and tested: `process/` (BPMN, 2 DMN tables, 8 forms) |
| 2. Services from Sprint 2 (job workers) | **next step**: the tests use mocked workers; see ARCHITECTURE.md section 9 |
| 3. Demo of the test cases | automated for the model (24 tests); live demo with the workers is the next step |

## Folder

| Path | Content |
|---|---|
| `process/returns-process.bpmn` | executable process `shopeasy-returns` (IDs as in Sprint 1) |
| `process/registration-check.dmn`, `process/acceptance-decision.dmn` | the two rule tables |
| `process/forms/*.form` | one Camunda Form per user task |
| `src/test/java/de/shopeasy/workflow/ReturnsProcessTest.java` | process tests with mocked workers |
| `src/main/java/de/shopeasy/workflow/WorkflowApplication.java` | worker application (workers follow) |

## Requirements

- Java 21 or newer (Gradle is downloaded by `./gradlew`).
- For the tests: Docker (Colima, Docker Desktop or Podman with the Docker socket). The tests start Camunda in a container.
- For the live model: `c8ctl` and a local cluster (`c8ctl cluster start`, Operate and Tasklist at http://localhost:8080, login `demo` / `demo`).

**Run every command in the folder `sprint4-workflow`.**

## Run the tests

```
./gradlew test --rerun
```

With Colima, Testcontainers needs the socket of the Colima VM; set it once in the terminal before the command:

```
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## Try the model on the local cluster

```
c8ctl cluster start
c8ctl deploy process/returns-process.bpmn process/registration-check.dmn process/acceptance-decision.dmn process/forms/*.form --profile=local
c8ctl publish msg email-return-received --variables '{"emailFrom":"kunde@example.com","emailSubject":"Retoure","emailBody":"Bitte Jacke zuruecknehmen"}' --profile=local
c8ctl list ut --profile=local
```

The instance waits at the user task "Capture return from e-mail" (Tasklist, group `customer-service`). The service tasks need the workers (next step). Until they exist, the instance waits at "Create return case" after the clerk completes the first task.

The other entries are the messages `marketplace-return-received` (variable `returnId`) and `webshop-return-registered`; the parcel arrives with the message `parcel-arrived` (correlation key `returnId`). For a short demo of the timer, start the instance with the variable `arrivalTimeout` set to `PT2M`.

## Checks without a cluster

```
c8ctl bpmn lint process/returns-process.bpmn
```
