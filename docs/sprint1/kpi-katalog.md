# KPI-Katalog

Die KPIs sind so definiert, dass sie in Sprint 3 aus einem Event-Log berechnet werden können (Case-ID = Retouren-ID, Activity, Timestamp, Resource, Kanal). Zielwerte sind Annahmen, die nach Sprint 3 mit den Ist-Werten abgeglichen werden.

Angenommene Aktivitätsnamen im Event-Log (an die tatsächliche Log-Datei anpassen): `Retoure angemeldet`, `Anmeldung erfasst`, `Label erstellt`, `Wareneingang`, `Zustand erfasst`, `Entscheidung`, `Eskalation`, `Bestand gebucht`, `Rückerstattung`, `Kunde informiert`.

| Nr. | KPI | Definition / Formel über das Event-Log | Ist (Aufgabe) | Soll-Ziel | Bezug |
|---|---|---|---|---|---|
| K1 | Durchlaufzeit gesamt | Zeit von `Retoure angemeldet` bis `Rückerstattung` je Fall; Median und 90. Perzentil | 8–12 Tage | ≤ 3 Tage | W12, alle |
| K2 | Zeit Anmeldung → Label | `Label erstellt` − `Retoure angemeldet` | Sprint 3 | ≤ 1 Stunde | W3, W5 |
| K3 | Zeit Wareneingang → Zustandserfassung | `Zustand erfasst` − `Wareneingang` | Sprint 3 | ≤ 4 Stunden | W7 |
| K4 | Zeit Entscheidung → Rückerstattung | `Rückerstattung` − `Entscheidung` | Sprint 3 | ≤ 1 Tag | W12 |
| K5 | Eskalationsquote | Anteil Fälle mit Aktivität `Eskalation` an allen Fällen | Sprint 3 | ≤ 5 % | W8, W9 |
| K6 | Rework-Quote | Anteil Fälle, in denen eine Aktivität mehrfach vorkommt | Sprint 3 | ≤ 5 % | W2, W9 |
| K7 | Fehlerquote Erfassung | Anteil Fälle mit korrigierter oder nachgefragter Erfassung (Aktivität `Rückfrage`/`Korrektur`, falls im Log) | Sprint 3 | ≤ 2 % | W2, W3 |
| K8 | Retourenquote | Retouren geteilt durch Bestellungen im Zeitraum (Bestelldaten nötig) | 25–35 % | Beobachtung, kein Ziel | Reporting |
| K9 | Anteil je Kanal | Fälle je Kanal (Webshop / Marktplatz / E-Mail) geteilt durch alle Fälle; Durchlaufzeit je Kanal | Sprint 3 | Ziel: E-Mail-Anteil sinkt | W1 |
| K10 | Ablehnungsquote und Gründe | Anteil Fälle mit Entscheidung „abgelehnt“; Verteilung der Ablehnungs- und Retourengründe | Sprint 3 | Beobachtung | W8 |
| K11 | Automatisierungsgrad | Anteil automatisch ausgeführter Aktivitäten an allen Aktivitäten (Resource = System/Bot gegenüber Mensch) | niedrig (Schätzung) | ≥ 60 % | alle |
| K12 | Verdachtsquote | Anteil Fälle mit Betrugsscore über Schwellwert; davon bestätigte Fälle | nicht erfasst | wird erstmals messbar | W10 |
| K13 | Erstattung vor Zustandsprüfung | Fälle, in denen `Rückerstattung` vor `Zustand erfasst` liegt (Regelverstoß) | Sprint 3 | 0 | W7, W12 |
| K14 | Nicht eingegangene Retouren | Fälle mit `Label erstellt`, aber ohne `Wareneingang` nach 21 Tagen | Sprint 3 | Beobachtung | W6 |
| K15 | Kundenanfragen zum Status | Anzahl Rückfragen je 100 Fälle (E-Mail-Postfach, falls im Log) | „häufig“ | −50 % | W13 |
| K16 | Retourenaufkommen je Tag | Anzahl Fälle mit `Wareneingang` je Kalendertag und Wochentag; Spitze geteilt durch Durchschnitt | Sprint 3 | Basis für Schichtplanung, kein Zielwert | W15 |

## Hinweise
- K1–K4 sind die Kern-KPIs für den Vergleich Ist/Soll.
- Falls das Log von Sprint 3 einzelne Aktivitäten nicht enthält, wird die KPI mit einem Ersatzattribut berechnet oder in der Auswertung als „nicht berechenbar“ begründet.
- Im Soll-Prozess liefern die publizierten Status-Events (Messaging) dieselben Daten laufend, sodass die KPIs dauerhaft im Dashboard berechnet werden können.
