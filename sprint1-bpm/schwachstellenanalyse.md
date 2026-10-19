# Schwachstellenanalyse Ist-Prozess

Bezug: `sprint1-bpm/ist-prozess.bpmn` (Spalte „Element“ nennt die IDs im Diagramm).
Typen: **MB** Medienbruch · **WZ** Wartezeit · **FQ** Fehlerquelle · **DE** doppelte Dateneingabe · **IN** Intransparenz · **MI** Missbrauchsrisiko.
Priorität: Auswirkung (A) und Aufwand zur Behebung (E) jeweils h/m/n (hoch/mittel/niedrig). Das ist eine Einschätzung des Teams; echte Messwerte liefert erst Sprint 3.

## 1. Übersicht nach Kategorien

### Medienbrüche
Die Daten wechseln zwischen Webshop-Formular, Marktplatz-Portalen, E-Mail, ERP, WMS, CRM, Zahlungsdienstleister und Excel, ohne dass ein System die Daten an das nächste übergibt. Jeder Wechsel erzeugt Abtippen und eigene Datenstände.
→ W1, W2, W5, W9, W14, W17

### Wartezeiten
Der Fall wartet darauf, dass eine Person ihn bearbeitet: Label, Zustandserfassung nach Wareneingang, Eskalation per E-Mail, Rückerstattung durch die Buchhaltung, Kundeninformation. Die Rückerstattung dauert aktuell 8–12 Tage (Aufgabenstellung).
→ W5, W7, W9, W12, W13, W15

### Fehlerquellen
Manuelles Abtippen, Prüfung nach Erfahrung und subjektive Zustandsbewertung führen zu Tippfehlern, uneinheitlichen Entscheidungen und Bestandsabweichungen.
→ W2, W3, W4, W7, W8, W11, W16

## 2. Detailtabelle

| Nr. | Schritt | Element | Typ | Schwachstelle | Auswirkung | Maßnahme im Soll-Prozess | Umsetzung | A | E |
|---|---|---|---|---|---|---|---|---|---|
| W1 | Anmeldung über drei Kanäle | `s_web`, `s_mkt`, `s_mail`, `t_mkt` | MB | Webshop, Marktplätze (eigene APIs) und E-Mail haben eigene Formate, keine gemeinsame Datenbasis. Eine Bündelung auf einen Kanal ist nicht möglich, weil die Marktplätze ihre eigenen Wege vorgeben. | Uneinheitliche Daten, hoher Erfassungsaufwand | Alle Kanäle münden in einen zentralen Retourenfall mit einheitlichem Datenmodell; Marktplätze per REST angebunden | System/Service (Sprint 2, 4) | h | h |
| W2 | E-Mail-Anmeldung lesen | `t_mail` | MB, FQ | Freitext, fehlende Angaben, Rückfragen | Verzögerung, Tippfehler | Extraktion der Daten aus dem E-Mail-Text | KI (Sprint 5) | m | m |
| W3 | Erfassung im ERP | `t_erp` | DE, FQ | Daten werden aus Portal oder E-Mail abgetippt; Übertragungsfehler, falsch gelesene oder verrutschte Spalten | Fehler, Zeitverlust, Kosten durch reine Bearbeitung | Der Fall wird automatisch angelegt | System/Service | h | m |
| W4 | Prüfung der Anmeldung | `t_erp`, `gw_ok`, `t_rej` | FQ | Prüfung nach Erfahrung, keine feste Vorprüfung; auch abgelaufene Fristen werden von Hand aussortiert | Uneinheitliche Entscheidungen, Zeitverlust | Regelbasierte Vorprüfung (Frist, Artikel, Kundenhistorie) | Regeln/DMN (Sprint 4) | m | n |
| W5 | Label erstellen und senden | `t_label`, `t_send` | WZ, MB | Eine Person erstellt das Label im Versandtool (nur wenige Klicks) und versendet es per E-Mail | Wartezeit für Kund:in, Personalaufwand pro Fall | Automatische Labelerzeugung per Versand-API, Versand per Nachricht | System/Service | m | n |
| W6 | Rückversand | `t_send`, `ev_wait` | IN | Kein Status-Tracking bis Eintreffen im Lager | Kund:innen fragen nach, Management ohne Überblick | Status-Events; Timer (21 Tage) für nicht eingegangene Ware | Messaging, Workflow | m | m |
| W7 | Wareneingang und Zustandsprüfung | `t_gr` | WZ, FQ | Erfassung im WMS erfolgt oft verzögert; Zustand (neuwertig/beschädigt/getragen) wird subjektiv bewertet. Das Öffnen und Beurteilen der Ware bleibt Handarbeit. | Verzögerte Erstattung, uneinheitliche Bewertung | Erfassung direkt am Wareneingang über gRPC-Service mit fester Zustandsskala | Mensch + Service (Sprint 2) | h | m |
| W8 | Entscheidung Annahme/Ablehnung | `t_dec`, `gw1` | FQ | Entscheidung nicht dokumentiert, nicht regelbasiert | Streit mit Kund:innen, keine Nachvollziehbarkeit | DMN-Regeln auf Zustand, Warenwert, Betrugsscore; Entscheidung wird gespeichert | Regeln/DMN | h | m |
| W9 | Grenzfall-Eskalation | `t_esc` | MB, WZ | Eskalation und Dokumentation laufen per E-Mail, ohne Frist; für die Auswertung muss später wieder in Excel übertragen werden | Lange Liegezeit, Informationsverlust | Menschliche Aufgabe im Workflow mit dokumentierter Entscheidung | Mensch | m | n |
| W10 | Betrugs-/Missbrauchserkennung | `t_dec` (fehlt im Ist-Modell) | MI | Wardrobing wird nicht systematisch erkannt | Wirtschaftlicher Schaden | Betrugsscore aus Kundenhistorie und Mustern | KI-Agent (Sprint 5) | h | m |
| W11 | Bestandsbuchung | `t_stock` | DE, FQ | Rückbuchung im ERP-Frontend von Hand | Bestandsabweichungen | Bot bucht im ERP-Frontend | RPA (Sprint 5) | m | m |
| W12 | Rückerstattung | `gw_ref`, `t_pay`, `t_voucher` | WZ, DE | Buchhaltung veranlasst Erstattung beim Zahlungsdienstleister bzw. Gutschein im CRM von Hand | Haupttreiber der Durchlaufzeit (8–12 Tage) | Automatische Erstattung per API nach Annahme; Fallback als User Task | System/Service | h | m |
| W13 | Kundenkommunikation | `t_comm` | IN, WZ | Verspätete E-Mails, häufige Nachfragen | Mehraufwand im Kundenservice, Unzufriedenheit | Automatische Statusnachrichten bei jedem Statuswechsel | Messaging | m | n |
| W14 | Reporting | `s_rep`, `t_report` | MB, DE, IN | Monatliches Excel-Reporting aus E-Mails und Listen, nur rückblickend | Management sieht Probleme zu spät | Events speisen Dashboard und Process Mining | Messaging, Process Mining (Sprint 3) | m | m |
| W15 | Planung des Wareneingangs | `t_send`, `ev_wait`, `t_gr` | WZ, IN | Ohne Tracking ist das Aufkommen der nächsten Tage unbekannt; die Schichtplanung berücksichtigt Spitzentage nicht | Rückstau am Wareneingang, verzögerte Zustandsprüfung | Tracking-Events bzw. Auswertung des Aufkommens je Tag für die Schichtplanung. Sendungsverfolgung verursacht Kosten; Nutzen gegen Aufwand abwägen, da die Arbeit dadurch nicht automatisch kleiner wird | Messaging, Process Mining | m | m |
| W16 | Ablehnung | `t_dec`, `t_reject` | FQ, WZ | Auch eindeutige Standardfälle werden von Menschen bearbeitet | Hohe Bearbeitungskosten | DMN entscheidet Standardfälle, der Mensch nur Grenzfälle | Regeln/DMN, Mensch | m | n |
| W17 | Retourengrund | `s_web`, `s_mkt`, `s_mail` | MB | Der Grund wird nicht strukturiert erfasst oder ausgewertet; Freitext aus den Kanälen | Keine Auswertung der Gründe, keine Ableitung von Maßnahmen | Klassifikation des Grundes aus Freitext | KI-Agent (Sprint 5) | m | m |

