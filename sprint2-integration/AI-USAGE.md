# KI-Nutzung – Sprint 2 (Integrationstechniken (REST, gRPC, Messaging), 20.10.–3.11.2026)

Laut Aufgabenstellung führt undokumentierte KI-Nutzung zum Nicht-Bestehen. Jeder Eintrag: Datum, Werkzeug, Co-Autor:in, Zweck, Prüfstatus. Übersicht über alle Sprints: [AI-USAGE.md](../AI-USAGE.md). GitHub-Repository: [https://github.com/krya1012/shopeasy-retouren](https://github.com/krya1012/shopeasy-retouren), diese Datei: [sprint2-integration/AI-USAGE.md](https://github.com/krya1012/shopeasy-retouren/blob/main/sprint2-integration/AI-USAGE.md).

**Co-Autorenschaft:** Einträge mit Claude Code entstehen in Sitzungen von **Yaraslau Krautsou** (Co-Autor von Claude Code). Andere Personen stehen je Eintrag in der Spalte „Co-Autor:in“.
**Modell:** Claude Code mit der Einstellung `opusplan` (Claude Opus 5.5 im Planungsmodus, sonst Claude Sonnet 5.5), sofern nicht anders eingetragen.

Einträge sofort beim Einsatz der KI anlegen, nicht erst zur Abgabe.

| Datum | Werkzeug | Co-Autor:in | Zweck / Artefakt | Prüfstatus (Team) |
|---|---|---|---|---|
| 20.10.2026 | Claude Code (Opus 5.5 im Planungsmodus, Sonnet 5.5) | Yaraslau Krautsou | `aufgabenanalyse.md`: Analyse der Sprint-2-Aufgabe (PDF) und der `.proto`, Abgleich mit dem Soll-Prozess, Vorschläge für Zustandsregeln, gRPC-Fehler und Statusübergänge, Fragen an den Dozenten | offen – siehe Prüfliste |
| 22.10.2026 | Claude Code (Opus 5.5 im Planungsmodus, Sonnet 5.5) | Yaraslau Krautsou | Code der drei Komponenten (Java/Spring Boot, RabbitMQ): `rest-mock/`, `grpc-service/` (mit Tests und Clients), `messaging-producer/`, `messaging-consumer/`, `compose.yaml`, `README.md`, Ergänzungen in `aufgabenanalyse.md`. Sprache und Broker hat Yaraslau Krautsou gewählt. | offen – siehe Prüfliste |
| 22.10.2026 | Claude Code (Opus 5.5 im Planungsmodus, Sonnet 5.5) | Yaraslau Krautsou | Abgleich des Codes mit der Aufgabe (PDF) und Korrekturen: REST-Liste im Feld `data`, Testdaten nach Folie 9, Clients mit eigenen Abfragen, Szenario „zu langsam“ (Frist) im gRPC-Service, OpenAPI-Beschreibung, Abschnitte „Abgleich mit der Aufgabe“ und „Enterprise Integration Patterns“ in der `README.md`, Fragenliste an den Dozenten (Abschnitt 7 in `aufgabenanalyse.md`) überarbeitet und um drei Fragen ergänzt, Antworten des Dozenten auf die Fragen 1 bis 3 eingetragen, Frage zu den BPMN-Modellen gestrichen (Entscheidung im Team) | offen – siehe Prüfliste |
| 22.10.2026 | Claude Code (Opus 5.5 im Planungsmodus, Sonnet 5.5) | Yaraslau Krautsou | `vorfuehrung.md`: Drehbuch für den 20-Minuten-Termin (Ablauf, Befehle, erwartete Ausgaben, mögliche Fragen) und kleine Anpassungen dafür: Testausgabe mit Testnamen, kurze Log-Zeilen in Consumer, Producer und gRPC-Server, Client-Option `UNSPECIFIED`. Der Ablauf wurde einmal komplett gegen den laufenden Aufbau durchgespielt. | offen – siehe Prüfliste |

## Prüfliste Sprint 2 (Team)

Hier steht erst ein Ergebnis, wenn jemand die Inhalte wirklich geprüft hat.

1. `aufgabenanalyse.md`: Alle lesen sie und prüfen, ob sie die Aufgabe (PDF) richtig wiedergibt. Die Vorschläge in Abschnitt 5 (Regeln, Fehler, Statusübergänge) sind Vorschläge der KI und müssen vom Team abgestimmt werden.
2. Die Fragen 1 bis 3 in Abschnitt 7 sind vom Dozenten beantwortet. Die Fragen 4 bis 6 stellt Yaraslau Krautsou dem Dozenten (oder jemand anderes aus dem Team).
3. Der Code der drei Komponenten: Alle lesen ihn und können erklären, was er tut (der Dozent stellt dazu Fragen). Jede Person startet die Demo selbst nach `README.md` und sieht, dass es funktioniert.
4. Das Team stimmt zu oder ändert: Java und RabbitMQ, die Zustandsregeln (README, Abschnitt 2), die Statuswechsel (README, Abschnitt 3) und die Annahme, dass der Consumer den REST-Mock nicht aufruft (Frage 4). „Alles oder nichts“ bei der Stapelprüfung, die Java-Optionen in der `.proto` und das Format der Bestelldaten hat der Dozent bestätigt.
5. Das Team legt fest, wer welche Komponente vorstellt (Tabelle in `vorfuehrung.md`), spielt die Vorführung einmal komplett durch und stoppt die Zeit.
