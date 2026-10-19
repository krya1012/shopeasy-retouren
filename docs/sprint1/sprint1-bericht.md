# Sprint 1 – Business Process Management (Abgabe 20.10., 11:30)

## Inhalt der Abgabe
- `bpmn/ist-prozess.bpmn` – Ist-Prozess, drei Eingangskanäle (Webshop, Marktplatz, E-Mail), vier Lanes
- `bpmn/soll-prozess.bpmn` – Soll-Prozess mit Umsetzungsart je Aktivität
- `docs/sprint1/schwachstellenanalyse.md`
- `docs/sprint1/kpi-katalog.md`

## Ist-Prozess
Die zehn Schritte der Aufgabenstellung sind in vier Lanes modelliert (Kund:in, Kundenservice, Lager, Buchhaltung). Die drei Startereignisse münden über kanalspezifische manuelle Schritte in die Erfassung im ERP. Die Entscheidung hat die Ausgänge Annahme, Ablehnung und Grenzfall; Grenzfälle laufen per E-Mail-Eskalation zurück in die Entscheidung. Alle Schritte sind manuell, daher `manualTask`. Die beteiligten Systeme aus der Aufgabenstellung (Webshop-Frontend, Marktplatz-Portale, ERP, WMS, CRM, Zahlungsdienstleister, E-Mail-Postfach, Excel) sind als Datenspeicher an den Aufgaben modelliert, die sie nutzen; zusätzlich das Versandtool aus Schritt 3. Bei der Rückerstattung (Schritt 8) entscheidet ein Gateway zwischen Geld über den Zahlungsdienstleister und Gutschein im CRM.

## Soll-Prozess – wichtigste Änderungen
1. Alle Kanäle münden in einen zentralen Retourenfall (REST/Messaging).
2. Regelbasierte Prüfung (DMN) und KI-Klassifikation mit Betrugsscore vor der Freigabe.
3. Automatische Labelerzeugung; Status-Nachrichten an Kund:in bei jedem Statuswechsel.
4. Eventbasiertes Warten auf den Wareneingang mit Timer (21 Tage).
5. Zustandserfassung am Wareneingang über gRPC-Service.
6. Entscheidung per DMN; Grenzfälle als dokumentierte menschliche Aufgabe.
7. Bestandsbuchung (RPA) und Rückerstattung (Service) parallel.
8. Fehlerbehandlung: Ausfall der Versand-API und fehlgeschlagene Zahlung führen zu manuellen Aufgaben.
9. Events ersetzen das manuelle Excel-Reporting.

Die Wahl der Umsetzungsart je Aktivität (mit Begründung, verworfener Alternative und Fehlerfall) steht in `docs/sprint1/soll-prozess-realisierung.md`. Von 19 Aufgaben laufen 13 automatisch, eine ist hybrid (Mensch + gRPC-Service), fünf sind menschlich (davon drei Ausnahmepfade).

## Umsetzungsarten im Soll-Prozess

| Art | Aktivitäten |
|---|---|
| System/Service | Fall anlegen, Label erzeugen, Statusnachrichten, Rückerstattung |
| Regeln (DMN) | Anmeldung prüfen, Annahme/Ablehnung entscheiden |
| Mensch | Verdacht prüfen, Grenzfall prüfen, Zustand prüfen, Weiterverwendung festlegen, Fallback-Aufgaben |
| RPA | Bestand im ERP buchen/korrigieren |
| KI | E-Mail auslesen, Retourengrund klassifizieren, Betrugsscore |

## Offene Punkte
- Modelle in Camunda Modeler öffnen, Layout prüfen und bei Bedarf nachbessern (die Diagramme wurden aus einem Skript erzeugt).
- Aktivitätsnamen des KPI-Katalogs mit der Log-Datei aus Sprint 3 abgleichen.
- Team prüft alle Inhalte und trägt Änderungen in `AI-USAGE.md` ein.
