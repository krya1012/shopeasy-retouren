# Vorführung Sprint 2 (20 Minuten)

Schritt für Schritt, wie wir REST-Mock, gRPC-Service und Messaging zeigen. Jeder Schritt hat **Sagen** (was wir erzählen), den **Befehl** und **Du siehst** (was erscheinen muss). Alle Befehle laufen im Ordner `sprint2-integration`.

Der Code ist mit Claude Code entstanden (siehe `AI-USAGE.md`). Jede Person liest vorher ihren Teil und muss ihn erklären können.

## Zeitplan

| Teil | Minuten | Wer |
|---|---|---|
| Einstieg | 1 | ___ |
| 1. REST-Mock | 3 | ___ |
| 2. gRPC-Service | 4 | ___ |
| 3. Messaging | 5 | ___ |
| 4. Tests und Code | 1 | ___ |
| Fragen und Puffer | 6 | alle |

Wer was vorstellt, klärt das Team und trägt es hier ein.

## Einstieg (1 Minute)

```
REST-Mock (8080)        gRPC-Service (9090)       Producer --> RabbitMQ --> Consumer
Marktplatz-Eingang      Zustandsprüfung           Status-Updates
„Marktplatz-Retoure     „Annahme/Ablehnung        (Nachrichtenschritte im Soll-Prozess)
 eingegangen (REST)“     entscheiden“
```

**Sagen:** Wir zeigen drei kleine Systeme, die später im Workflow zusammenkommen (Sprint 4). Jedes hat einen Client zum Ausprobieren. Alles ist in Java mit Spring Boot, der Broker ist RabbitMQ. Bei jedem Teil zeigen wir zuerst den normalen Fall und danach die Fehlerfälle.

## Vorbereitung (vor dem Termin, ca. 10 Minuten)

1. Einmal mit Internet `./gradlew build`, danach sind alle Bibliotheken auf dem Rechner.
2. RabbitMQ starten: `podman machine start` (falls die VM nicht läuft), dann `podman start shopeasy-rabbitmq`. Prüfen: http://localhost:15672 (Benutzer `guest`, Passwort `guest`).
3. Drei Terminals **frisch** starten (wer vorher geübt hat, startet alles neu, sonst ist der Speicher nicht mehr leer):

```
./gradlew :rest-mock:bootRun --console=plain          # Terminal 1
./gradlew :grpc-service:run --console=plain           # Terminal 2
./gradlew :messaging-consumer:bootRun --console=plain # Terminal 3
```

4. Ein vierter Terminal für die Client-Befehle. Schrift groß stellen.
5. Browser-Tabs: http://localhost:8080/swagger-ui.html und die RabbitMQ-Oberfläche. In der Oberfläche unter „Queues“ → `retouren.status.dlq` → „Purge Messages“ drücken (jeder Übungslauf legt dort 3 Nachrichten ab, vor dem Termin soll sie leer sein).

Fertig, wenn Terminal 1 „Started RestMockApplication“, Terminal 2 „läuft auf Port 9090“ und Terminal 3 „Started ConsumerApplication“ zeigt.

## 1. REST-Mock (3 Minuten)

**Sagen:** Der REST-Mock spielt den Marktplatz (Amazon, Zalando). Das Format der Meldungen ist das aus der Aufgabe. Die Liste kann man nach Status und Marktplatz filtern.

```
./gradlew :rest-mock:runClient --args="list status=NEW marketplace=ZALANDO" --console=plain -q
```
**Du siehst:** `-> 200` und `{"data":[{"returnId":"RET-2026-00457", ...` (die Liste steckt im Feld `data`).

```
./gradlew :rest-mock:runClient --args="get RET-2026-00457" --console=plain -q
./gradlew :rest-mock:runClient --args="order ORD-2026-118932" --console=plain -q
```
**Du siehst:** die einzelne Meldung und die Bestelldaten (Format der Bestellung haben wir selbst entworfen, der Dozent hat das bestätigt).

```
./gradlew :rest-mock:runClient --args="status RET-2026-00457 APPROVED" --console=plain -q
```
**Du siehst:** `"status":"APPROVED"` und ein neues `updatedAt`.

