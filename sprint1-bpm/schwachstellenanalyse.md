# Schwachstellenanalyse Ist-Prozess

Bezug: `sprint1-bpm/ist-prozess.bpmn`. Die W-Nummern stehen dort als Anmerkungen im Diagramm. Die Spalte „Element“ nennt Name und ID (die ID steht im Modeler im Eigenschaftenfeld; sie bleibt beim Umbenennen gleich, neu angelegte Elemente bekommen eine neue ID).

**Kategorien** (Vorgabe der Aufgabe): **MB** Medienbruch · **WZ** Wartezeit · **FQ** Fehlerquelle. Doppelerfassung, Intransparenz und Missbrauchsrisiko sind Ausprägungen und stehen im Text der Schwachstelle.

**Beleg:** **A** = steht in der Aufgabenstellung (Schritt 1–10 des Ausgangsszenarios, Systemliste oder „Bekannte Schwachstellen“ 1–6 in der Reihenfolge von S. 5). **H** = Annahme, abgeleitet; sie ist im Team oder mit dem Dozenten zu bestätigen.

**Priorität:** Auswirkung (A) und Aufwand zur Behebung (E) jeweils h/m/n (hoch/mittel/niedrig). Das ist ein **Vorschlag der KI, im Team abzustimmen** (siehe Prüfliste in `AI-USAGE.md`); echte Messwerte liefert erst Sprint 3.

## 1. Übersicht nach Kategorien

### Medienbrüche (10): W1, W2, W3, W5, W6, W9, W11, W12, W14, W17
Die Daten wechseln zwischen Webshop-Formular, Marktplatz-Portalen, E-Mail, ERP, Versandtool, WMS, CRM, Zahlungsdienstleister und Excel, ohne dass ein System die Daten an das nächste übergibt. Jeder Wechsel erzeugt Abtippen (Doppelerfassung) und eigene Datenstände. W6 zählt dazu, weil zwischen Rückversand und ShopEasy kein Informationsfluss besteht.

| Nr. | Von | Nach | Schritt / Element | Art | W | Beleg |
|---|---|---|---|---|---|---|
| M1 | Webshop-Frontend (Formular) | ERP | Schritt 2, Anmeldung im ERP erfassen (`t_erp`) | abtippen | W1, W3 | A |
| M2 | Marktplatz-Portale (Amazon, Zalando) | ERP | Schritt 2, Marktplatz-Portal sichten (`t_mkt`), `t_erp` | abtippen, eigene Portale | W1, W3 | A (separate Meldungen, eigene APIs) |
| M3 | E-Mail-Postfach (Freitext) | ERP | Schritt 2, E-Mail lesen und abtippen (`t_mail`), `t_erp` | lesen, nachfragen, abtippen | W1, W2, W3 | A |
| M4 | ERP | Versandtool | Schritt 3, Label im Versandtool erstellen (`t_label`) | Daten erneut eingeben | W5 | H (Aufgabe: „manuelle Erstellung im Versandtool“) |
| M5 | Versandtool | E-Mail-Postfach, Kund:in | Schritt 3, Label per E-Mail senden (`t_send`) | Label von Hand versenden | W5 | A |
| M6 | WMS | ERP | Schritt 5 und 7, `t_gr`, Bestand im ERP buchen (`t_stock`) | Zustand und Bestand getrennt gepflegt | W7, W11 | A (Systemliste), H (Doppelerfassung) |
| M7 | E-Mail-Postfach (Eskalation) | Excel-Listen | Schritt 6, Grenzfall per E-Mail eskalieren (`t_esc`) | Dokumentation nur in E-Mails, später Übertrag | W9 | A (E-Mails), H (Excel) |
| M8 | ERP | Zahlungsdienstleister, CRM | Schritt 8, `t_pay`, Gutschein im CRM (`t_voucher`) | Erstattung und Gutschein von Hand | W12 | A |
| M9 | ERP, WMS, CRM, E-Mail | Excel-Listen | Schritt 10, Reporting in Excel (`t_report`) | Zahlen zusammentragen | W14 | A |

Damit sind alle acht Systeme der Aufgabenstellung (und das Versandtool aus Schritt 3) an mindestens einem Medienbruch beteiligt.

### Wartezeiten (7): W5, W7, W9, W12, W13, W15, W16
Der Fall wartet darauf, dass eine Person ihn bearbeitet oder dass die Kund:in die Ware zurückschickt. Die Rückerstattung dauert aktuell 8–12 Tage (Aufgabenstellung, ohne Angabe des Startpunkts).

