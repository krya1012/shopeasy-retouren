# Sprint 2: Analyse der Aufgabe

Quellen: `Aufgabe DvG WS26_Sprint 2.pdf` (Prof. Heberle) und `article_condition_service.proto` (jetzt in `grpc-service/src/main/proto/`, dort nur um zwei Java-Optionen ergänzt). Die Umsetzung steht in der [README](README.md). Abgabe: **3.11.2026, 11:30 Uhr in ILIAS**.

Alles mit „Vorschlag“ ist ein Vorschlag der KI und muss im Team abgestimmt werden (Prüfliste in `AI-USAGE.md`).

## 1. Was wir bauen

Drei kleine Programme, die später im Prozess genutzt werden. Jedes bekommt einen **Client**, mit dem man es ausprobieren kann. Die Programmiersprache ist frei.

| Nr. | Programm | Was es tut | Bezug zum Soll-Prozess |
|---|---|---|---|
| 1 | **REST-Mock** für Retourenmeldungen | Liefert Retourenmeldungen, wie sie bei Amazon oder Zalando eingehen | „Marktplatz-Retoure eingegangen (REST)“ (`s_mkt`) |
| 2 | **gRPC-Service** zur Zustandsprüfung | Bewertet den Zustand eines Artikels und empfiehlt annehmen, ablehnen oder manuell prüfen | „Annahme/Ablehnung entscheiden“ (`t_decide`), „Zustand im WMS buchen“ (`t_gr_rec`) |
| 3 | **Nachrichtenaustausch** für Status-Updates | Ein Producer sendet Status-Updates, ein Consumer empfängt sie und aktualisiert den Status | Nachrichtenschritte „Label & Status „genehmigt“ senden“ (`t_notify`), „Wareneingang bestätigen“ (`t_notify_in`), „Kund:in über Ergebnis informieren“ (`t_end_notify`) |

## 2. Anforderungen je Programm

### 2.1 REST-Mock
- Alle Retourenmeldungen abrufen, mit Filter (z. B. nach Status oder Marktplatz).
- Eine Meldung über die `returnId` abrufen.
- Die Daten einer Bestellung über die `orderId` abrufen.
- Den Status einer Meldung ändern.
- Das Format der Meldung steht in der Aufgabe: `returnId`, `orderId`, `marketplace` (z. B. `ZALANDO`), `status` (z. B. `NEW`), `reason` (Code, z. B. `SIZE_TOO_SMALL`), `reasonText`, `requestedAt`, `updatedAt`, `items` (mit `sku`, `productName`, `quantity`, `unitPrice`, `currency`), `customer` und `returnLabelUrl`. Die Liste steckt im Beispiel in einem Feld `data`.

### 2.2 gRPC-Service
- Ausgangspunkt ist die mitgelieferte `.proto`. Änderungen nur nach Rücksprache mit dem Dozenten.
- Drei Aufrufe: `CheckCondition` (ein Artikel), `CheckConditionBatch` (mehrere Artikel) und `GetCheckResult` (früheres Ergebnis abrufen).
- Antwort: `decision` (`ACCEPT`, `REJECT`, `MANUAL_REVIEW`), `confidence_score`, `reason`, `requires_manual_review`, `checked_at`.
- Der Service **simuliert** die Bewertung, erlaubt **verschiedene Prozessszenarien** und berücksichtigt **Fehler**.
- Grenzfälle (`MANUAL_REVIEW`) werden später im Workflow (Sprint 4) von einer Person entschieden.

### 2.3 Nachrichtenaustausch
- Ein Message Broker ist Pflicht: ActiveMQ oder RabbitMQ, beide auch mit Docker.
- Wir überlegen selbst, welche Statusübergänge sinnvoll sind und welche Topics oder Queues wir brauchen.
- Producer: erzeugt Status-Updates zu Retouren. Consumer: holt sie ab und aktualisiert den Status einer Retoure.

## 3. Wie bewertet wird

Aus der Folie „Aspekte Messaging und gRPC“:

| Bereich | Bewertet wird |
|---|---|
| Messaging | saubere Definition der Events, richtige Producer-/Consumer-Logik, Entkopplung der Systeme, Struktur des Codes |
| Messaging, Fehler | Nachricht kann nicht zugestellt werden, Consumer versteht die Nachricht nicht, **doppelte Events** (auf der Folie rot) |
| gRPC | saubere und sinnvolle Schnittstellenbeschreibung, Struktur des Codes |
| gRPC, Fehler | passende Fehler im Service (z. B. doppelte Anfrage, falsche Daten), Fehlerbehandlung im Client |

**Auffällig:** Die Folie spricht von „Payment“ und „Rechnungen“, unsere Aufgabe handelt aber von Retouren. **Antwort des Dozenten (Frage 1): Das ist eine alte Folie, wir ignorieren sie.** Die Fehlerfälle (nicht zugestellt, nicht verstanden, doppelte Events, doppelte Anfrage, falsche Daten) setzen wir trotzdem um, weil sie zu Messaging und gRPC allgemein gehören.