## 3. Priorisierung (Einschätzung)

Hohe Auswirkung zuerst, bei gleicher Auswirkung der geringere Aufwand:

1. **W12 Rückerstattung** (A h, E m): größter Einzeltreiber der Durchlaufzeit.
2. **W7 Wareneingang/Zustand** (A h, E m): verzögert alle nachfolgenden Schritte.
3. **W8 Entscheidung** (A h, E m): Grundlage für nachvollziehbare Ergebnisse.
4. **W3 Erfassung im ERP** (A h, E m): Fehlerquelle und Zeitverlust.
5. **W10 Betrugserkennung** (A h, E m): erfüllt ein ausdrückliches Ziel des Managements.

W1 hat ebenfalls hohe Auswirkung, aber hohen Aufwand (Anbindung mehrerer Kanäle); sie wird durch die Mock-REST-API aus Sprint 2 prototypisch umgesetzt.
Schnelle Gewinne (geringer Aufwand): W4, W5, W9, W13, W16.

## 4. Abdeckung der bekannten Schwachstellen aus der Aufgabenstellung

| Bekannte Schwachstelle | Behandelt durch |
|---|---|
| Medienbrüche zwischen Kanälen, keine gemeinsame Datenbasis | W1, W2, W3, W17 |
| Keine Ende-zu-Ende-Transparenz | W6, W13, W14, W15 |
| Subjektive, undokumentierte Entscheidungen | W4, W7, W8, W9, W16 |
| Mehrfache manuelle Datenpflege (ERP, WMS, CRM, Excel) | W3, W11, W12, W14 |
| Kein Erkennen von Betrugsmustern | W10 |
| Lange Durchlaufzeit (8–12 Tage) | W5, W7, W12, W15 |

## 5. Hypothesen für Sprint 3 (Process Mining)
- Der größte Zeitanteil liegt zwischen Wareneingang und Rückerstattung (W7, W12).
- Fälle aus dem E-Mail-Kanal haben mehr Schleifen und längere Durchlaufzeiten als Webshop-Fälle.
- Grenzfälle und Rückfragen erzeugen Varianten mit hoher Durchlaufzeit.
- Das Aufkommen im Wareneingang schwankt je Wochentag deutlich (W15).