| Nr. | Stelle (von → nach) | intern / extern | beeinflussbar | zugehörige W | KPI (Sprint 3) | Beleg |
|---|---|---|---|---|---|---|
| Z1 | Anmeldung → Label erstellt (Erfassung im ERP, Label im Versandtool) | intern | ja | W3, W5 | K9 | A (Schritte 2–3 manuell), H (Wartezeit) |
| Z2 | Label gesendet → Paket im Lager (Rückversand) | extern (Kund:in, Versand) | nein, nur sichtbar machen | W6, W15 | K14 | A (Schritt 4) |
| Z3 | Paket im Lager → Zustand erfasst | intern | ja | W7 | K2 | A (Schritt 5 „oft verzögert“) |
| Z4 | Zustand erfasst → Entscheidung (inkl. Eskalation) | intern | ja | W8, W9, W16 | K4 | A (Schritt 6), H (Liegezeit) |
| Z5 | Entscheidung „Annahme“ → Rückerstattung | intern | ja | W12 | K3 | A (Schritt 8, 8–12 Tage) |
| Z6 | Ergebnis → Information an die Kund:in | intern | ja | W13 | K8 | A (Schritt 9 „verzögert“) |

Der Rückversand (Z2) ist Transportzeit der Kund:in und nicht abstellbar. Beeinflussbar sind die Liegezeiten Z1 und Z3 bis Z6.

### Fehlerquellen (8): W2, W3, W4, W7, W8, W10, W11, W16
Manuelles Abtippen, Prüfung nach Erfahrung, subjektive Zustandsbewertung und fehlende Kontrolle führen zu Tippfehlern, uneinheitlichen Entscheidungen, Bestandsabweichungen und unerkanntem Missbrauch (W10: Kontrolllücke bei der Entscheidung).

### Zusammenhänge
1. **Datenmodell → Fehler → Zeit:** kein gemeinsames Datenmodell (W1) → Abtippen (W3) → Tippfehler → Rückfragen und Nacharbeit (W2) → längere Durchlaufzeit.
2. **Entscheidung → Erstattung:** verzögerte, subjektive Zustandserfassung (W7) → verzögerte oder uneinheitliche Entscheidung (W8) → Eskalation per E-Mail (W9) → späte Rückerstattung (W12) → Nachfragen der Kund:innen (W13).
3. **Transparenz:** kein Status-Tracking (W6) → Nachfragen (W13), keine Kapazitätsplanung im Lager (W15), rückblickendes Reporting (W14).

## 2. Detailtabelle

