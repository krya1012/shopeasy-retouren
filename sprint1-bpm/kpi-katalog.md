# KPI-Katalog

Grundlage ist der KPI-Katalog von Christian Sticker (12.10.2026, K1–K11). Ergänzt sind K12 und K13, damit auch die wichtigsten Wartezeiten der Schwachstellenanalyse gemessen werden. Die Werte kommen in Sprint 3 aus dem Event-Log (Celonis).

Aus der Aufgabenstellung bekannt sind nur zwei Ist-Werte: **Retourenquote 25–35 %** und **Durchlaufzeit bis zur Rückerstattung 8–12 Tage**. Alle anderen Werte werden erst in Sprint 3 gemessen. Die Spalte „Ziel“ ist ein **Vorschlag der KI** und im Team abzustimmen (Prüfliste in `AI-USAGE.md`).

## 1. KPIs

| Nr. | KPI | Definition | Benötigte Daten | Schwachstelle | Ziel |
|---|---|---|---|---|---|
| K1 | Durchlaufzeit | Zeit von der Retourenanmeldung bis zum Abschluss des Retourenfalls | Retouren-ID, Zeitpunkt Anmeldung, Zeitpunkt Abschluss | W5, W6, W12 (Wartezeiten) | senken (Ist laut Aufgabe 8–12 Tage) |
| K2 | Bearbeitungszeit nach Wareneingang | Zeit vom Eingang im Lager bis zur Annahme-/Ablehnungsentscheidung | Zeitpunkt Wareneingang, Zeitpunkt Entscheidung | W7, W8 | senken |
| K3 | Grenzfallquote | Anteil der Retourenfälle, die eskalieren und in E-Mails dokumentiert werden | Retouren-ID, Eskalationen, E-Mails | W9, W16 | senken |
| K4 | Fehlerquote | Anteil der Retouren, bei denen ein Bearbeitungsfehler passiert ist (siehe Abschnitt 2) | Retouren-ID, Fehlerkennzeichnung | W2, W3, W4, W11 (Fehlerquellen) | senken |
| K5 | Retourenquote | Anteil der Bestellungen, die zurückgeschickt worden sind | Bestell-ID, Bestellungen, zugehörige Retouren | W14 | beobachten (Ist laut Aufgabe 25–35 %) |
| K6 | Neuwertigquote | Anteil der geprüften Retouren, die als neuwertig eingestuft werden | Retouren-ID, erfasster Zustand | W7 | beobachten |
| K7 | Beschädigtquote | Anteil der geprüften Retouren, die als beschädigt eingestuft werden | Retouren-ID, erfasster Zustand | W7 | beobachten |
| K8 | Getragenquote | Anteil der geprüften Retouren, die als getragen eingestuft werden | Retouren-ID, erfasster Zustand | W7, W10 (Missbrauch) | beobachten |
| K9 | Rückversandquote | Anteil der abgeschlossenen Retourenfälle, bei denen die Ware an den Kunden zurückgesendet wurde | Retouren-ID, dokumentierter Rückversand | W16 | beobachten |
| K10 | Entsorgungsquote | Anteil der abgeschlossenen Retourenfälle, bei denen die Ware entsorgt wurde | Retouren-ID, dokumentierte Entsorgung | W16 | beobachten |
| K11 | Spendenquote | Anteil der abgeschlossenen Retourenfälle, bei denen die Ware gespendet wurde | Retouren-ID, dokumentierte Spende | W16 | beobachten |
| K12 | Zeit bis zur Rückerstattung (Ergänzung) | Zeit von der Entscheidung „Annahme“ bis zur Rückerstattung | Zeitpunkt Entscheidung, Zeitpunkt Rückerstattung | W12 (Platz 1 der Priorisierung) | senken |
| K13 | Zeit bis zum Label (Ergänzung) | Zeit von der Retourenanmeldung bis zum Versand des Labels an die Kund:in | Zeitpunkt Anmeldung, Zeitpunkt Label versendet | W5, W13 | senken |

## 2. Fehlerquote (K4)

Ein Fall zählt als fehlerhaft, wenn mindestens einer dieser Punkte zutrifft:
- Daten wurden nach der Erfassung korrigiert oder die Kund:in musste nachgefragt werden (W2, W3),
- die Entscheidung wurde nachträglich geändert, z. B. Erstattung nach einer Ablehnung (W4, W8),
- der Lagerbestand musste korrigiert werden (W11).

Die Fehlerkennzeichnung steht vermutlich nicht direkt im Log; dann wird sie aus wiederholten Aktivitäten abgeleitet (Frage 3 in Abschnitt 4).

## 3. So rechnen wir

- Zeit-KPIs (K1, K2, K12, K13): nur abgeschlossene Fälle; Median in Tagen bzw. Stunden. Offene Fälle werden gesondert gezählt, sonst sehen die Zeiten zu gut aus.
- Quoten (K3–K11): Anteil in Prozent, Nenner steht in der Definition.
- Alle KPIs zusätzlich je Kanal (Webshop, Marktplatz, E-Mail) und je Monat auswerten.

## 4. Offene Fragen

1. Ab wann zählen die „8–12 Tage“ (Anmeldung oder Wareneingang; Kalender- oder Arbeitstage)? Frage an den Dozenten. K1 misst ab Anmeldung, K2 und K12 decken den Teil ab Wareneingang ab.
2. Sind Entscheidung und Eskalation im Log enthalten? Laut Aufgabe läuft die Eskalation per E-Mail und die Entscheidung ist nicht dokumentiert (W8, W9). Wenn nicht, schätzen wir K2, K3 und K12 aus anderen Zeitpunkten (z. B. Zustand erfasst, Bestand gebucht) oder aus einer Stichprobe der E-Mails.
3. Welche Spalten hat das Log (Fall-ID, Aktivität, Zeitstempel, Kanal, Zustand, Entscheidung, Fehlerkennzeichnung)? Die Namen der Aktivitäten werden in Sprint 3 mit dem Log abgeglichen.

## 5. Vorlage zum Befüllen in Sprint 3

| KPI | Ist-Wert | Ziel | Bemerkung (Zeitraum, Filter, Berechnungsweg) |
|---|---|---|---|
| K1 | | senken (Ist laut Aufgabe 8–12 Tage) | |
| K2 | | senken | |
| K3 | | senken | |
| K4 | | senken | |
| K5 | | beobachten (Ist laut Aufgabe 25–35 %) | |
| K6 | | beobachten | |
| K7 | | beobachten | |
| K8 | | beobachten | |
| K9 | | beobachten | |
| K10 | | beobachten | |
| K11 | | beobachten | |
| K12 | | senken | |
| K13 | | senken | |
