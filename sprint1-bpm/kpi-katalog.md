# KPI-Katalog

Die KPIs werden in Sprint 3 aus dem Event-Log mit Celonis berechnet. Sie sind deshalb als Messung über Aktivitäten und Zeitstempel definiert.

**Wichtig zu den Werten**
- Aus der Aufgabenstellung bekannt sind nur zwei Ist-Werte: **Retourenquote 25–35 %** und **Durchlaufzeit bis zur Rückerstattung 8–12 Tage**. Alle übrigen Ist-Werte werden erst in Sprint 3 gemessen.
- Der **Startpunkt der 8–12 Tage** nennt die Aufgabe nicht (ab Anmeldung oder ab Wareneingang, Kalender- oder Arbeitstage?). Das klären wir mit dem Dozenten; K1a und K1b messen beide Varianten.
- **Zielwerte sind Vorschläge der KI, im Team abzustimmen** (in der Spalte „Ziel“ mit *V* markiert, Prüfliste in `sprint1-bpm/AI-USAGE.md`). Sie werden in Sprint 3 mit den gemessenen Ist-Werten abgeglichen.
- Das Event-Log liegt noch nicht vor (Aufgabe: Bereitstellung vor Sprint 3). Die Aktivitätsnamen unten sind Annahmen und werden dann angepasst; offene Fragen an den Dozenten stehen in Abschnitt 11.
- **Pflicht in Sprint 3** sind die Kern-KPIs K1–K8 (Abschnitt 3). Die Zusatz-KPIs K9–K21 (Abschnitt 5) werden berechnet, soweit das Log es hergibt.

## 1. Minimales Event-Log-Schema

| Attribut | Bedeutung |
|---|---|
| Fall-ID | Retouren-ID (ein Fall = eine Retoure) |
| Aktivität | siehe Zuordnung in Abschnitt 7 |
| Zeitstempel | Zeitpunkt des Abschlusses der Aktivität |
| Ressource | Person, System oder Bot (für den Automatisierungsgrad) |
| Kanal | Webshop / Marktplatz / E-Mail |
| Retourengrund, Zustand, Entscheidung, Betrag, Erstattungsart | Fallattribute, falls im Log vorhanden |

**Hinweis zum Zeitstempel:** Mit nur einem Zeitstempel je Aktivität enthält jede Zeit zwischen zwei Aktivitäten Warten **und** Bearbeiten. Eine reine Wartezeit oder Servicezeit (Begriffe der Vorlesung) lässt sich nur messen, wenn das Log Start und Ende je Aktivität führt (Frage 2 in Abschnitt 11).

## 2. Berechnungsregeln für alle KPIs

- **Grundgesamtheit:** alle Fälle im Auswertungszeitraum. Zeitkennzahlen (K1a, K1b, K2, K3, K8, K9) zählen nur **abgeschlossene** Fälle. Offene Fälle und Fälle ohne Wareneingang (K14) werden gesondert ausgewiesen, sonst fallen die langen Fälle heraus und die Zeiten sehen zu gut aus.
- **Zeit:** Kalendertage bzw. Stunden; zusätzlich Arbeitstage (Montag bis Freitag), damit der Vergleich mit den „8–12 Tagen“ der Aufgabe in beiden Zählweisen möglich ist.
- **Lage:** Median und 90. Perzentil (robust gegen Ausreißer), zusätzlich der Mittelwert, weil die Aufgabe eine Spanne nennt.
- **Aufschlüsselung:** je Kanal (K7), nach Entscheidung (angenommen, abgelehnt) und je Monat (Trend).
- **Stichtag:** Auswertung zum Exportdatum; Fälle ohne Abschlussaktivität zählen als offen. Ausreißer (über 90 Tage) werden geprüft, nicht gestrichen.
- **Dokumentation:** Filter und Berechnungsweg stehen je KPI in der Vorlage (Abschnitt 10).

## 3. Kern-KPIs

Die Perspektive folgt den Kennzahlenfamilien der Vorlesung (Dumas): Effektivität, Zeit, Kosten, Qualität, Flexibilität.