| Nr. | Schritt | Element (Name, ID) | Typ | Schwachstelle | Beleg | Auswirkung | Maßnahme im Soll-Prozess | Umsetzung | A | E |
|---|---|---|---|---|---|---|---|---|---|---|
| W1 | Anmeldung über drei Kanäle | Startereignisse Webshop, Marktplatz, E-Mail (`s_web`, `s_mkt`, `s_mail`), Marktplatz-Portal sichten (`t_mkt`) | MB | Webshop, Marktplätze (eigene APIs) und E-Mail haben eigene Formate, keine gemeinsame Datenbasis. Eine Bündelung auf einen Kanal ist nicht möglich, weil die Marktplätze ihre eigenen Wege vorgeben. | A (Schritt 1, Schwachstelle 1) | Uneinheitliche Daten, hoher Erfassungsaufwand | Alle Kanäle münden in einen zentralen Retourenfall mit einheitlichem Datenmodell; Marktplätze per REST angebunden | System/Service (Sprint 2, 4) | h | h |
| W2 | E-Mail-Anmeldung lesen | E-Mail lesen und abtippen (`t_mail`) | MB, FQ | Freitext, fehlende Angaben, Rückfragen | A (Schritt 1 „formlos per E-Mail“), H (Rückfragen, fehlende Angaben) | Verzögerung, Tippfehler | Extraktion der Daten aus dem E-Mail-Text | KI (Sprint 5) | m | m |
| W3 | Erfassung im ERP | Anmeldung im ERP erfassen (`t_erp`) | MB, FQ | Doppelerfassung: Daten werden aus Portal oder E-Mail abgetippt; Übertragungsfehler, falsch gelesene oder verrutschte Spalten | A (Schritt 2 „manuell“), H (Fehlerbild) | Fehler, Zeitverlust, Kosten durch reine Bearbeitung | Der Fall wird automatisch angelegt | System/Service | h | m |
| W4 | Prüfung der Anmeldung | Anmeldung im ERP erfassen (`t_erp`), Anmeldung gültig? (`gw_ok`), Ablehnung per E-Mail (`t_rej`) | FQ | Prüfung nach Erfahrung, keine feste Vorprüfung; auch abgelaufene Fristen werden von Hand aussortiert | H (Aufgabe nennt nur „Prüfung der Anmeldung“, Frist und Artikel sind Annahmen) | Uneinheitliche Entscheidungen, Zeitverlust | Regelbasierte Vorprüfung (Frist, Artikel, Kundenhistorie) | Regeln/DMN (Sprint 4) | m | n |
| W5 | Label erstellen und senden | Label im Versandtool erstellen (`t_label`), Label per E-Mail senden (`t_send`) | WZ, MB | Eine Person erstellt das Label im Versandtool (nur wenige Klicks) und versendet es per E-Mail | A (Schritt 3), H („wenige Klicks“, Wartezeit) | Wartezeit für Kund:in, Personalaufwand pro Fall | Automatische Labelerzeugung per Versand-API, Versand per Nachricht | System/Service | m | n |
| W6 | Rückversand | Label per E-Mail senden (`t_send`), Paket trifft im Lager ein (`ev_wait`) | MB | Informationsbruch: kein Status-Tracking bis Eintreffen im Lager (Intransparenz) | A (Schritt 4, Schwachstelle 2) | Kund:innen fragen nach, Management ohne Überblick | Status-Events (u. a. Wareneingang bestätigen); Timer (21 Tage) für nicht eingegangene Ware | Messaging, Workflow | m | m |
| W7 | Wareneingang und Zustandsprüfung | Zustand prüfen und im WMS erfassen (`t_gr`) | WZ, FQ | Erfassung im WMS erfolgt oft verzögert; Zustand (neuwertig/beschädigt/getragen) wird subjektiv bewertet. Das Öffnen und Beurteilen der Ware bleibt Handarbeit. | A (Schritt 5, Schwachstelle 3) | Verzögerte Erstattung, uneinheitliche Bewertung | Erfassung direkt am Wareneingang über gRPC-Service mit fester Zustandsskala | Mensch, Service (gRPC) (Sprint 2, 4) | h | m |
| W8 | Entscheidung Annahme/Ablehnung | Annahme / Ablehnung entscheiden (`t_dec`), Entscheidung? (`gw1`) | FQ | Entscheidung nicht dokumentiert, nicht regelbasiert | A (Schritt 6, Schwachstelle 3) | Streit mit Kund:innen, keine Nachvollziehbarkeit | DMN-Regeln auf Zustand, Warenwert, Betrugsscore; Entscheidung wird gespeichert | Regeln/DMN | h | m |
| W9 | Grenzfall-Eskalation | Grenzfall per E-Mail eskalieren (`t_esc`) | MB, WZ | Eskalation und Dokumentation laufen per E-Mail, ohne Frist; für die Auswertung muss später wieder in Excel übertragen werden | A (Schritt 6 „in E-Mails dokumentiert“), H (ohne Frist, Übertrag in Excel) | Lange Liegezeit, Informationsverlust | Menschliche Aufgabe im Workflow mit dokumentierter Entscheidung | Mensch | m | n |
| W10 | Betrugs-/Missbrauchserkennung | Annahme / Ablehnung entscheiden (`t_dec`); eine Betrugsprüfung fehlt im Ist-Modell | FQ | Missbrauchsrisiko: Wardrobing wird nicht systematisch erkannt (Kontrolllücke) | A (Schwachstelle 5) | Wirtschaftlicher Schaden | Betrugsscore aus Kundenhistorie und Mustern | KI-Agent (Sprint 5) | h | m |
| W11 | Bestandsbuchung | Bestand im ERP buchen (`t_stock`) | MB, FQ | Doppelerfassung: Rückbuchung im ERP-Frontend von Hand | A (Schritt 7, Systemliste), H (Handarbeit im Frontend, Abweichungen) | Bestandsabweichungen | Bot bucht im ERP-Frontend | RPA (Sprint 5) | m | m |
| W12 | Rückerstattung | Erstattungsart? (`gw_ref`), Rückerstattung veranlassen (`t_pay`), Gutschein im CRM (`t_voucher`) | WZ, MB | Doppelerfassung: Buchhaltung veranlasst Erstattung beim Zahlungsdienstleister bzw. Gutschein im CRM von Hand | A (Schritt 8, Schwachstelle 6) | Haupttreiber der Durchlaufzeit (8–12 Tage) | Automatische Erstattung per API nach Annahme; Fallback als User Task | System/Service | h | m |
| W13 | Kundenkommunikation | Kund:in per E-Mail informieren (`t_comm`) | WZ | Verspätete E-Mails, häufige Nachfragen (Intransparenz) | A (Schritt 9) | Mehraufwand im Kundenservice, Unzufriedenheit | Automatische Statusnachrichten bei jedem Statuswechsel | Messaging | m | n |
| W14 | Reporting | Monatsende (`s_rep`), Reporting in Excel (`t_report`) | MB | Monatliches Excel-Reporting aus E-Mails und Listen, nur rückblickend (Doppelerfassung, Intransparenz) | A (Schritt 10) | Management sieht Probleme zu spät | Events speisen Dashboard und Process Mining | Messaging, Process Mining (Sprint 3) | m | m |
| W15 | Planung des Wareneingangs | Label per E-Mail senden (`t_send`), Paket trifft ein (`ev_wait`), Wareneingang (`t_gr`) | WZ | Ohne Tracking ist das Aufkommen der nächsten Tage unbekannt; die Schichtplanung berücksichtigt Spitzentage nicht (Intransparenz) | H (abgeleitet aus Schritt 4) | Rückstau am Wareneingang, verzögerte Zustandsprüfung | Tracking-Events bzw. Auswertung des Aufkommens je Tag für die Schichtplanung. Sendungsverfolgung verursacht Kosten; Nutzen gegen Aufwand abwägen, da die Arbeit dadurch nicht automatisch kleiner wird | Messaging, Process Mining | m | m |
| W16 | Ablehnung | Annahme / Ablehnung entscheiden (`t_dec`), Rückversand / Entsorgung / Spende (`t_reject`) | FQ, WZ | Auch eindeutige Standardfälle werden von Menschen bearbeitet | H (abgeleitet aus Schritt 6 und Schwachstelle 3) | Hohe Bearbeitungskosten | DMN entscheidet Standardfälle, der Mensch nur Grenzfälle | Regeln/DMN, Mensch | m | n |
| W17 | Retourengrund | Startereignisse (`s_web`, `s_mkt`, `s_mail`) | MB | Der Grund wird nicht strukturiert erfasst oder ausgewertet; Freitext aus den Kanälen | A (Schritt 10 „Gründe“), H (nicht strukturiert erfasst) | Keine Auswertung der Gründe, keine Ableitung von Maßnahmen | Klassifikation des Grundes aus Freitext | KI-Agent (Sprint 5) | m | m |