**Fehlerfälle:**
```
./gradlew :rest-mock:runClient --args="get RET-GIBT-ES-NICHT" --console=plain -q
./gradlew :rest-mock:runClient --args="status RET-2026-00457 QUATSCH" --console=plain -q
./gradlew :rest-mock:runClient --args="status RET-2026-00462 APPROVED" --console=plain -q
```
**Du siehst:** `-> 404`, `-> 400` und `-> 409` (die Retoure `RET-2026-00462` ist abgelehnt und darf nicht mehr genehmigt werden). Fehler kommen als ProblemDetail (RFC 9457) mit dem passenden Statuscode.

**Wenn Zeit ist:** Im Browser Swagger-UI öffnen und bei `GET /api/v1/returns` „Try it out“ drücken. Die Beschreibung der Schnittstelle entsteht automatisch (OpenAPI).

## 2. gRPC-Service (4 Minuten)

**Sagen:** Der Service bewertet den Zustand eines Artikels und empfiehlt Annahme, Ablehnung oder manuelle Prüfung. Die Schnittstelle ist die `.proto` des Dozenten (wir haben nur Java-Optionen ergänzt). Die Regeln stehen in `ConditionRules`. Im Terminal 2 sieht man, was der Server tut.

**Normale Fälle:**
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00458 SH-DRS-017-S DAMAGED" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00459 SH-JKT-088-L AS_NEW tags=false" --console=plain -q
```
**Du siehst:** `ACCEPT (Score 0.95)`, `REJECT (Score 0.95)` und `MANUAL_REVIEW (Score 0.5, manuell prüfen: true)`. Der Grenzfall geht später im Workflow an eine Person (Sprint 4).

```
./gradlew :grpc-service:runClient --args="get RET-2026-00457 SH-BLZ-042-M" --console=plain -q
```
**Du siehst:** dasselbe Ergebnis wie vorher, jetzt abgerufen.

**Fehlerfälle:**
```
./gradlew :grpc-service:runClient --args="check RET-2026-00457 SH-BLZ-042-M AS_NEW" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-2026-00460 SH-SNK-120-42 UNSPECIFIED" --console=plain -q
./gradlew :grpc-service:runClient --args="check RET-FAIL-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
**Du siehst:**
- `FEHLER [ALREADY_EXISTS] Doppelte Anfrage` (dieselbe Prüfung noch einmal)
- `FEHLER [INVALID_ARGUMENT] Falsche Daten: observed_condition muss gesetzt sein`
- `Service nicht verfügbar, neuer Versuch 2 von 3` und danach das Ergebnis (simulierter Ausfall, der Client wiederholt)

**Wenn Zeit ist:** Die Frist zeigen. Das dauert 3 Sekunden.
```
./gradlew :grpc-service:runClient --args="check RET-SLOW-1 SH-BLZ-042-M AS_NEW" --console=plain -q
```
**Du siehst:** `FEHLER [DEADLINE_EXCEEDED] Zeitüberschreitung ...`. Der Client wiederholt hier absichtlich nicht, weil die Prüfung beim Service schon gespeichert sein kann.

Hinweis: `RET-FAIL-1` fällt nur beim ersten Mal aus und `RET-SLOW-1` wird nur einmal gespeichert. Beim Üben `RET-FAIL-2`, `RET-SLOW-2` usw. nehmen.

**Reserve:** `./gradlew :grpc-service:runClient --console=plain -q` ohne Argumente spielt alle zwölf Szenarien am Stück ab (ca. 8 Sekunden).

## 3. Messaging mit RabbitMQ (5 Minuten)

**Sagen:** Ein Producer meldet Statuswechsel, der Consumer aktualisiert den Status. Der Producer kennt nur den Exchange, keine Empfänger. Das entkoppelt die Systeme.

**Aufbau zeigen** (RabbitMQ-Oberfläche): Unter „Exchanges“ `retouren.status`, unter „Queues“ `verwaltung`, `kundeninfo` und `dlq` (Fehler-Queue). Bei `retouren.status` unter „Bindings“: `retoure.status.#` geht an `verwaltung`, nur `APPROVED`, `REJECTED`, `ACCEPTED` und `REFUNDED` gehen an `kundeninfo`.