| Nr. | KPI | Perspektive | Definition und Berechnung | Messpunkte (Start → Ende) | Einheit | Datenquelle | Aus Log? | Schwachstelle | Ziel (V) |
|---|---|---|---|---|---|---|---|---|---|
| K1a | Durchlaufzeit gesamt | Zeit | Median und 90. Perzentil der Zeit je abgeschlossenem Fall, einschließlich Rückversand durch die Kund:in. Bei Ablehnung endet der Fall mit der Mitteilung an den Kunden. | `Retoure angemeldet` → `Rückerstattung` (bzw. `Kunde informiert` bei Ablehnung) | Tage | Event-Log | ja | W12, alle | kein fester Zielwert, Vergleich mit Ist: 8–12 Tage laut Aufgabe (Startpunkt offen) |
| K1b | Interne Durchlaufzeit | Zeit | Median und 90. Perzentil ab Wareneingang. Dieser Teil ist für ShopEasy steuerbar, der Rückversand nicht. | `Wareneingang` → `Rückerstattung` (bzw. `Kunde informiert` bei Ablehnung) | Tage | Event-Log | ja | W7, W8, W12 | Median ≤ 1 Tag, 90. Perzentil ≤ 3 Tage |
| K2 | Zeit Wareneingang → Zustandserfassung | Zeit | Median und 90. Perzentil. Im Ist-Log vermutlich derselbe WMS-Eintrag, dann Ersatzmessung (Abschnitt 6). | `Wareneingang` → `Zustand erfasst` | Stunden | Event-Log | ggf. | W7, W15 | Median ≤ 4 Stunden |
| K3 | Zeit Entscheidung → Rückerstattung | Zeit | Median und 90. Perzentil, nur Fälle mit Annahme. Die Entscheidung ist im Ist nicht dokumentiert (W8), dann Ersatzmessung. | `Entscheidung` → `Rückerstattung` | Stunden | Event-Log | ggf. | W12 | Median ≤ 1 Stunde (Soll: automatische Erstattung) |
| K4 | Eskalationsquote | Qualität | Fälle mit Aktivität `Eskalation` geteilt durch alle Fälle mit `Entscheidung`. Die Eskalation läuft im Ist per E-Mail, dann Ersatzmessung. | – | % | Event-Log | ggf. | W8, W9, W16 | ≤ 5 % |
| K5 | Fehlerquote (drei Teile, siehe Abschnitt 4) | Qualität | K5a Erfassungsfehler, K5b Nacharbeit, K5c Fehlentscheidung | – | % | Event-Log, Ersatzmessung | ggf. | W2, W3, W8, W11 | je ≤ 2 % |
| K6 | Automatisierungsgrad | Kosten | Anteil der Aktivitäten, deren Ressource ein System oder Bot ist, an allen Aktivitäten | – | % | Event-Log (Ressource) | ggf. | W16, alle | ≥ 60 % (im Soll-Modell: 16 von 22 Aufgaben automatisch, im Normalfall 11 von 12) |
| K7 | Kanalverteilung und Durchlaufzeit je Kanal | Effektivität | Anteil der Fälle je Kanal; K1a und K1b getrennt je Kanal | `Retoure angemeldet` (Kanal) | % und Tage | Event-Log | ja | W1, W2 | Ziel: E-Mail-Kanal nicht langsamer als Webshop |
| K8 | Zeit bis zur ersten Statusnachricht an den Kunden | Effektivität | Median der Zeit bis zur ersten Nachricht an den Kunden | `Retoure angemeldet` → erste Aktivität `Label versendet` oder `Kunde informiert` | Stunden | Event-Log | ggf. | W6, W13 | ≤ 1 Stunde |

**Konsistenz der Zielwerte:** K1b entspricht ungefähr K2 + Zeit bis zur Entscheidung + K3. Die Vorschläge (K2 ≤ 4 Stunden, K3 ≤ 1 Stunde) lassen für die Entscheidung genug Raum innerhalb von einem Tag (Median).

## 4. Fehlerquote: Definition

Die Fehlerquote wird in drei Teile zerlegt, weil „Fehler“ sonst nicht eindeutig messbar ist.

| Teil | Definition | Berechnung | Ersatzmessung, falls das Log es nicht hergibt |
|---|---|---|---|
| K5a Erfassungsfehlerquote | Fälle, in denen nach der Erfassung Daten korrigiert oder nachgefragt werden mussten | Fälle mit Aktivität `Daten korrigiert` oder `Rückfrage` geteilt durch alle Fälle | Fälle, in denen `Anmeldung erfasst` mehr als einmal vorkommt |
| K5b Nacharbeitsquote | Fälle, in denen irgendeine Aktivität wiederholt wird (Schleifen, z. B. Eskalation und erneute Entscheidung) | Fälle mit einer Aktivität ≥ 2 Mal geteilt durch alle Fälle | – |
| K5c Fehlentscheidungsquote | Fälle, in denen die Entscheidung nachträglich geändert wurde (z. B. Erstattung nach Ablehnung) | Fälle mit mehr als einer unterschiedlichen `Entscheidung` oder mit `Rückerstattung` nach Ablehnung geteilt durch alle Fälle mit Entscheidung | Reklamationen nach Ablehnung (aus dem E-Mail-Postfach, falls verfügbar) |

