# Soll-Prozess: Realisierung der Schritte

Bezug: `bpmn/soll-prozess.bpmn` (Spalte „ID“ nennt die Elemente im Diagramm) und `docs/sprint1/schwachstellenanalyse.md` (W-Nummern).

## 1. Entscheidungsregeln

| Wenn … | dann … | Beispiel |
|---|---|---|
| ein System eine Schnittstelle (API) anbietet | **System/Service** | Label erzeugen, Rückerstattung auslösen |
| die Entscheidung aus festen, prüfbaren Regeln folgt | **Regeln (DMN)** | Frist abgelaufen, Annahme bei Zustand „neuwertig“ |
| unstrukturierter Text ausgewertet oder ein Muster erkannt werden muss | **KI** | Retourengrund aus Freitext, Verdacht auf Wardrobing |
| nur eine Bedienoberfläche ohne API existiert | **Software-Roboter (RPA)** | Bestandskorrektur im ERP-Frontend |
| Ermessen, körperliche Tätigkeit oder Verantwortung gefragt ist | **Mensch** | Ware öffnen und bewerten, Grenzfall entscheiden |

Grundsatz: Automatisierung nur dort, wo das Ergebnis nachvollziehbar und im Fehlerfall abfangbar ist. KI liefert Vorschläge und Scores, die endgültige Entscheidung treffen Regeln oder Menschen.

## 2. Aktivitäten im Soll-Prozess

