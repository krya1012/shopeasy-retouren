# Soll-Prozess: Realisierung der Schritte

Bezug: `sprint1-bpm/soll-prozess.bpmn` (Spalte „ID“ nennt die Elemente im Diagramm) und `sprint1-bpm/schwachstellenanalyse.md` (W-Nummern). Im Diagramm sind die Aufgaben nach Umsetzungsart eingefärbt: **blau** System/Service/Messaging, **grün** Regeln (DMN), **orange** Mensch, **violett** Software-Roboter (RPA), **rot** KI. Die Legende steht im Diagramm.

## 1. Entscheidungsregeln

| Wenn … | dann … | Beispiel |
|---|---|---|
| ein System eine Schnittstelle (API) anbietet | **System/Service** | Label erzeugen, Rückerstattung auslösen |
| die Entscheidung aus festen, prüfbaren Regeln folgt | **Regeln (DMN)** | Frist abgelaufen, Annahme bei Zustand „neuwertig“ |
| unstrukturierter Text ausgewertet oder ein Muster erkannt werden muss | **KI** | Retourengrund aus Freitext, Verdacht auf Wardrobing |
| nur eine Bedienoberfläche ohne API existiert | **Software-Roboter (RPA)** | Bestandskorrektur im ERP-Frontend |
| Ermessen, körperliche Tätigkeit oder Verantwortung gefragt ist | **Mensch** | Ware öffnen und bewerten, Grenzfall entscheiden |

Grundsatz: Automatisierung nur dort, wo das Ergebnis nachvollziehbar und im Fehlerfall abfangbar ist. KI liefert Vorschläge und Scores, die endgültige Entscheidung treffen Regeln oder Menschen.

**Der Mensch ist die Rückfallebene.** Für jeden automatisierten Schritt gibt es einen Weg zu einer Person: bei Label und Zahlung über ein Fehlerereignis (`b_label`, `b_refund`), bei KI-Auslesen und KI-Bewertung über die Regel (unklare oder unbewertete Fälle laufen zu `t_man1`), beim Bot technisch (Incident, die Sachbearbeitung bucht von Hand).

## 2. Aktivitäten im Soll-Prozess

„Fehlerfall“ nennt, ob der Fallback im Diagramm steht oder technisch (Wiederholung, Incident) umgesetzt wird. „Sprint“: 2 Integration, 3 Process Mining, 4 Workflow, 5 RPA/KI.