## 5. Zusatz-KPIs

| Nr. | KPI | Definition und Berechnung | Messpunkte | Einheit | Aus Log? | Schwachstelle | Ziel (V) |
|---|---|---|---|---|---|---|---|
| K9 | Zeit Anmeldung → Label | Median und 90. Perzentil | `Retoure angemeldet` → `Label erstellt` | Stunden | ja | W3, W5 | ≤ 1 Stunde |
| K10 | Ablehnungsquote | Fälle mit Entscheidung „abgelehnt“ geteilt durch alle Fälle mit Entscheidung | – | % | ggf. (Attribut Entscheidung, sonst Ersatzmessung) | W8 | Beobachtung |
| K11 | Verteilung der Retourengründe | Anzahl Fälle je Retourengrund | – | Anzahl, % | ggf. (Attribut Grund) | W17 | Beobachtung |
| K12 | Verdachtsquote | Fälle mit Betrugsscore über Schwellwert; davon im Nachhinein bestätigt | – | % | nein im Ist (nicht erfasst), im Soll ja | W10 | Beobachtung |
| K13 | Erstattung vor Zustandserfassung | Fälle, in denen `Rückerstattung` vor `Zustand erfasst` liegt | – | Anzahl | ja | W7, W10, W12 | 0 |
| K14 | Nicht eingegangene Retouren | Fälle mit `Label erstellt`, aber ohne `Wareneingang` nach 21 Tagen zum Stichtag (im Ist-Prozess gibt es keine Frist, im Soll-Modell 21 Tage als Vorschlag) | `Label erstellt` → `Wareneingang` | Anzahl, % | ja | W6 | Beobachtung |
| K15 | Retourenaufkommen je Tag | Fälle mit `Wareneingang` je Kalendertag und Wochentag; Spitze geteilt durch Durchschnitt | – | Anzahl | ja | W15 | Basis für Schichtplanung, kein Zielwert |
| K16 | Kundenanfragen zum Status | Anzahl Rückfragen je 100 Fälle | – | Anzahl | nein (E-Mail-Postfach) | W13 | Vorschlag: halbieren |
| K17 | Retourenquote | Retouren geteilt durch Bestellungen im Zeitraum | – | % | nein (braucht Bestelldaten) | Reporting | Beobachtung (Ist: 25–35 % laut Aufgabe) |
| K18 | Bearbeitungsaufwand je Retoure | Mittelwert der Aktivitäten je Fall, deren Ressource eine Person ist. Geschätzte Kosten = Anzahl × Minuten je Aktivität × Stundensatz (beides Annahmen, in Sprint 3 abgleichen) | – | Anzahl, € (geschätzt) | ggf. (Ressource) | W3, W9, W11, W12, W16 | Beobachtung; im Soll-Modell sind 6 von 22 Aufgaben menschlich, im Normalfall (Webshop, Annahme) nur die Zustandsprüfung |
| K19 | Änderungsaufwand für neue Regel oder neuen Kanal | qualitativ: Änderung einer DMN-Tabelle bzw. eines Startereignisses statt Codeänderung | – | Aufwand (Stunden, geschätzt) | nein | W1, W4, W16 | Nachweis in Sprint 4 (Regel ändern, Testlauf wiederholen) |
| K20 | Prozessvarianten | Anzahl der Varianten (verschiedene Aktivitätsfolgen) und Anteil der drei häufigsten Varianten an allen Fällen; zeigt den Grad der Standardisierung. Sprint 3 verlangt die „Identifikation der tatsächlichen Prozess-Varianten“ | – | Anzahl, % | ja | W1, W2, W9, W16 | Beobachtung; im Soll wenige, bekannte Varianten |
| K21 | Bestandskorrekturquote | Fälle mit Aktivität `Bestandskorrektur` oder mit mehr als einer `Bestand gebucht` geteilt durch alle Fälle mit `Bestand gebucht` | – | % | ggf. (ERP-Log) | W11 | ≤ 2 % |

### Zuordnung zu den Kennzahlenfamilien