| ID | Aktivität | Art | Begründung | Verworfene Alternative | Schwachstelle | Sprint | Fehlerfall / Fallback |
|---|---|---|---|---|---|---|---|
| `s_mkt` | Marktplatz-Retoure empfangen | Service (REST) | Marktplätze haben eigene APIs; Daten kommen strukturiert an | Von Hand aus dem Portal abtippen: Aufwand, Tippfehler | W1 | 2 | Meldung fehlerhaft: Fall wird abgelehnt und protokolliert |
| `t_parse` | E-Mail auslesen und strukturieren | KI | Formlose E-Mails sind zu verschieden für feste Regeln | Reguläre Ausdrücke: scheitern an Freitext; Mensch: Aufwand wie bisher | W2, W17 | 5 | Unsichere Extraktion: Sachbearbeitung ergänzt (als Aufgabe in Sprint 4) |
| `t_create` | Retourenfall zentral anlegen | Service | Eine gemeinsame Datenbasis für alle Kanäle; Fall-ID für Nachverfolgung | Manuelle Anlage im ERP: Doppelerfassung | W1, W3 | 2, 4 | Wiederholung bei Fehler |
| `t_ai` | Retourengrund klassifizieren, Betrugsscore berechnen | KI-Agent | Freitext und Muster über die Kundenhistorie lassen sich nicht sinnvoll als feste Regeln beschreiben | Schlüsselwörter: erfassen nur einfache Fälle; Mensch: erkennt Muster nicht systematisch | W10, W17 | 5 | Score ist nur Eingabe für die Regel; bei Ausfall Standardwert „nicht bewertet“ |
| `t_rule` | Anmeldung prüfen (Frist, Artikel, Kundenhistorie) | Regeln (DMN) | Kriterien sind eindeutig und müssen nachvollziehbar sein | KI: nicht nachvollziehbar für klare Fristen; Mensch: uneinheitlich | W4, W8 | 4 | Unklarer Fall: Weg „Verdacht“ |
| `t_man1` | Verdacht manuell prüfen | Mensch | Verdacht ist kein Beweis; eine Fehlentscheidung schadet dem Kunden | Automatisch ablehnen: Risiko falscher Ablehnungen | W10 | 4 | Entscheidung wird dokumentiert |
| `t_deny`, `t_deny2` | Ablehnung mitteilen | Service (Nachricht) | Standardtext mit Begründung; sofortige Information | E-Mail von Hand: verspätet, Nachfragen | W13 | 2, 4 | Versandfehler: Wiederholung |
| `t_label` | Retourenlabel erzeugen | Service | Nur wenige Klicks, vollständig automatisierbar | RPA im Versandtool: nur nötig, wenn keine API existiert (Annahme: API vorhanden); Mensch: Aufwand | W5 | 4 | Fehlerereignis `b_label` → `t_label_man` |
| `t_label_man` | Label manuell erstellen | Mensch | Fallback bei Ausfall der Schnittstelle | Fall abbrechen: Kunde wartet | W5 | 4 | – |
| `t_notify` | Label und Status senden | Messaging | Statusänderung wird als Nachricht veröffentlicht; Kunden- und Reporting-Systeme lesen mit | E-Mail von Hand | W5, W6, W13 | 2 | Nachricht wird wiederholt |
| `ev_arrive`, `ev_timeout` | Auf Wareneingang warten (21 Tage) | Ereignis/Timer | Workflow wartet ohne Zutun; Frist verhindert hängende Fälle | Manuelle Wiedervorlage in Excel | W6, W15 | 4 | Timer → `t_expired` |
| `t_expired` | Als nicht eingegangen schließen | Service | Eindeutige Regel nach Fristablauf | Mensch: Liste manuell prüfen | W6 | 4 | – |
| `t_gr` | Zustand prüfen und am Wareneingang buchen | Mensch + Service (gRPC) | Das Öffnen und Beurteilen der Ware bleibt Handarbeit; die Erfassung läuft direkt über den Service mit fester Zustandsskala | Bilderkennung für den Zustand: aufwendig und unsicher; Eingabe später im WMS: Verzögerung | W7 | 2, 4 | Service nicht erreichbar: lokale Zwischenspeicherung |
| `t_decide` | Annahme/Ablehnung entscheiden | Regeln (DMN) | Zustand, Warenwert und Betrugsscore ergeben eindeutige Standardfälle; nachvollziehbar und dokumentiert | KI entscheidet direkt: nicht nachvollziehbar; Mensch für alle Fälle: Aufwand | W8, W16 | 4 | Unklar: Weg „Grenzfall“ |
| `t_man2` | Grenzfall prüfen und dokumentieren | Mensch | Grenzfälle sind per Definition Ermessenssache; Dokumentation im System statt im E-Mail-Postfach | Automatisch entscheiden: Fehlentscheidung, Streit mit Kunden | W8, W9 | 4 | – |
| `t_reject` | Weiterverwendung festlegen (Rückversand, Spende, Entsorgung) | Mensch | Hängt von Ware und Situation ab | Regel nach Warenwert: kann später als Entscheidungshilfe ergänzt werden | W16 | 4 | – |
| `t_stock` | Bestand im ERP buchen / korrigieren | RPA | Das ERP wird über die Bedienoberfläche bedient (Annahme: keine passende API); die Buchung ist regelmäßig und gleichförmig | Service per API: besser, wenn vorhanden; Mensch: Fehler und Aufwand | W11 | 5 | Bot scheitert: Aufgabe für Sachbearbeitung |
| `t_refund` | Rückerstattung auslösen / Gutschein anlegen | Service | Zahlungsdienstleister und CRM haben Schnittstellen; größter Hebel für die Durchlaufzeit. Geld oder Gutschein wird im Service nach Zahlungsart und Kundenwunsch entschieden | RPA im Portal: unnötig bei vorhandener API; Mensch: 8–12 Tage | W12 | 4 | Fehlerereignis `b_refund` → `t_refund_man` |
| `t_refund_man` | Rückerstattung manuell klären | Mensch | Fallback bei fehlgeschlagener Zahlung | Fall bleibt offen | W12 | 4 | – |
| `t_end_notify` | Kund:in über Ergebnis informieren | Messaging | Automatische Abschlussmeldung; Statusereignisse speisen Dashboard | E-Mail von Hand | W13, W14 | 2 | Nachricht wird wiederholt |

## 3. Zusammenfassung

| Art | Anzahl Aufgaben | Aufgaben |
|---|---|---|
| System/Service/Messaging | 8 | `t_create`, `t_deny`, `t_deny2`, `t_label`, `t_notify`, `t_expired`, `t_refund`, `t_end_notify` |
| Regeln (DMN) | 2 | `t_rule`, `t_decide` |
| KI | 2 | `t_parse`, `t_ai` |
| RPA | 1 | `t_stock` |
| Mensch + Service | 1 | `t_gr` |
| Mensch | 5 | `t_man1`, `t_man2`, `t_reject`, `t_label_man`, `t_refund_man` |

