# KPI-Katalog

Die KPIs werden in Sprint 3 aus dem Event-Log mit Celonis berechnet. Sie sind deshalb als Messung über Aktivitäten und Zeitstempel definiert.

**Wichtig zu den Werten**
- Aus der Aufgabenstellung bekannt sind nur zwei Ist-Werte: **Retourenquote 25–35 %** und **Durchlaufzeit bis zur Rückerstattung 8–12 Tage**. Alle übrigen Ist-Werte werden erst in Sprint 3 gemessen.
- **Zielwerte sind Vorschläge** des Teams (in der Spalte „Ziel“ mit *V* markiert) und werden in Sprint 3 mit den gemessenen Ist-Werten abgeglichen.
- Das Event-Log liegt noch nicht vor (Aufgabe: Bereitstellung vor Sprint 3). Die Aktivitätsnamen unten sind Annahmen und werden dann angepasst.

## 1. Minimales Event-Log-Schema

| Attribut | Bedeutung |
|---|---|
| Fall-ID | Retouren-ID (ein Fall = eine Retoure) |
| Aktivität | siehe Zuordnung in Abschnitt 5 |
| Zeitstempel | Zeitpunkt des Abschlusses der Aktivität |
| Ressource | Person, System oder Bot (für den Automatisierungsgrad) |
| Kanal | Webshop / Marktplatz / E-Mail |
| Retourengrund, Zustand, Entscheidung, Betrag, Erstattungsart | Fallattribute, falls im Log vorhanden |

## 2. Kern-KPIs

| Nr. | KPI | Perspektive | Definition und Berechnung | Messpunkte (Start → Ende) | Einheit | Datenquelle | Aus Log? | Schwachstelle | Ziel (V) |
|---|---|---|---|---|---|---|---|---|---|
| K1 | Durchlaufzeit gesamt | Zeit | Median und 90. Perzentil der Zeit je Fall. Bei Ablehnung endet der Fall mit der Mitteilung an den Kunden. | `Retoure angemeldet` → `Rückerstattung` (bzw. `Kunde informiert` bei Ablehnung) | Tage | Event-Log | ja | W12, alle | ≤ 3 Tage (Ist: 8–12 Tage laut Aufgabe) |
| K2 | Zeit Wareneingang → Zustandserfassung | Zeit | Median und 90. Perzentil | `Wareneingang` → `Zustand erfasst` | Stunden | Event-Log | ja | W7, W15 | ≤ 4 Stunden |
| K3 | Zeit Entscheidung → Rückerstattung | Zeit | Median und 90. Perzentil, nur Fälle mit Annahme | `Entscheidung` → `Rückerstattung` | Stunden | Event-Log | ja | W12 | ≤ 24 Stunden |
| K4 | Eskalationsquote | Qualität | Fälle mit Aktivität `Eskalation` geteilt durch alle Fälle mit `Entscheidung` | – | % | Event-Log | ja | W8, W9, W16 | ≤ 5 % |
| K5 | Fehlerquote (drei Teile, siehe Abschnitt 3) | Qualität | K5a Erfassungsfehler, K5b Nacharbeit, K5c Fehlentscheidung | – | % | Event-Log, Ersatzmessung | ggf. | W2, W3, W8, W11 | je ≤ 2 % |
| K6 | Automatisierungsgrad | Automatisierung | Anteil der Aktivitäten, deren Ressource ein System oder Bot ist, an allen Aktivitäten | – | % | Event-Log (Ressource) | ggf. | W16, alle | ≥ 60 % (im Soll-Modell: 13 von 19 Aufgaben automatisch) |
| K7 | Kanalverteilung und Durchlaufzeit je Kanal | Transparenz | Anteil der Fälle je Kanal; K1 getrennt je Kanal | `Retoure angemeldet` (Kanal) | % und Tage | Event-Log | ja | W1, W2 | Ziel: E-Mail-Kanal nicht langsamer als Webshop |
| K8 | Zeit bis zur ersten Statusnachricht an den Kunden | Kundensicht | Median der Zeit bis zur ersten Nachricht an den Kunden | `Retoure angemeldet` → erste Aktivität `Label versendet` oder `Kunde informiert` | Stunden | Event-Log | ggf. | W6, W13 | ≤ 1 Stunde |

## 3. Fehlerquote: Definition

Die Fehlerquote wird in drei Teile zerlegt, weil „Fehler“ sonst nicht eindeutig messbar ist.

| Teil | Definition | Berechnung | Ersatzmessung, falls das Log es nicht hergibt |
|---|---|---|---|
| K5a Erfassungsfehlerquote | Fälle, in denen nach der Erfassung Daten korrigiert oder nachgefragt werden mussten | Fälle mit Aktivität `Daten korrigiert` oder `Rückfrage` geteilt durch alle Fälle | Fälle, in denen `Anmeldung erfasst` mehr als einmal vorkommt |
| K5b Nacharbeitsquote | Fälle, in denen irgendeine Aktivität wiederholt wird (Schleifen, z. B. Eskalation und erneute Entscheidung) | Fälle mit einer Aktivität ≥ 2 Mal geteilt durch alle Fälle | – |
| K5c Fehlentscheidungsquote | Fälle, in denen die Entscheidung nachträglich geändert wurde (z. B. Erstattung nach Ablehnung) | Fälle mit mehr als einer unterschiedlichen `Entscheidung` oder mit `Rückerstattung` nach Ablehnung geteilt durch alle Fälle mit Entscheidung | Reklamationen nach Ablehnung (aus dem E-Mail-Postfach, falls verfügbar) |

