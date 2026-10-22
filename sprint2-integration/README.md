# Sprint 2: Integrationstechniken (REST, gRPC, Messaging)

Drei kleine Programme, die später im Prozess genutzt werden. Jedes hat einen Client zum Ausprobieren. Die Aufgabe steht in `Aufgabe DvG WS26_Sprint 2.pdf`, die Analyse in [aufgabenanalyse.md](aufgabenanalyse.md). KI-Nutzung: [AI-USAGE.md](AI-USAGE.md).

Der Code ist mit Claude Code entstanden (Co-Autor Yaraslau Krautsou) und noch **nicht vom Team geprüft**. Sprache (Java mit Spring Boot) und Broker (RabbitMQ) hat Yaraslau Krautsou am 22.10.2026 gewählt, die Zustimmung des Teams steht noch aus.

| Ordner | Programm | Bezug zum Soll-Prozess |
|---|---|---|
| `rest-mock/` | REST-Mock für Retourenmeldungen (Amazon, Zalando) | „Marktplatz-Retoure eingegangen (REST)“ |
| `grpc-service/` | gRPC-Service zur Zustandsprüfung | „Annahme/Ablehnung entscheiden“, „Zustand im WMS buchen“ |
| `messaging-producer/`, `messaging-consumer/` | Status-Updates über RabbitMQ | Nachrichtenschritte (`t_notify`, `t_notify_in`, `t_end_notify`) |

## Voraussetzungen

- Java 21 oder neuer. Gradle wird über `./gradlew` geladen, eine eigene Installation ist nicht nötig.
- Für das Messaging ein Container-Programm (Podman oder Docker).

Alle Tests: `./gradlew test`. Alles bauen: `./gradlew build`.

## 1. REST-Mock

```
./gradlew :rest-mock:bootRun       # Server auf Port 8080
./gradlew :rest-mock:runClient     # Client, ruft alle Endpunkte auf
```

| Aufruf | Zweck |
|---|---|
| `GET /api/v1/returns?status=NEW&marketplace=ZALANDO` | Liste, beide Filter sind optional |
| `GET /api/v1/returns/{returnId}` | eine Meldung (404, wenn unbekannt) |
| `GET /api/v1/orders/{orderId}` | Bestelldaten (404, wenn unbekannt) |
| `PATCH /api/v1/returns/{returnId}` mit `{"status":"APPROVED"}` | Status ändern, `updatedAt` wird neu gesetzt |

Das Format der Meldung ist das aus der Aufgabe (Folie 9). Fehler kommen als `ProblemDetail` (RFC 9457) mit passendem Statuscode: 404 für Unbekanntes, 400 für ungültigen Status oder Filter. Es gibt sechs Testretouren im Speicher (`ReturnStore`); nach einem Neustart sind die Daten wieder im Ausgangszustand. Die Bestelldaten (`orderId`) sind eine Annahme, die Aufgabe nennt dafür kein Format (Frage 3 an den Dozenten).

## 2. gRPC-Service

```
./gradlew :grpc-service:run          # Server auf Port 9090
./gradlew :grpc-service:runClient    # Client, spielt alle Szenarien durch
```

Die Schnittstelle ist die `.proto` des Dozenten (`grpc-service/src/main/proto/article_condition_service.proto`). **Einzige Änderung:** zwei Zeilen `option java_...`, damit der erzeugte Code in einem Paket liegt. Service, Nachrichten und Felder sind unverändert (Frage 2 an den Dozenten bleibt offen).

Regeln der Prüfung (Vorschlag, im Team abzustimmen):

| Zustand | Weitere Angaben | Entscheidung | Score |
|---|---|---|---|
| `AS_NEW` | Etikett vorhanden | `ACCEPT` | 0,95 |
| `AS_NEW` | Etikett fehlt | `MANUAL_REVIEW` | 0,50 |
| `MINOR_WEAR` | – | `MANUAL_REVIEW` | 0,55 |
| `MISSING_TAGS` | – | `MANUAL_REVIEW` | 0,50 |
| `VISIBLY_WORN` | – | `REJECT` | 0,90 |
| `DAMAGED` | – | `REJECT` | 0,95 |
| `MISSING_PARTS` | – | `REJECT` | 0,90 |

Fehler (gRPC-Statuscodes):

| Fall | Statuscode |
|---|---|
| `return_id` oder `sku` leer, Zustand nicht gesetzt, `inspected_at` kein ISO-8601-Wert | `INVALID_ARGUMENT` |
| Dieselbe Prüfung (`return_id` + `sku`) noch einmal | `ALREADY_EXISTS` |
| `GetCheckResult` für eine Prüfung, die es nicht gibt | `NOT_FOUND` |
| Simulierter Ausfall: Eine `return_id` mit Präfix `RET-FAIL` schlägt beim ersten Aufruf fehl | `UNAVAILABLE` |
| Unerwarteter Fehler im Service | `INTERNAL` (ohne Details nach außen) |

**Stapelprüfung:** Die `.proto` hat kein Fehlerfeld je Position. Deshalb gilt „alles oder nichts“: Ist eine Position falsch oder doppelt, wird der ganze Stapel abgelehnt und nichts gespeichert. Die Meldung nennt die Position (`items[1]: ...`).

**Client** (`ConditionClient`): Jeder Aufruf hat eine Frist von 3 Sekunden. Bei `UNAVAILABLE` wiederholt er bis zu dreimal, alle anderen Fehler nicht (falsche Daten bleiben falsch, ein Duplikat soll nicht noch einmal gesendet werden). Am Ende steht eine `ConditionClientException` mit verständlicher Meldung.

