# Project Charter – ShopEasy Retourenabwicklung (ENTWURF)

> Entwurf: Mit `TODO` markierte Felder muss das Team gemeinsam festlegen.

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
| Rolle | Person | Aufgabe |
|---|---|---|
| Projektleitung / Koordination | TODO | Termine, ILIAS-Abgaben |
| BPM / Modellierung | TODO | Sprint 1, 3 |
| Integration | TODO | Sprint 2 |
| Workflow | TODO | Sprint 4 |
| RPA / AI | TODO | Sprint 5 |
| Dokumentation | TODO | Sprint 6 |

Jede Person ist bei mindestens einem Sprint Hauptverantwortliche:r; Beiträge bleiben über Commits nachvollziehbar.

## 5. Zeitrahmen und Meilensteine
Abgaben jeweils bis 11:30 in ILIAS: 20.10. (S1), 3.11. (S2), 17.11. (S3), 8.12. (S4), 12.1. (S5), 26.1. (S6).

## 6. Kommunikationsregeln
- Meeting-Takt: TODO (Vorschlag: 1× pro Woche, 30 Min.)
- Tools: GitHub (Code/Doku), TODO (Chat)
- Antwortzeit: TODO (Vorschlag: 24 h)
- Kurzprotokoll pro Meeting im Repo oder Chat

## 7. Entscheidungsregeln
Konsens; bei Uneinigkeit Mehrheitsentscheid, bei Gleichstand entscheidet die Projektleitung. Fachliche Änderungen an abgestimmten Artefakten nur mit Info an das Team.

## 8. Risiken und Annahmen
**Top-3-Risiken:** (1) Ungleiche Arbeitsverteilung; (2) Toolprobleme (Camunda, UiPath, Celonis-Zugang); (3) Zeitdruck wegen paralleler Prüfungen.
**Annahmen:** Event-Log für Sprint 3 wird vom Dozenten bereitgestellt; alle Tools sind kostenlos/Lizenz verfügbar.

## Teamerwartungen
TODO: Ziele und Erwartungen der einzelnen Mitglieder (Notenziel, Zeitbudget) gemeinsam sammeln.

## Regeln zur KI-Nutzung
Jede KI-Nutzung wird in `AI-USAGE.md` dokumentiert. Die persönliche Zusammenfassung in Sprint 6 entsteht ohne KI.
