# Project Charter – ShopEasy Retourenabwicklung (ENTWURF)

> Stand 19.10.2026. Die Kommunikationsregeln sind als Vorschlag übernommen und im Team zu bestätigen. Product Owner und Erwartungen trägt das Team ein (mit `_…_` markierte Felder).

## 1. Projektname und Beschreibung
**ShopEasy Retouren digital.** Der teilweise manuelle, medienbruchbehaftete Retourenprozess der ShopEasy GmbH (Retourenquote 25–35 %, Rückerstattung nach 8–12 Tagen) wird als digitales Konzept entworfen und zentrale Bausteine werden prototypisch umgesetzt (BPMN, REST/gRPC/Messaging, Process Mining, Camunda-Workflow, RPA, AI-Agent).

## 2. Projektziele (SMART)
1. Bis 20.10.: Ist- und Soll-Prozess in BPMN, Schwachstellenanalyse und KPI-Katalog liegen vor und sind im Team abgestimmt.
2. Bis 3.11.: REST-Mock, gRPC-Service und Messaging-Statusupdates laufen lokal und sind per Tests oder Video demonstrierbar.
3. Bis 8.12.: Soll-Prozess läuft als Camunda-Workflow, bindet die Sprint-2-Services ein und hat mindestens 3 demonstrierbare Testläufe (Standard, Ablehnung, Fehlerfall).
4. Bis 12.1.: UiPath-Bot und n8n-AI-Agent sind implementiert und getestet; der Agent ist in Camunda eingebunden.
5. Bis 26.1.: Gesamtdokumentation und Abschlusspräsentation sind fertig; Code liegt auf GitHub.

## 3. Scope
**Im Scope:** Retourenprozess von Anmeldung bis Rückerstattung/Reporting, alle drei Eingangskanäle, Prototypen der Bausteine mit Mock-Systemen.
**Nicht im Scope:** Anbindung echter Amazon-/Zalando-/ERP-Systeme, Produktivbetrieb, echte Zahlungsabwicklung, rechtliche Detailprüfung (Widerrufsrecht).

## 4. Rollen und Verantwortlichkeiten
Der Kurs sieht zwei Rollen vor (Agile Vorlesung, Folie „Team und Rollen“): Product Owner und Mitarbeitende. Wir sind drei Personen und arbeiten jeden Sprint gemeinsam.

| Rolle | Person | Aufgabe |
|---|---|---|
| Product Owner | _Name eintragen_ | Product Backlog pflegen, Kontakt zum Dozenten, Abgabetermine im Blick behalten |
| Mitarbeitende | Eduard Ede Moser, Christian Sticker, Yaraslau Krautsou | bearbeiten alle Sprints gemeinsam |

Der Product Owner arbeitet wie die anderen als Mitarbeiter:in mit. Wechsel des Product Owners: _eintragen (gleichbleibend oder z. B. pro Sprint)_.
Beiträge bleiben über Commits unter dem eigenen GitHub-Konto nachvollziehbar.

## 5. Zeitrahmen und Meilensteine
Abgaben jeweils bis 11:30 in ILIAS: 20.10. (S1), 3.11. (S2), 17.11. (S3), 8.12. (S4), 12.1. (S5), 26.1. (S6).

## 6. Kommunikationsregeln
- Meeting-Takt: 1× pro Woche, 30 Minuten
- Tools: GitHub (Code/Doku), WhatsApp (Chat)
- Antwortzeit: innerhalb von 24 Stunden
- Kurzprotokoll pro Meeting im Chat oder im Repo
- Jede Person committet unter ihrem eigenen GitHub-Konto, damit Beiträge nachvollziehbar sind.

## 7. Entscheidungsregeln
Konsens; bei Uneinigkeit Mehrheitsentscheid, bei Gleichstand entscheidet der Product Owner. Fachliche Änderungen an abgestimmten Artefakten nur mit Info an das Team.

## 8. Risiken und Annahmen
**Top-3-Risiken:** (1) Ungleiche Arbeitsverteilung; (2) Toolprobleme (Camunda, UiPath, Celonis-Zugang); (3) Zeitdruck wegen paralleler Prüfungen.
**Annahmen:** Event-Log für Sprint 3 wird vom Dozenten bereitgestellt; alle Tools sind kostenlos/Lizenz verfügbar.

## Teamerwartungen
Jede Person trägt ihre eigenen Angaben ein (Projektregel: Ziele und Erwartungen gemeinsam klären).

| Person | Notenziel | Stunden pro Woche | Besonderheiten (z. B. Prüfungsphasen) |
|---|---|---|---|
| Eduard Ede Moser | _eintragen_ | _eintragen_ | _eintragen_ |
| Christian Sticker | _eintragen_ | _eintragen_ | _eintragen_ |
| Yaraslau Krautsou | _eintragen_ | _eintragen_ | _eintragen_ |

## Regeln zur KI-Nutzung
Jede KI-Nutzung wird in `AI-USAGE.md` dokumentiert. Die persönliche Zusammenfassung in Sprint 6 entsteht ohne KI.