| Familie | KPIs |
|---|---|
| Effektivität | K7, K8, K11, K16, K17 |
| Zeit | K1a, K1b, K2, K3, K9, K14, K15 |
| Kosten | K6, K18 |
| Qualität | K4, K5a–c, K10, K12, K13, K21 |
| Flexibilität | K19, K20 |

## 6. Ersatzmessungen, wenn das Ist-Log eine Aktivität nicht enthält

Das Ist-Log enthält nur, was die Systeme der Aufgabe schreiben (ERP, Versandtool, WMS, Zahlungsdienstleister, CRM). Entscheidungen und Eskalationen werden laut Aufgabe nicht in einem System dokumentiert.

| KPI | Problem im Ist-Log | Ersatzmessung | Grenze |
|---|---|---|---|
| K2 | `Wareneingang` und `Zustand erfasst` sind vermutlich derselbe WMS-Eintrag (Schritt 5) | Verteilung der Zeitstempel von `Zustand erfasst` nach Uhrzeit und Wochentag: Häufung zu festen Zeiten zeigt Sammelerfassung; zusätzlich `Label versendet` → `Zustand erfasst` als Gesamtzeit aus Rückversand und Erfassung | die Verzögerung selbst wird nicht direkt gemessen |
| K3 | `Entscheidung` ist nicht dokumentiert (W8) | `Bestand gebucht` → `Rückerstattung` (im Ist folgt die Bestandsbuchung der Annahme); alternativ `Zustand erfasst` → `Rückerstattung` | enthält die Zeit bis zur Bestandsbuchung |
| K4 | Eskalation läuft per E-Mail (Schritt 6) | Fälle mit auffällig langer Zeit `Zustand erfasst` → `Bestand gebucht` (über dem 90. Perzentil) als Näherung; Stichprobe aus dem E-Mail-Postfach | grobe Näherung |
| K8 | `Kunde informiert` per E-Mail ist evtl. nicht im Log | K9 (Zeit bis `Label erstellt`) als Obergrenze | im Soll liefert `t_notify` den Wert direkt |
| K10 | Ablehnung ist keine Aktivität | abgeschlossene Fälle mit `Zustand erfasst`, aber ohne `Bestand gebucht` und ohne `Rückerstattung` zählen als nicht angenommen | offene Fälle ausschließen |

## 7. Zuordnung Aktivität im Log ↔ Element im Modell

Die Namen in der ersten Spalte sind angenommen und werden mit dem tatsächlichen Log abgeglichen. Im Ist-Modell sind `Zustand geprüft` und `Zustand erfasst` derselbe Schritt (`t_gr`).

| Aktivität (Log, angenommen) | Ist-Modell | Soll-Modell |
|---|---|---|
| `Retoure angemeldet` | `s_web`, `s_mkt`, `s_mail` | `s_web`, `s_mkt`, `s_mail` |
| `Anmeldung erfasst` | `t_erp` | `t_create` |
| `Anmeldung geprüft` | `t_erp`, `gw_ok` | `t_rule` |
| `Anmeldung manuell geprüft` | – | `t_man1` |
| `Label erstellt` | `t_label` | `t_label` |
| `Label manuell erstellt` | – | `t_label_man` |
| `Label versendet` | `t_send` | `t_notify` |
| `Wareneingang bestätigt` | – | `t_notify_in` |
| `Wareneingang` | `ev_wait` | `ev_arrive` |
| `Zustand geprüft` | `t_gr` | `t_gr` |
| `Zustand erfasst` | `t_gr` | `t_gr_rec` |
| `Entscheidung` | `t_dec` | `t_decide` |
| `Eskalation` | `t_esc` | `t_man2` |
| `Bestand gebucht` / `Bestandskorrektur` | `t_stock` | `t_stock` |
| `Rückerstattung` / Gutschein | `t_pay`, `t_voucher` | `t_refund` |
| `Rückerstattung manuell geklärt` | – | `t_refund_man` |
| `Weiterverwendung festgelegt` | `t_reject` | `t_reject` |
| `Kunde informiert` | `t_comm`, `t_rej` | `t_end_notify`, `t_deny`, `t_deny2` |
| `Retoure abgeschlossen` | `end` | `end` |

Menschliche Aufgaben im Soll-Modell (für K6 und K18): `t_gr`, `t_man1`, `t_man2`, `t_reject`, `t_label_man`, `t_refund_man`.

## 8. Abdeckung der Schwachstellen

