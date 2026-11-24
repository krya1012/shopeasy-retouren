# Sprint 4: Workflow (Camunda)

Period 17.11.–8.12.2026. Hand-in in ILIAS by 11:30. Task: `Aufgabe DvG WS26_Sprint 4.pdf`. All documentation is in English. AI use: [AI-USAGE.md](AI-USAGE.md).

The target process of Sprint 1 runs as an executable Camunda 8 workflow. Architecture, element mapping, error handling and test cases are in [ARCHITECTURE.md](ARCHITECTURE.md).

## State

| Part of the task | State |
|---|---|
| 1. Architecture with workflow engine | done: [ARCHITECTURE.md](ARCHITECTURE.md) |
| 2. Executable model: gateways, rules, human tasks, errors | done and tested: `process/` (BPMN, 2 DMN tables, 8 forms) |
| 2. Services from Sprint 2 (REST mock, gRPC, RabbitMQ) | done: six job workers and the marketplace poller in `src/main/java/de/shopeasy/workflow/` |
| 3. Demo of the test cases | done: [DEMO-SCRIPT.md](DEMO-SCRIPT.md) (marketplace and e-mail returns; accepted, rejected, manual review; gRPC service, shipping API and payment not reachable); 91 automated tests |
| Open | PNG export of the diagram from the Camunda Modeler (`process/returns-process.png`); answers of the lecturer (ARCHITECTURE.md sections 7 and 8) |

## Folder

| Path | Content |
|---|---|
| `process/returns-process.bpmn` | executable process `shopeasy-returns` (IDs as in Sprint 1) |
| `process/registration-check.dmn`, `process/acceptance-decision.dmn` | the two rule tables |
| `process/forms/*.form` | one Camunda Form per user task |
| `src/main/java/de/shopeasy/workflow/` | worker application: `workers/` (six job workers), `poller/`, `returns/` (REST mock client), `status/` (status changes and events), `config/` |
| `src/test/java/de/shopeasy/workflow/` | 91 tests: model with mocked workers (`ReturnsProcessTest`), model with real workers (`WorkersEndToEndTest`), one test class per worker and client |
| `DEMO-SCRIPT.md` | the demo, one command per step |

## Requirements

- Java 21 or newer (Gradle is downloaded by `./gradlew`).
- For the tests: Docker (Colima, Docker Desktop or Podman with the Docker socket). The tests start Camunda in a container.
- For the live demo: `c8ctl` and a local cluster (`c8ctl cluster start`, Operate and Tasklist at http://localhost:8080, login `demo` / `demo`), Docker for RabbitMQ, and the Sprint 2 services (REST mock on port 8081, gRPC service, consumer). The steps are in [DEMO-SCRIPT.md](DEMO-SCRIPT.md).

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

## Run the demo

The step-by-step demo with all cases is in [DEMO-SCRIPT.md](DEMO-SCRIPT.md). In short:

```
./gradlew bootRun
```

starts the workers (after the Sprint 2 services and the cluster run, and the model is deployed with `c8ctl deploy process/returns-process.bpmn process/registration-check.dmn process/acceptance-decision.dmn process/forms/*.form --profile=local`). The start messages are `marketplace-return-received` (sent by the poller), `webshop-return-registered` and `email-return-received`; the parcel arrives with `parcel-arrived` (correlation key `returnId`). For a short demo of the timer, start the instance with the variable `arrivalTimeout` set to `PT2M`. Settings (`workflow.*`: REST mock URL, gRPC target, retry pause, poller) are in `src/main/resources/application.yaml`; `--workflow.poller.enabled=false` switches the poller off.

## Checks without a cluster

```
c8ctl bpmn lint process/returns-process.bpmn
```
