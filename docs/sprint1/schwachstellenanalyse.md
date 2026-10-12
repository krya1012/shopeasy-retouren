# Schwachstellenanalyse Ist-Prozess

Bezug: `bpmn/ist-prozess.bpmn`. Typen: **MB** Medienbruch · **WZ** Wartezeit · **FQ** Fehlerquelle · **DE** Doppelte Dateneingabe · **IN** Intransparenz · **MI** Missbrauchsrisiko.

| Nr. | Prozessschritt (Ist) | Typ | Schwachstelle | Auswirkung | Maßnahme im Soll-Prozess | Umsetzung |
|---|---|---|---|---|---|---|
| W1 | Anmeldung (Webshop / Marktplatz / E-Mail) | MB | Drei Kanäle mit eigenen Formaten und APIs, keine gemeinsame Datenbasis | Uneinheitliche Daten, hoher Erfassungsaufwand | Zentraler Retourenfall mit einheitlichem Datenmodell; Marktplätze per REST angebunden | System/Service (Sprint 2, 4) |
| W2 | E-Mail-Anmeldung lesen | FQ | Freitext, fehlende Angaben, Rückfragen | Verzögerung, Tippfehler | Extraktion der Daten aus E-Mail | KI (Sprint 5) |
| W3 | Erfassung im ERP | DE | Daten werden aus Portal/Mail manuell abgetippt | Fehler, Zeitverlust | Automatische Anlage des Falls durch die Plattform | System/Service |
| W4 | Prüfung der Anmeldung | FQ | Prüfung nach Erfahrung, keine festen Regeln | Uneinheitliche Entscheidungen | Regelbasierte Prüfung (Frist, Artikel, Kundenhistorie) | Regeln/DMN (Sprint 4) |
| W5 | Label manuell erstellen und per E-Mail senden | WZ, MB | Manueller Schritt im Versandtool, Versand per E-Mail | Wartezeit für Kund:in, Aufwand pro Fall | Automatische Labelerzeugung per Versand-API und Versand per Nachricht | System/Service |
| W6 | Warenrückversand | IN | Kein Status-Tracking bis Eintreffen im Lager | Kund:innen fragen nach, Management ohne Überblick | Statusereignisse; Timer (21 Tage) für nicht eingegangene Ware | Messaging, Workflow |
| W7 | Wareneingang/Zustandsprüfung im WMS | WZ, FQ | Erfassung oft verzögert; Zustand subjektiv | Verzögerte Erstattung; uneinheitliche Bewertung | Erfassung direkt am Eingang über gRPC-Service mit definierter Zustandsskala | System + Mensch (Sprint 2) |
| W8 | Entscheidung Annahme/Ablehnung | FQ | Entscheidung undokumentiert und subjektiv | Streit mit Kund:innen, keine Nachvollziehbarkeit | DMN-Regeln auf Zustand, Warenwert, Betrugsscore; Entscheidung wird gespeichert | Regeln/DMN |
| W9 | Grenzfall-Eskalation per E-Mail | MB, WZ | Dokumentation im Postfach, keine Frist | Lange Liegezeit, Informationsverlust | Menschliche Aufgabe (User Task) im Workflow mit dokumentierter Entscheidung | Mensch |
| W10 | Betrugs-/Missbrauchserkennung | MI | Wardrobing wird nicht systematisch erkannt | Wirtschaftlicher Schaden | Betrugsscore aus Kundenhistorie und Mustern | KI-Agent (Sprint 5) |
| W11 | Bestandsbuchung im ERP | DE | Manuelle Buchung im ERP-Frontend | Bestandsabweichungen | Bot bucht im ERP-Frontend | RPA (Sprint 5) |
| W12 | Rückerstattung manuell | WZ, DE | Buchhaltung bucht von Hand beim Zahlungsdienstleister / CRM | Haupttreiber der Durchlaufzeit (8–12 Tage) | Automatische Erstattung per API nach Annahme; Fallback als User Task | System/Service |
| W13 | Kundenkommunikation | IN, WZ | Verspätete E-Mails, häufige Nachfragen | Mehraufwand im Kundenservice, Unzufriedenheit | Automatische Statusnachrichten bei jedem Statuswechsel | Messaging |
| W14 | Reporting in Excel | DE, IN | Monatlich, manuell, nur rückblickend | Management sieht Probleme zu spät | Events speisen Dashboard und Process Mining | Messaging, Process Mining (Sprint 3) |

## Abdeckung der bekannten Schwachstellen aus der Aufgabenstellung

| Bekannte Schwachstelle | Behandelt durch |
|---|---|
| Medienbrüche zwischen Kanälen, keine gemeinsame Datenbasis | W1, W2, W3 |
| Keine Ende-zu-Ende-Transparenz | W6, W13, W14 |
| Subjektive, undokumentierte Entscheidungen | W4, W7, W8, W9 |
| Mehrfache manuelle Datenpflege (ERP, WMS, CRM, Excel) | W3, W11, W12, W14 |
| Kein Erkennen von Betrugsmustern | W10 |
| Lange Durchlaufzeit (8–12 Tage) | W5, W7, W12 |

## Hypothesen für Sprint 3 (Process Mining)
- Der größte Zeitanteil liegt zwischen Wareneingang und Rückerstattung (W7, W12).
- Fälle aus dem E-Mail-Kanal haben mehr Schleifen und längere Durchlaufzeiten als Webshop-Fälle.
- Grenzfälle und Rückfragen erzeugen Varianten mit hoher Durchlaufzeit.