Die Spalte „Auswirkung“ ist durchgängig abgeleitet; belegt ist nur die Durchlaufzeit von 8–12 Tagen bei W12.

## 3. Priorisierung (Vorschlag)

Bewertung nach Auswirkung (A) und Aufwand (E). Bei Gleichstand zuerst, was **Ziel 1 des Managements** (Durchlaufzeit verkürzen) beeinflusst, danach **Ziel 3** (Missbrauch frühzeitig erkennen). Ziel 2 (Transparenz schaffen) deckt die Gruppe W6, W13, W14, W15 ab.

| Ziel des Managements (Ausgangsszenario) | Schwachstellen |
|---|---|
| 1 Durchlaufzeiten verkürzen | W3, W5, W7, W8, W9, W12, W16 |
| 2 Transparenz schaffen | W6, W13, W14, W15, W17 |
| 3 Missbrauch frühzeitig erkennen | W10 |

Alle sechs mit A = h (W1, W3, W7, W8, W10, W12) haben bis auf W1 den Aufwand m. Top 5:

1. **W12 Rückerstattung** (A h, E m): größter Einzeltreiber der Durchlaufzeit, in der Aufgabe belegt (8–12 Tage).
2. **W7 Wareneingang/Zustand** (A h, E m): verzögert alle nachfolgenden Schritte.
3. **W8 Entscheidung** (A h, E m): Grundlage für nachvollziehbare Ergebnisse.
4. **W3 Erfassung im ERP** (A h, E m): Fehlerquelle und Zeitverlust.
5. **W10 Betrugserkennung** (A h, E m): Ziel 3 des Managements.

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

| Nr. | Hypothese | W | KPI |
|---|---|---|---|
| H1 | Innerhalb von ShopEasy (ab Wareneingang) liegt der größte Zeitanteil zwischen Zustandserfassung und Rückerstattung. | W7, W12 | K1b, K2, K3 |
| H2 | Fälle aus dem E-Mail-Kanal haben mehr Schleifen und längere Durchlaufzeiten als Webshop-Fälle. | W2 | K7, K5b |
| H3 | Grenzfälle und Rückfragen erzeugen Varianten mit hoher Durchlaufzeit. | W9 | K4, K5b |
| H4 | Das Aufkommen im Wareneingang schwankt je Wochentag deutlich. | W15 | K15 |
| H5 | Der Rückversand der Kund:in macht einen großen Teil der 8–12 Tage aus. | W6 | K1a gegenüber K1b |

H5 klärt zugleich, ab wann die „8–12 Tage“ der Aufgabe zählen (ab Anmeldung oder ab Wareneingang).

## 6. Annahmen, die im Team oder mit dem Dozenten zu klären sind
- Eine Ablehnung schon bei der Anmeldung (Frist, Artikel; `gw_ok`, `t_rej`) steht so nicht in der Aufgabe (W4).
- Die Entscheidung über Annahme oder Ablehnung liegt im Kundenservice (Lane im Ist-Modell).
- Versandtool und Zahlungsdienstleister haben eine Schnittstelle, das ERP nicht (Grundlage der Umsetzungsart im Soll-Prozess).
- Startpunkt der „8–12 Tage“: Anmeldung oder Wareneingang.