## 4. Zusatz-KPIs

| Nr. | KPI | Definition und Berechnung | Messpunkte | Einheit | Aus Log? | Schwachstelle | Ziel (V) |
|---|---|---|---|---|---|---|---|
| K9 | Zeit Anmeldung → Label | Median und 90. Perzentil | `Retoure angemeldet` → `Label erstellt` | Stunden | ja | W3, W5 | ≤ 1 Stunde |
| K10 | Ablehnungsquote | Fälle mit Entscheidung „abgelehnt“ geteilt durch alle Fälle mit Entscheidung | – | % | ja (Attribut Entscheidung) | W8 | Beobachtung |
| K11 | Verteilung der Retourengründe | Anzahl Fälle je Retourengrund | – | Anzahl, % | ggf. (Attribut Grund) | W17 | Beobachtung |
| K12 | Verdachtsquote | Fälle mit Betrugsscore über Schwellwert; davon im Nachhinein bestätigt | – | % | nein im Ist (nicht erfasst), im Soll ja | W10 | Beobachtung |
| K13 | Erstattung vor Zustandserfassung | Fälle, in denen `Rückerstattung` vor `Zustand erfasst` liegt | – | Anzahl | ja | W7, W10, W12 | 0 |
| K14 | Nicht eingegangene Retouren | Fälle mit `Label erstellt`, aber ohne `Wareneingang` nach 21 Tagen (im Ist-Prozess gibt es keine Frist, im Soll-Modell 21 Tage als Vorschlag) | `Label erstellt` → `Wareneingang` | Anzahl, % | ja | W6 | Beobachtung |
| K15 | Retourenaufkommen je Tag | Fälle mit `Wareneingang` je Kalendertag und Wochentag; Spitze geteilt durch Durchschnitt | – | Anzahl | ja | W15 | Basis für Schichtplanung, kein Zielwert |
| K16 | Kundenanfragen zum Status | Anzahl Rückfragen je 100 Fälle | – | Anzahl | nein (E-Mail-Postfach) | W13 | Vorschlag: halbieren |
| K17 | Retourenquote | Retouren geteilt durch Bestellungen im Zeitraum | – | % | nein (braucht Bestelldaten) | Reporting | Beobachtung (Ist: 25–35 % laut Aufgabe) |

## 5. Zuordnung Aktivität im Log ↔ Element im Modell

Die Namen in der ersten Spalte sind angenommen und werden mit dem tatsächlichen Log abgeglichen.

| Aktivität (Log, angenommen) | Ist-Modell | Soll-Modell |
|---|---|---|
| `Retoure angemeldet` | `s_web`, `s_mkt`, `s_mail` | `s_web`, `s_mkt`, `s_mail` |
| `Anmeldung erfasst` | `t_erp` | `t_create` |
| `Anmeldung geprüft` | `t_erp` | `t_rule` |
| `Label erstellt` | `t_label` | `t_label` |
| `Label versendet` | `t_send` | `t_notify` |
| `Wareneingang` | `ev_wait` | `ev_arrive` |
| `Zustand erfasst` | `t_gr` | `t_gr` |
| `Entscheidung` | `t_dec` | `t_decide` |
| `Eskalation` | `t_esc` | `t_man2` |
| `Bestand gebucht` | `t_stock` | `t_stock` |
| `Rückerstattung` / Gutschein | `t_pay`, `t_voucher` | `t_refund` |
| `Weiterverwendung festgelegt` | `t_reject` | `t_reject` |
| `Kunde informiert` | `t_comm` | `t_end_notify` |
| `Retoure abgeschlossen` | `end` | `end` |

## 6. Abdeckung der Schwachstellen

| Schwachstelle | KPI |
|---|---|
| W1 Kanäle | K7 |
| W2, W3 Erfassung | K5a, K7 |
| W4 Prüfung | K5c, K10 |
| W5 Label | K9 |
| W6 Rückversand | K8, K14 |
| W7 Wareneingang | K2, K13 |
| W8, W9 Entscheidung, Eskalation | K4, K5c |
| W10 Betrug | K12, K13 |
| W11 Bestandsbuchung | K5a (Bestandsabweichungen stehen im ERP, nicht im Log; qualitativ) |
| W12 Rückerstattung | K1, K3 |
| W13 Kommunikation | K8, K16 |
| W14 Reporting | qualitativ: Aufwand für das Excel-Reporting ist nicht im Log messbar; im Soll ersetzt durch Dashboard |
| W15 Aufkommen | K15 |
| W16 Standardablehnungen | K4, K6 |
| W17 Retourengrund | K11 |

## 7. Messung im Soll-Prozess

Im Soll-Prozess entstehen die Zeitstempel aus den Status-Events (Message Broker) und den Camunda-Ereignissen. Dieselben KPIs können damit laufend in einem Dashboard berechnet werden, statt monatlich in Excel. Besonders K1 bis K4 und K8 ergeben sich unmittelbar aus den Statuswechseln.

## 8. Vorlage zum Befüllen in Sprint 3

| KPI | Ist-Wert | Datum der Berechnung | Berechnungsweg (Celonis) | Bemerkung |
|---|---|---|---|---|
| K1 | | | | |
| K2 | | | | |
| K3 | | | | |
| K4 | | | | |
| K5a | | | | |
| K5b | | | | |
| K5c | | | | |
| K6 | | | | |
| K7 | | | | |
| K8 | | | | |