| ID | Aktivität | Art | Begründung | Verworfene Alternative | Schwachstelle | Sprint | Fehlerfall / Fallback |
|---|---|---|---|---|---|---|---|
| `s_mkt` | Marktplatz-Retoure empfangen | Service (REST) | Marktplätze haben eigene APIs; Daten kommen strukturiert an | Von Hand aus dem Portal abtippen: Aufwand, Tippfehler | W1 | 2 | Meldung fehlerhaft: Fall wird abgelehnt und protokolliert (technisch) |
| `t_parse` | E-Mail auslesen und strukturieren | KI | Formlose E-Mails sind zu verschieden für feste Regeln | Reguläre Ausdrücke: scheitern an Freitext; Mensch: Aufwand wie bisher | W2, W17 | 5 (Sprint 4: Testdaten) | Unsichere oder fehlende Angaben: Fall gilt als „unklar“, die Regel leitet ihn zu `t_man1` (im Diagramm über `gw1`) |
| `t_create` | Retourenfall zentral anlegen | Service | Eine gemeinsame Datenbasis für alle Kanäle; Fall-ID für Nachverfolgung | Manuelle Anlage im ERP: Doppelerfassung | W1, W3 | 2, 4 | Wiederholung bei Fehler (technisch) |
| `t_ai` | Retourengrund klassifizieren, Betrugsscore berechnen | KI-Agent | Freitext und Muster über die Kundenhistorie lassen sich nicht sinnvoll als feste Regeln beschreiben | Schlüsselwörter: erfassen nur einfache Fälle; Mensch: erkennt Muster nicht systematisch | W10, W17 | 5 (Sprint 4: Testwerte) | Ausfall: Score „nicht bewertet“, die Regel leitet unklare Fälle zu `t_man1` (im Diagramm über `gw1`) |
| `t_rule` | Anmeldung prüfen (Frist, Artikel, Kundenhistorie) | Regeln (DMN) | Kriterien sind eindeutig und müssen nachvollziehbar sein | KI: nicht nachvollziehbar für klare Fristen; Mensch: uneinheitlich | W4, W8 | 4 | Unklarer Fall: Weg „unklar / Verdacht“ |
| `t_man1` | Anmeldung manuell prüfen (Verdacht oder unklare Angaben) | Mensch | Verdacht ist kein Beweis; unklare Angaben ergänzt eine Person; eine Fehlentscheidung schadet dem Kunden | Automatisch ablehnen: Risiko falscher Ablehnungen | W10 | 4 | Entscheidung wird dokumentiert |
| `t_deny`, `t_deny2` | Ablehnung mitteilen | Service (Nachricht) | Standardtext mit Begründung; sofortige Information | E-Mail von Hand: verspätet, Nachfragen | W13 | 2, 4 | Versandfehler: Wiederholung (technisch) |
| `t_label` | Retourenlabel erzeugen | Service | Nur wenige Klicks, vollständig automatisierbar | RPA im Versandtool: nur nötig, wenn keine API existiert (Annahme: API vorhanden); Mensch: Aufwand | W5 | 4 | Fehlerereignis `b_label` → `t_label_man` (im Diagramm) |
| `t_label_man` | Label manuell erstellen | Mensch | Fallback bei Ausfall der Schnittstelle | Fall abbrechen: Kunde wartet | W5 | 4 | – |
| `t_notify` | Label und Status senden | Messaging | Statusänderung wird als Nachricht veröffentlicht; Kunden- und Reporting-Systeme lesen mit | E-Mail von Hand | W5, W6, W13 | 2 | Nachricht wird wiederholt (technisch) |
| `ev_arrive`, `ev_timeout` | Auf Wareneingang warten (21 Tage) | Ereignis/Timer | Workflow wartet ohne Zutun; die Nachricht „Paket eingetroffen“ kommt vom gRPC-Wareneingangs-Service; die Frist verhindert hängende Fälle | Manuelle Wiedervorlage in Excel | W6, W15 | 2, 4 | Timer → `t_expired` (im Diagramm) |
| `t_expired` | Als nicht eingegangen schließen | Service | Eindeutige Regel nach Fristablauf | Mensch: Liste manuell prüfen | W6 | 4 | – |
| `t_notify_in` | Wareneingang bestätigen | Messaging | Die Kund:in erfährt automatisch, dass die Ware angekommen ist; das beseitigt die Nachfragen ohne Tracking | E-Mail von Hand: verspätet | W6, W13 | 2 | Nachricht wird wiederholt (technisch) |
| `t_gr` | Zustand prüfen und erfassen | Mensch | Das Öffnen und Beurteilen der Ware bleibt Handarbeit; feste Zustandsskala (neuwertig / beschädigt / getragen) macht die Bewertung vergleichbar | Bilderkennung für den Zustand: aufwendig und unsicher | W7 | 4 | – |
| `t_gr_rec` | Zustand im WMS buchen | Service (gRPC) | Der Service aus Sprint 2 bucht den erfassten Zustand sofort im WMS; die Workflow-Engine ruft ihn auf, so ist die Integration im Prozess sichtbar und testbar | Eingabe später im WMS: Verzögerung und Doppelerfassung | W7 | 2, 4 | Service nicht erreichbar: Wiederholung, lokale Zwischenspeicherung (technisch) |
| `t_decide` | Annahme/Ablehnung entscheiden | Regeln (DMN) | Zustand, Warenwert und Betrugsscore ergeben eindeutige Standardfälle; nachvollziehbar und dokumentiert | KI entscheidet direkt: nicht nachvollziehbar; Mensch für alle Fälle: Aufwand | W8, W16 | 4 | Unklar: Weg „Grenzfall“ |
| `t_man2` | Grenzfall prüfen und dokumentieren | Mensch | Grenzfälle sind per Definition Ermessenssache; Dokumentation im System statt im E-Mail-Postfach | Automatisch entscheiden: Fehlentscheidung, Streit mit Kunden | W8, W9 | 4 | – |
| `t_reject` | Weiterverwendung festlegen (Rückversand, Spende, Entsorgung) | Mensch | Hängt von Ware und Situation ab | Regel nach Warenwert: kann später als Entscheidungshilfe ergänzt werden | W16 | 4 | – |
| `t_stock` | Bestand im ERP buchen / korrigieren | RPA | Das ERP wird über die Bedienoberfläche bedient (Annahme: keine passende API); die Buchung ist regelmäßig und gleichförmig; der Bot ist in der Aufgabe (Sprint 5) vorgesehen | Service per API: besser, wenn vorhanden; Mensch: Fehler und Aufwand | W11 | 5 (Sprint 4: Benutzeraufgabe) | Bot scheitert: Incident, die Sachbearbeitung bucht von Hand (technisch, nicht im Diagramm) |
| `t_refund` | Rückerstattung / Gutschein auslösen | Service | Zahlungsdienstleister und CRM haben Schnittstellen; größter Hebel für die Durchlaufzeit. Geld oder Gutschein wird im Service nach Zahlungsart und Kundenwunsch entschieden | RPA im Portal: unnötig bei vorhandener API; Mensch: 8–12 Tage | W12 | 4 | Fehlerereignis `b_refund` → `t_refund_man` (im Diagramm) |
| `t_refund_man` | Rückerstattung manuell klären | Mensch | Fallback bei fehlgeschlagener Zahlung | Fall bleibt offen | W12 | 4 | – |
| `t_end_notify` | Kund:in über Ergebnis informieren | Messaging | Automatische Abschlussmeldung; Statusereignisse speisen das Dashboard | E-Mail von Hand | W13, W14 | 2 | Nachricht wird wiederholt (technisch) |
| `t_report` | Kennzahlen aus Ereignisdaten berechnen | Service | Die Kennzahlen entstehen aus Status-Events und Camunda-Ereignissen und ersetzen das manuelle Excel-Reporting; derselbe Event-Log speist die KPIs aus Sprint 3 | Excel von Hand: monatlich, rückblickend, fehleranfällig | W14 | 3, 6 | Berechnung scheitert: Wiederholung, Hinweis an das Controlling (technisch) |