Von 19 Aufgaben laufen 13 automatisch, eine ist hybrid. Von den 5 rein menschlichen Aufgaben sind drei Ausnahmepfade (`t_label_man`, `t_refund_man`, `t_man1`); im Normalfall bleiben also die Grenzfallprüfung und die Entscheidung über die Weiterverwendung beim Menschen.

### Abdeckung der Schwachstellen

| Schwachstelle | Soll-Element |
|---|---|
| W1–W5 | `s_mkt`, `t_parse`, `t_create`, `t_rule`, `t_label`, `t_notify` |
| W6 | `ev_arrive`, `ev_timeout`, `t_notify` |
| W7–W9 | `t_gr`, `t_decide`, `t_man2` |
| W10, W17 | `t_ai`, `t_man1` |
| W11, W12 | `t_stock`, `t_refund` |
| W13 | `t_notify`, `t_end_notify`, Anmerkung `Ann_3` |
| W16 | `t_decide` |
| W14, W15 | **nur als Anmerkung** (`Ann_1`, `Ann_2`): Reporting aus Status-Events und Aufkommen für die Schichtplanung. Eigene Aktivitäten dafür entstehen erst mit Process Mining und Workflow (Sprint 3 und 4). |

## 4. Ablaufsteuerung, Teilnehmer und Systeme

**Gateways (Camunda 8 erlaubt Bedingungen nur an Gateways, deshalb folgt auf jede Entscheidung einer Person ein Gateway)**

| ID | Gateway | Zweck |
|---|---|---|
| `gw1` | Prüfergebnis? | Ausgang der DMN-Prüfung: zulässig, unzulässig oder Verdacht |
| `gw_m1` | Verdacht bestätigt? | Entscheidung aus `t_man1`: weiter (Variable `verdachtBestaetigt = false`) oder ablehnen |
| `gw_evt` | Ereignisbasiert | wartet auf Wareneingang (`ev_arrive`) oder Ablauf der 21 Tage (`ev_timeout`) |
| `gw2` | Ergebnis? | Ausgang der DMN-Entscheidung: Annahme, Ablehnung oder Grenzfall |
| `gw_m2` | Angenommen? | Entscheidung aus `t_man2` (Variable `angenommen`) |
| `gw_par` / `gw_pj` | Parallel | Bestandsbuchung und Rückerstattung laufen gleichzeitig; der Join erwartet genau zwei Zweige |
| `gw_rm` | XOR-Merge | führt `t_refund` und `t_refund_man` zusammen, damit nur ein Token den parallelen Join erreicht (ohne `gw_rm` käme es zum Deadlock) |

**Externe Teilnehmer (eingeklappte Pools, Kommunikation über Nachrichtenflüsse):** Kund:in und Marktplatz (Amazon, Zalando). Die Kund:in meldet per Webshop oder E-Mail an und erhält Ablehnung, Label, Status und Ergebnis. Der Rückversand und das Eintreffen des Pakets (`ev_arrive`) sind Nachrichten der Kund:in; der Marktplatz liefert die Retourenmeldung per REST (`s_mkt`).

**Datenspeicher im Soll-Modell** (nur dort, wo ein Service oder Roboter ein System anspricht): Retouren-Datenbank (`t_create`), WMS (`t_gr`, gRPC-Service), ERP-System (`t_stock`, RPA). Versandtool, Zahlungsdienstleister und CRM werden über APIs angesprochen (`t_label`, `t_refund`) und im Diagramm nicht einzeln gezeigt.

## 5. Risiken der Automatisierung

- **KI-Fehlklassifikation:** Der Score ist nur Entscheidungshilfe, die Entscheidung treffen Regeln und Menschen. In Sprint 5 wird die Qualität an Testfällen geprüft.
- **RPA bricht bei Änderungen der Oberfläche:** Fallback ist eine Aufgabe für die Sachbearbeitung.
- **Annahmen:** Das Versandtool und der Zahlungsdienstleister haben Schnittstellen, das ERP nicht. Das ist mit dem Dozenten bzw. dem Team zu bestätigen.
- **Kosten der Sendungsverfolgung** (W15): Nutzen gegen Aufwand abwägen, bevor sie umgesetzt wird.