**Weitere Regeln aus der Aufgabe:**
- Standards und Best Practices von Prof. Zimmermann anwenden.
- KI ist erlaubt. Wir müssen verstehen, was wir abgeben, der Dozent stellt dazu Fragen.
- Die KI-Nutzung muss dokumentiert sein (`AI-USAGE.md`).

**Aus unserer Definition of Done** (`Definition of Done.pdf`): Alle stimmen dem Endstand zu, die Ergebnisse sind für das Team und den Dozenten zugänglich, alte Versionen sind aus dem Repo gelöscht, es ist geklärt, wer was vorstellt, und wir nutzen KI nur, soweit wir sie nachvollziehen können.

## 4. Die `.proto` im Detail

**Eingabe** (`ConditionCheckRequest`): `return_id`, `sku`, `observed_condition`, `image_urls`, `tags_present`, `original_packaging`, `inspector_note`, `inspected_at`.

**Zustände** (`ObservedCondition`): `AS_NEW`, `MINOR_WEAR`, `VISIBLY_WORN`, `DAMAGED`, `MISSING_TAGS`, `MISSING_PARTS` (und `OBSERVED_CONDITION_UNSPECIFIED`).

**Ergebnis** (`Decision`): `ACCEPT`, `REJECT`, `MANUAL_REVIEW` (und `DECISION_UNSPECIFIED`).

**Was die Datei offen lässt:**
- Welcher Zustand zu welcher Entscheidung führt. Das legen wir fest (Vorschlag in 5.1).
- Welche Fehler es gibt. gRPC kennt dafür Statuscodes (Vorschlag in 5.2).
- Bei `CheckConditionBatch` gibt es kein Feld für einen Fehler bei einzelnen Artikeln.
- Es gibt kein `package` und keine Sprachoptionen. Beides kann nötig sein, je nach Sprache.

**Zuordnung zu unserer Zustandsskala aus Sprint 1** (für die KPIs K6–K8): Sprint 1 kennt nur neuwertig, beschädigt und getragen. Vorschlag: `AS_NEW` = neuwertig, `DAMAGED` und `MISSING_PARTS` = beschädigt, `VISIBLY_WORN` und `MINOR_WEAR` = getragen, `MISSING_TAGS` ist ein Sonderfall. Das ist im Team abzustimmen.

## 5. Vorschläge für die Umsetzung

### 5.1 Regeln der Zustandsprüfung (Vorschlag)

| Beobachteter Zustand | Weitere Angaben | Entscheidung |
|---|---|---|
| `AS_NEW` | Etikett vorhanden, Originalverpackung vorhanden | `ACCEPT` |
| `AS_NEW` | Etikett vorhanden, Originalverpackung fehlt | `ACCEPT` (mit niedrigerem Score) |
| `AS_NEW` | Etikett fehlt | `MANUAL_REVIEW` |
| `MINOR_WEAR` | – | `MANUAL_REVIEW` |
| `MISSING_TAGS` | – | `MANUAL_REVIEW` |
| `VISIBLY_WORN` | – | `REJECT` |
| `DAMAGED` | – | `REJECT` |
| `MISSING_PARTS` | – | `REJECT` |

Der `confidence_score` ist bei eindeutigen Fällen hoch und bei Grenzfällen niedrig. Bei `MANUAL_REVIEW` ist `requires_manual_review` gleich `true`. Damit kann der Dozent verschiedene Szenarien sehen: Annahme, Ablehnung, Grenzfall, Fehler.

### 5.2 Fehler im gRPC-Service (Vorschlag)

| Fall | gRPC-Statuscode |
|---|---|
| `return_id` oder `sku` leer, Zustand `UNSPECIFIED` | `INVALID_ARGUMENT` |
| Dieselbe Prüfung wird zweimal gesendet | `ALREADY_EXISTS` |
| `GetCheckResult` für eine unbekannte Prüfung | `NOT_FOUND` |
| Simulierter Ausfall des Services | `UNAVAILABLE` (der Client wiederholt den Aufruf) |

### 5.3 Statusübergänge (Vorschlag, aus dem Soll-Prozess abgeleitet)

```
NEW → APPROVED | REJECTED | IN_REVIEW
APPROVED → LABEL_SENT → RECEIVED | EXPIRED
RECEIVED → INSPECTED → ACCEPTED | REJECTED | MANUAL_REVIEW
ACCEPTED → REFUNDED → CLOSED
```

Bei der Umsetzung ergänzt (ebenfalls Vorschlag): `IN_REVIEW → APPROVED | REJECTED`, `MANUAL_REVIEW → ACCEPTED | REJECTED`, `REJECTED | EXPIRED → CLOSED`. Sonst gäbe es für Grenzfälle und abgelehnte Retouren keinen Weg zum Ende.