### Sprint-4-Varianten für RPA und KI
Die Aufgabe verlangt in Sprint 4 „menschliche Aktivitäten auch für vorher identifizierte RPA-/KI-Aktivitäten“. Die drei Schritte `t_parse`, `t_ai` und `t_stock` laufen deshalb im ausführbaren Workflow zuerst als Benutzeraufgabe oder mit festen Testwerten und werden in Sprint 5 durch den n8n-Agenten bzw. den UiPath-Bot ersetzt. Das Diagramm zeigt den Zielzustand.

## 3. Zusammenfassung

| Art | Anzahl Aufgaben | Aufgaben |
|---|---|---|
| System/Service/Messaging | 11 | `t_create`, `t_deny`, `t_deny2`, `t_label`, `t_notify`, `t_notify_in`, `t_expired`, `t_gr_rec`, `t_refund`, `t_end_notify`, `t_report` |
| Regeln (DMN) | 2 | `t_rule`, `t_decide` |
| KI | 2 | `t_parse`, `t_ai` |
| RPA | 1 | `t_stock` |
| Mensch | 6 | `t_man1`, `t_man2`, `t_reject`, `t_gr`, `t_label_man`, `t_refund_man` |

Von 22 Aufgaben laufen 16 automatisch (73 %). **Im Normalfall (Webshop, Annahme) sind es 11 von 12 Aufgaben, nur die Zustandsprüfung am Wareneingang ist menschlich.** Die übrigen fünf menschlichen Aufgaben sind Ausnahmen: unklare Anmeldung oder Verdacht (`t_man1`), Grenzfall (`t_man2`), Weiterverwendung nach Ablehnung (`t_reject`), Ausfall der Label-Schnittstelle (`t_label_man`) und fehlgeschlagene Zahlung (`t_refund_man`).

### Abdeckung der Schwachstellen

| Schwachstelle | Soll-Element |
|---|---|
| W1–W5 | `s_mkt`, `t_parse`, `t_create`, `t_rule`, `t_label`, `t_notify` |
| W6 | `ev_arrive`, `ev_timeout`, `t_notify`, `t_notify_in` |
| W7–W9 | `t_gr`, `t_gr_rec`, `t_decide`, `t_man2` |
| W10, W17 | `t_ai`, `t_man1` |
| W11, W12 | `t_stock`, `t_refund` |
| W13 | `t_notify`, `t_notify_in`, `t_end_notify`, Anmerkung `Ann_3` |
| W14 | `t_report` (eigener Ablauf in der Lane Management / Controlling), Anmerkung `Ann_1` |
| W16 | `t_decide` |
| W15 | **nur als Anmerkung** (`Ann_2`): Aufkommen für die Schichtplanung. Eine eigene Aktivität entsteht erst nach der Bewertung von Kosten und Nutzen der Sendungsverfolgung. |

## 4. Ablaufsteuerung, Teilnehmer und Systeme