**Producer starten** (Terminal 4, braucht ca. 5 Sekunden):
```
./gradlew :messaging-producer:bootRun --console=plain
```
**Du siehst im Producer:** `Gesendet: ... -> retoure.status.APPROVED (Broker hat bestätigt)` für jede Nachricht.

**Im Consumer (Terminal 3) zeigen wir der Reihe nach:**
1. `Status aktualisiert: RET-... NEW -> APPROVED` bis `REFUNDED -> CLOSED`: der ganze Weg einer Retoure.
2. `Duplikat ignoriert: Event ... wurde schon verarbeitet`: dasselbe Event kam zweimal, es wird nur einmal verarbeitet (an der `eventId` erkannt).
3. `INSPECTED -> MANUAL_REVIEW -> ACCEPTED`: der Grenzfall mit manueller Prüfung.
4. `WARN Verwaltung lehnt Nachricht ab: Übergang nicht erlaubt: ... NEW -> INSPECTED`: ein nicht erlaubter Statuswechsel wird abgelehnt und kommt in die Fehler-Queue.
5. `Nachricht ist kein gültiges Event-JSON` und `Pflichtfeld fehlt: returnId`: der Consumer versteht die Nachricht nicht, sie kommt ebenfalls in die Fehler-Queue.
6. In der RabbitMQ-Oberfläche bei `retouren.status.dlq` stehen jetzt **3 Nachrichten** (die Anzeige aktualisiert sich alle 5 Sekunden). „Get messages“ zeigt den Inhalt und im Header `x-death` den Grund (`rejected`).
7. `Kundeninfo: Kund:in informiert, ...` nur bei `APPROVED`, `ACCEPTED`, `REFUNDED`: der zweite Empfänger bekommt nur seine Status.

**Fehlerfall Broker nicht erreichbar** (optional, ca. 1 Minute). Zuerst den Consumer in Terminal 3 mit Strg+C beenden, sonst füllt er das Log mit Verbindungsmeldungen (er verbindet sich nach dem Neustart des Brokers von selbst wieder, das dauert bis zu 10 Sekunden):
```
podman stop shopeasy-rabbitmq
./gradlew :messaging-producer:bootRun --console=plain
podman start shopeasy-rabbitmq
```
**Du siehst:** `Senden fehlgeschlagen ... Versuch 2 von 3`, `Versuch 3 von 3` und `FEHLER: Event nicht zugestellt. Broker nach 3 Versuchen nicht erreichbar`. Danach den Consumer in Terminal 3 mit dem Befehl aus der Vorbereitung neu starten.

**Nicht live zeigbar:** „Keine Queue nimmt die Nachricht an“ (der Broker gibt sie zurück). Im Live-Aufbau findet jeder Routing-Key eine Queue. Das zeigt der Test `StatusEventPublisherTest`.

## 4. Tests und Code (1 Minute)

```
./gradlew test --rerun --console=plain
```
**Du siehst:** jeden Test mit Namen und `PASSED`, am Ende `BUILD SUCCESSFUL` (65 Tests, ca. 8 Sekunden). Die Testnamen beschreiben die Fälle (`doppeltesEventWirdIgnoriert`, `langsamerServiceLaeuftInDieFristDesClients`).

**Sagen:** Jedes Modul hat kleine Klassen mit je einer Aufgabe:
- REST: `ReturnController` (Endpunkte), `ReturnStore` (Daten), `RestClientDemo` (Client)
- gRPC: `ConditionRules` (Regeln), `RequestValidator` (Prüfung der Eingabe), `ArticleConditionServiceImpl` (Service), `ConditionClient` (Client mit Wiederholung und Frist)
- Messaging: `StatusEventPublisher` (Senden mit Bestätigung und Wiederholung), `StatusListeners` (Queues), `StatusUpdateHandler` (Logik), `StatusTransitions` (erlaubte Wechsel), `EventParser` (Prüfung der Nachricht)

## Wenn etwas nicht klappt