- Eine Nachricht je Statuswechsel, mit `eventId`, `returnId`, `oldStatus`, `newStatus`, `occurredAt`.
- Ein Topic für Status-Updates, aus dem mehrere Empfänger lesen können (Kundeninformation, Reporting). Das trennt die Systeme voneinander.
- Fehler: Nachrichten, die der Consumer nicht versteht, landen in einer eigenen Fehler-Queue. Der Producer wiederholt das Senden, wenn der Broker nicht erreichbar ist.
- **Doppelte Events:** Der Consumer merkt sich die `eventId` und ignoriert eine Nachricht, die er schon verarbeitet hat. Er prüft außerdem, ob der Statuswechsel erlaubt ist.
- Die Statuswechsel mit Zeitstempel sind zugleich der Event-Log für Sprint 3 (Process Mining).

## 6. Abgleich mit dem Soll-Prozess aus Sprint 1

**Passt:**
- Der REST-Mock ist der Marktplatz-Eingang (`s_mkt`).
- Die drei Entscheidungen des gRPC-Service entsprechen den drei Wegen nach dem Gateway „Ergebnis?“ (`gw2`): Annahme, Ablehnung, Grenzfall. Ein Grenzfall geht zu „Grenzfall prüfen & Entscheidung dokumentieren“ (`t_man2`).
- Die Messaging-Schritte (`t_notify`, `t_notify_in`, `t_end_notify`) senden Statusnachrichten.

**Passt nicht ganz** (die Modelle bleiben unverändert; sie sind unsere eigene Arbeit, das Team entscheidet und passt sie in Sprint 4 an, wenn der ausführbare Workflow entsteht):
- In `soll-prozess-realisierung.md` „bucht“ der gRPC-Service den Zustand im WMS (`t_gr_rec`) und liefert die Nachricht „Paket eingetroffen“ (`ev_arrive`). Laut Aufgabe **bewertet** er den Zustand und gibt eine Empfehlung. Die Entscheidung trifft weiterhin die Regel `t_decide`.
- Der REST-Mock wird laut Aufgabe **abgerufen** (der Workflow holt die Meldungen). Das Modell zeigt eine eingehende Nachricht.
- Vorschlag: Die Modelle ändern wir erst in Sprint 4, wenn der ausführbare Workflow entsteht. Bis dahin steht die Abweichung hier.

## 7. Fragen an den Dozenten

Die Fragen 1 bis 3 hat der Dozent beantwortet, die Fragen 4 bis 6 sind noch offen.

1. Die Bewertungsfolie nennt „Payment“ und „Rechnungen“. Dürfen wir die Aspekte sinngemäß übertragen: Event für den Status einer Retoure statt Payment, Zustandsprüfung eines Artikels statt Rechnungserfassung?
   **Antwort: Das ist eine alte Folie, einfach ignorieren.**
2. Änderungen an der `.proto`: Wir haben nur zwei Java-Optionen ergänzt (`java_package`, `java_multiple_files`). Ist das in Ordnung? Die Stapelprüfung hat kein Fehlerfeld je Position. Wir lehnen bei einem Fehler den ganzen Stapel ab und nennen die Position in der Meldung. Reicht das, oder sollen wir ein Fehlerfeld je Position ergänzen?
   **Antwort: Ja, das passt.**
3. REST-Mock: Das Beispiel zeigt nur Retourenmeldungen. Für die Bestelldaten (Abfrage über die `orderId`) haben wir ein eigenes Format entworfen. Gibt es Vorgaben, oder ist es frei?
   **Antwort: Wir können ein eigenes, sinnvolles Format wählen.**
4. Entkopplung: Der Consumer hält den Status in einem eigenen Speicher und ruft den REST-Mock nicht auf. Verbunden werden die Systeme erst im Workflow (Sprint 4). Ist das gewünscht, oder soll der Consumer den Status schon jetzt im REST-Mock aktualisieren?
5. Abgabe: Wir zeigen die Szenarien live im 20-Minuten-Termin (Ablauf in `vorfuehrung.md`). Was laden wir in ILIAS hoch, reicht der Link zum GitHub-Repository? Unter welchem GitHub-Namen laden wir den Dozenten als Mitglied ein?
6. Aus Sprint 1, noch offen: Ab wann zählt die Retourenfrist „8–12 Tage“, und in Kalender- oder Werktagen?

## 8. Offen im Team

- Programmiersprache und Broker: **Java (Spring Boot) und RabbitMQ**, festgelegt von Yaraslau Krautsou am 22.10.2026. Die Zustimmung des Teams steht noch aus. Wir müssen den Code erklären können.
- Aufgaben verteilen: Der Code für alle drei Programme liegt vor (Claude Code, Co-Autor Yaraslau Krautsou). Offen ist, wer ihn liest, ausprobiert und später vorstellt (siehe Prüfliste in `AI-USAGE.md`).
- Prüfen, ob die Statusübergänge in 5.3 und die Regeln in 5.1 passen.
- Vorstellung im 20-Minuten-Termin: Wer zeigt welchen Teil, steht noch offen (Tabelle in `vorfuehrung.md`). Das Team spielt die Vorführung vorher einmal komplett durch.