## 3. Messaging mit RabbitMQ

### Broker starten

Podman (einmalig die virtuelle Maschine anlegen, danach den Container starten):

```
podman machine init && podman machine start
podman run -d --name shopeasy-rabbitmq -p 5672:5672 -p 15672:15672 docker.io/library/rabbitmq:4-management
```

Mit Docker geht auch `docker compose up -d` (Datei `compose.yaml`). Weboberfläche: http://localhost:15672 (Benutzer `guest`, Passwort `guest`). Später `podman start shopeasy-rabbitmq` bzw. `podman stop shopeasy-rabbitmq`.

### Programme starten (zwei Terminals)

```
./gradlew :messaging-consumer:bootRun    # zuerst: legt Queues an und wartet auf Nachrichten
./gradlew :messaging-producer:bootRun    # spielt das Demo-Szenario ab und beendet sich
```

### Aufbau

```
Producer --> Exchange retouren.status (Topic)
              |-- retoure.status.#  --> Queue verwaltung   (alle Statuswechsel, aktualisiert den Status)
              |-- ...APPROVED / REJECTED / ACCEPTED / REFUNDED --> Queue kundeninfo (informiert Kund:innen)
            abgelehnte Nachrichten --> Exchange retouren.status.dlx --> Queue dlq (Fehler-Queue)
```

Der Producer kennt nur den Exchange und den Routing-Key `retoure.status.<NEUER_STATUS>`, keine Queues und keine Empfänger. Ein dritter Empfänger braucht keine Änderung am Producer.

### Event `ReturnStatusChanged` (JSON, der Vertrag zwischen Producer und Consumer)

```json
{
  "eventId": "3d70997d-0378-46d7-ae36-c9d8672865dc",
  "eventType": "ReturnStatusChanged",
  "returnId": "RET-2026-00457",
  "oldStatus": "NEW",
  "newStatus": "APPROVED",
  "occurredAt": "2026-10-22T11:47:25.123Z",
  "source": "ShopEasy-Retourenmanagement"
}
```

Pflichtfelder sind `eventId`, `returnId`, `oldStatus`, `newStatus` und `occurredAt`. Unbekannte Zusatzfelder ignoriert der Consumer, damit das Event später erweitert werden kann. Producer und Consumer haben je eine eigene Kopie der Klassen, es gibt bewusst keinen gemeinsamen Code.

### Erlaubte Statuswechsel (Vorschlag, im Team abzustimmen)

```
NEW → APPROVED | REJECTED | IN_REVIEW
IN_REVIEW → APPROVED | REJECTED
APPROVED → LABEL_SENT → RECEIVED | EXPIRED
RECEIVED → INSPECTED → ACCEPTED | REJECTED | MANUAL_REVIEW
MANUAL_REVIEW → ACCEPTED | REJECTED
ACCEPTED → REFUNDED → CLOSED
REJECTED, EXPIRED → CLOSED
```

### Fehlerbehandlung

| Fall | Was passiert |
|---|---|
| Broker nicht erreichbar | Producer versucht es dreimal, dann `EventNotDeliveredException` und Exit-Code 1 |
| Broker bestätigt nicht (nack) oder gar nicht | Producer meldet den Fehler (Publisher Confirms) |
| Keine Queue nimmt die Nachricht an | Der Broker gibt sie zurück (`mandatory`), Producer meldet den Fehler |
| Consumer versteht die Nachricht nicht (kein JSON, Pflichtfeld fehlt, unbekannter Status) | Consumer lehnt ab (reject, ohne erneute Zustellung), die Nachricht liegt in der Fehler-Queue |
| Nicht erlaubter Statuswechsel oder `oldStatus` passt nicht zum bekannten Status | wie oben |
| **Doppeltes Event** (gleiche `eventId`) | Consumer erkennt es an der `eventId`, ignoriert es und bestätigt die Nachricht |
| Broker startet neu | Queues und Nachrichten sind dauerhaft (durable, persistent) |

Das Demo-Szenario zeigt: den vollständigen Weg einer Retoure, ein doppeltes Event, den Grenzfall `MANUAL_REVIEW`, einen nicht erlaubten Übergang, eine Nachricht ohne JSON und eine Nachricht ohne `returnId`. Im Log des Consumers steht jeweils, was passiert ist.

**Grenzen:** Der aktuelle Status und die Liste verarbeiteter `eventId`s liegen im Speicher und sind nach einem Neustart leer. Im Betrieb gehört beides in eine Datenbank. Die Kundeninfo prüft keine Statuswechsel, sie vertraut den Events (die Prüfung macht die Verwaltung). Die Fehler-Queue wird nur ins Log geschrieben.

## Zuordnung zu den Bewertungsaspekten

| Aspekt | Wo |
|---|---|
| Messaging: saubere Eventdefinition | Abschnitt „Event“, `StatusEvent` |
| Messaging: Producer-/Consumer-Logik, Entkopplung | `StatusEventPublisher`, `StatusListeners`, `RabbitConfig`, zwei Empfänger-Queues |
| Messaging: Fehler (nicht zustellbar, nicht verstanden, doppelt) | Tabelle „Fehlerbehandlung“, Tests `StatusEventPublisherTest`, `StatusUpdateHandlerTest` |
| gRPC: saubere Schnittstelle | `.proto` unverändert bis auf Java-Optionen |
| gRPC: Fehler im Service und im Client | Tabelle „Fehler“, Tests `ErrorHandlingTest`, `ConditionClientTest` |
| Struktur des Codes | Je Modul kleine Klassen mit einer Aufgabe, Regeln, Validierung und Service getrennt |