- **Port belegt:** `lsof -i :8080` (bzw. 9090), den alten Prozess beenden.
- **RabbitMQ nicht erreichbar:** `podman machine start`, danach `podman start shopeasy-rabbitmq`.
- **Gradle lädt etwas:** Das braucht Internet. Darum vorher einmal `./gradlew build` ausführen.
- **Ergebnis sieht anders aus:** Server neu starten (der Speicher ist dann wieder leer), z. B. meldet gRPC sonst `ALREADY_EXISTS`.
- **Rückfall:** gRPC-Demo ohne Argumente, danach `./gradlew test --rerun`. Die Tests zeigen alle Fälle auch ohne Broker. Eine Bildschirmaufnahme eines früheren Durchlaufs kann als Reserve dienen (nicht ins Repo legen).

## Fragen, die kommen können

- **Warum ein Topic-Exchange und zwei Queues?** Der Producer weiß nichts von den Empfängern. Ein neuer Empfänger bekommt eine neue Queue, am Producer ändert sich nichts. (`RabbitConfig`)
- **Wie erkennt ihr doppelte Events?** Der Consumer merkt sich die `eventId` (`ProcessedEvents`). Ein Duplikat wird bestätigt, aber nicht noch einmal verarbeitet. Nach einem Neustart ist die Liste leer, im Betrieb gehört sie in eine Datenbank.
- **Warum wird eine unverständliche Nachricht nicht erneut zugestellt?** Beim zweiten Mal wäre sie auch nicht verständlicher, es gäbe eine Endlosschleife. Sie kommt in die Fehler-Queue, dort kann man sie prüfen. (`StatusListeners`)
- **Was bedeuten Publisher Confirms und `mandatory`?** Der Broker bestätigt, dass er die Nachricht angenommen hat. Mit `mandatory` meldet er auch, wenn keine Queue sie bekommen hat. (`StatusEventPublisher`)
- **Warum `ALREADY_EXISTS` bei einer doppelten Prüfung?** Die Aufgabe nennt die doppelte Anfrage als Fehlerfall. Der Client sieht so, dass es die Prüfung schon gibt, und kann sie mit `get` abrufen.
- **Warum wiederholt der Client bei `DEADLINE_EXCEEDED` nicht?** Der Service könnte die Prüfung schon gespeichert haben, eine Wiederholung würde `ALREADY_EXISTS` liefern. Erst mit `get` nachsehen. Bei `UNAVAILABLE` (nicht verarbeitet) wiederholt er dagegen.
- **Warum wird bei der Stapelprüfung alles oder nichts abgelehnt?** Die `.proto` hat kein Fehlerfeld je Position. Der Dozent hat bestätigt, dass das passt.
- **Warum gibt der REST-Mock 409 bei einem Statuswechsel?** Die Anfrage passt nicht zum Zustand der Retoure. Es gelten dieselben Wechsel wie beim Messaging. Derselbe Status noch einmal gibt 200, damit man eine Anfrage nach einem Timeout gefahrlos wiederholen darf. (`ReturnStore`, `StatusTransitions`)
- **Warum wird ohne Originalverpackung nicht abgelehnt?** Die Verpackung ist keine Bedingung für die Rücknahme. Wir senken nur den Score von 0,95 auf 0,85 und nennen es in der Begründung. (`ConditionRules`)
- **Wer hat die Regeln für Annahme und Ablehnung festgelegt?** Sie sind ein Vorschlag aus `aufgabenanalyse.md` (Abschnitt 5.1). Das Team muss sie vor dem Termin abstimmen. Bis dahin steht in `AI-USAGE.md` „offen“.
- **Was hat die KI gemacht?** Der Code ist mit Claude Code entstanden und in `AI-USAGE.md` dokumentiert. Wir haben ihn gelesen und können ihn erklären (das muss vorher stimmen).
- **Wie hängt das mit Sprint 1 zusammen?** Der REST-Mock entspricht „Marktplatz-Retoure eingegangen (REST)“, der gRPC-Service „Annahme/Ablehnung entscheiden“, das Messaging den Nachrichtenschritten. Die Modelle passen wir in Sprint 4 an (Abschnitt 6 in `aufgabenanalyse.md`).

## Nach dem Termin

Terminals mit Strg+C beenden, `podman stop shopeasy-rabbitmq`.