| Schwachstelle | KPI |
|---|---|
| W1 Kanäle | K7, K20 |
| W2, W3 Erfassung | K5a, K7 |
| W4 Prüfung | K5c, K10 |
| W5 Label | K9 |
| W6 Rückversand | K8, K14 |
| W7 Wareneingang | K2, K13 |
| W8, W9 Entscheidung, Eskalation | K4, K5c |
| W10 Betrug | K12, K13 |
| W11 Bestandsbuchung | K21 (Bestandskorrekturen im ERP-Log); Bestandsabweichungen selbst stehen im ERP, nicht im Log |
| W12 Rückerstattung | K1a, K1b, K3 |
| W13 Kommunikation | K8, K16 |
| W14 Reporting | qualitativ: Aufwand für das Excel-Reporting ist nicht im Log messbar; im Soll ersetzt durch den Ablauf `t_report` und das Dashboard |
| W15 Aufkommen | K15 |
| W16 Standardablehnungen | K4, K6, K18 |
| W17 Retourengrund | K11 |

## 9. Messung im Soll-Prozess

Im Soll-Prozess entstehen die Zeitstempel aus den Status-Events (Message Broker, Sprint 2) und den Camunda-Ereignissen. Der Ablauf `t_report` (Lane Management / Controlling) berechnet daraus die Kennzahlen und stellt sie im Dashboard bereit, statt monatlich in Excel. K1a bis K4 und K8 ergeben sich unmittelbar aus den Statuswechseln, K6 und K18 aus der Art der Aufgabe (Service, Regel, Bot oder Mensch), K20 aus der Variantenanalyse.

## 10. Vorlage zum Befüllen in Sprint 3

„Pflicht“: Kern-KPI, in Sprint 3 zu berechnen. „Optional“: soweit das Log es hergibt.

| KPI | Pflicht / Optional | Zeitraum / Filter | Ist-Wert | Ziel (V) | Datum der Berechnung | Berechnungsweg (Celonis) | Bemerkung |
|---|---|---|---|---|---|---|---|
| K1a | Pflicht | | | kein Zielwert (Vergleich 8–12 Tage) | | | |
| K1b | Pflicht | | | Median ≤ 1 Tag, P90 ≤ 3 Tage | | | |
| K2 | Pflicht | | | Median ≤ 4 Stunden | | | |
| K3 | Pflicht | | | Median ≤ 1 Stunde | | | |
| K4 | Pflicht | | | ≤ 5 % | | | |
| K5a | Pflicht | | | ≤ 2 % | | | |
| K5b | Pflicht | | | ≤ 2 % | | | |
| K5c | Pflicht | | | ≤ 2 % | | | |
| K6 | Pflicht | | | ≥ 60 % | | | |
| K7 | Pflicht | | | E-Mail nicht langsamer als Webshop | | | |
| K8 | Pflicht | | | ≤ 1 Stunde | | | |
| K9 | Optional | | | ≤ 1 Stunde | | | |
| K10 | Optional | | | Beobachtung | | | |
| K11 | Optional | | | Beobachtung | | | |
| K12 | Optional | | | Beobachtung | | | |
| K13 | Optional | | | 0 | | | |
| K14 | Optional | | | Beobachtung | | | |
| K15 | Optional | | | kein Zielwert | | | |
| K16 | Optional | | | halbieren | | | |
| K17 | Optional | | | Beobachtung (Ist 25–35 %) | | | |
| K18 | Optional | | | Beobachtung | | | |
| K19 | Optional | | | Nachweis Sprint 4 | | | |
| K20 | Optional | | | Beobachtung | | | |
| K21 | Optional | | | ≤ 2 % | | | |

## 11. Offene Fragen zum Log (an den Dozenten)

1. Welche Spalten hat das Log (Fall-ID, Aktivität, Zeitstempel, Ressource, Kanal, Fallattribute)?
2. Gibt es Start- und Endzeit je Aktivität oder nur einen Zeitstempel?
3. Welcher Zeitraum und wie viele Fälle; enthält das Log offene Fälle?
4. Sind `Entscheidung`, `Eskalation` und `Kunde informiert` als Aktivität enthalten oder nur die Systemereignisse aus ERP, WMS, Zahlungsdienstleister und CRM?
5. Wie ist die Ressource gekennzeichnet (Person, System, Bot)?
6. Ab wann zählen die „8–12 Tage“ (Anmeldung oder Wareneingang) und in Kalender- oder Arbeitstagen?