**Gateways (Camunda 8 erlaubt Bedingungen nur an Gateways, deshalb folgt auf jede Entscheidung einer Person ein Gateway; Zusammenführungen sind explizit)**

| ID | Gateway | Zweck |
|---|---|---|
| `gw_cr` | XOR-Merge | führt die drei Eingangskanäle (Webshop, Marktplatz, E-Mail über `t_parse`) zusammen |
| `gw1` | Prüfergebnis? | Ausgang der DMN-Prüfung: zulässig, unzulässig oder unklar / Verdacht |
| `gw_m1` | Anmeldung ablehnen? | Entscheidung aus `t_man1` (Variable `ablehnen`): ablehnen oder weiter |
| `gw_join` | XOR-Merge | führt zulässige und freigegebene Anmeldungen vor dem Label zusammen |
| `gw_nt` | XOR-Merge | führt automatisch (`t_label`) und manuell (`t_label_man`) erstelltes Label zusammen |
| `gw_evt` | Ereignisbasiert | wartet auf Wareneingang (`ev_arrive`) oder Ablauf der 21 Tage (`ev_timeout`) |
| `gw2` | Ergebnis? | Ausgang der DMN-Entscheidung: Annahme, Ablehnung oder Grenzfall |
| `gw_m2` | Angenommen? | Entscheidung aus `t_man2` (Variable `angenommen`) |
| `gw_rj` | XOR-Merge | führt Ablehnung nach der Zustandsprüfung und abgelehnte Grenzfälle vor `t_reject` zusammen |
| `gw_acc` | XOR-Merge | führt Annahme und angenommene Grenzfälle zusammen |
| `gw_par` / `gw_pj` | Parallel | Bestandsbuchung und Rückerstattung laufen gleichzeitig; der Join erwartet genau zwei Zweige |
| `gw_rm` | XOR-Merge | führt `t_refund` und `t_refund_man` zusammen, damit nur ein Token den parallelen Join erreicht (ohne `gw_rm` käme es zum Deadlock) |
| `gw_en` | XOR-Merge | führt Annahme (nach dem Join) und Ablehnung vor der Ergebnismeldung zusammen |

**Externe Teilnehmer (eingeklappte Pools, Kommunikation über Nachrichtenflüsse):** Kund:in und Marktplatz (Amazon, Zalando). Die Kund:in meldet per Webshop oder E-Mail an und erhält Ablehnung, Label, Wareneingangsbestätigung und Ergebnis. Der Rückversand und das Eintreffen des Pakets (`ev_arrive`) sind Nachrichten der Kund:in; der Marktplatz liefert die Retourenmeldung per REST (`s_mkt`).

**Lanes:** Retouren-Workflow (automatisiert), Lager, Sachbearbeitung / Kundenservice und neu Management / Controlling für das Reporting (`t_report`).

**Datenspeicher im Soll-Modell** (nur dort, wo ein Service oder Roboter ein System anspricht): Retouren-Datenbank (`t_create`), WMS (`t_gr_rec`, gRPC-Service), ERP-System (`t_stock`, RPA), Kennzahlen-Dashboard (`t_report`). Versandtool, Zahlungsdienstleister und CRM werden über APIs angesprochen (`t_label`, `t_refund`) und im Diagramm nicht einzeln gezeigt.

## 5. Risiken der Automatisierung

- **KI-Fehlklassifikation:** Der Score ist nur Entscheidungshilfe, die Entscheidung treffen Regeln und Menschen. In Sprint 5 wird die Qualität an Testfällen geprüft.
- **RPA bricht bei Änderungen der Oberfläche:** Fallback ist eine Aufgabe für die Sachbearbeitung.
- **Annahmen:** Das Versandtool und der Zahlungsdienstleister haben Schnittstellen, das ERP nicht. Das ist mit dem Dozenten bzw. dem Team zu bestätigen.
- **Daten für die Regeln:** Die DMN-Prüfung nutzt die Kundenhistorie. Für die Mock-Umgebung müssen wenige Kennzahlen (Anzahl Retouren, Retourenquote) festgelegt werden.
- **Reporting braucht Ereignisdaten:** Das Dashboard setzt voraus, dass Status-Events und Camunda-Ereignisse mit Zeitstempel und Fall-ID geschrieben werden (siehe „Benötigte Daten“ im KPI-Katalog).
- **Kosten der Sendungsverfolgung** (W15): Nutzen gegen Aufwand abwägen, bevor sie umgesetzt wird.
