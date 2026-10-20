# Schwachstellenanalyse Ist-Prozess

Grundlage ist das Dokument „Prozess Analyse der Shop Easy GmbH“ von Eduard Moser. (siehe `AI-USAGE.md`).

Unter jedem Text steht **„Im Ist-Modell“**: die zugehörigen Schwachstellen mit ihrer Nummer (W1–W17, so stehen sie als Hinweise in `ist-prozess.bpmn`), der Kategorie und dem **Namen des Schritts im Diagramm**. Die Maßnahmen im Soll-Prozess stehen in `soll-prozess-realisierung.md`.

**Kategorien** (Vorgabe der Aufgabe): **Medienbruch** (Daten wechseln das System und werden abgetippt), **Wartezeit** (der Fall wartet), **Fehlerquelle** (Fehler oder unklare Entscheidungen sind möglich).

**„Ergänzung (Aufgabe)“** markiert Punkte, die den Automatisierungs Prozess ergänzen sollten, aber nicht in der Aufgabenstellung oder im Ist-Diagramm vorkommen.

## 1. Retoureneingang

Der Prozess beginnt damit, dass der Kunde über verschiedene Kanäle eine Retoure einleiten kann. Das liegt daran, dass die ShopEasy GmbH ihre Produkte über verschiedene Marktplätze vertreibt. Da es schwer wird, alle Retouren auf nur einen Kanal zu bündeln, müssen die Retouren auch weiterhin über verschiedene Kanäle in unsere Retourenannahme einfließen.

Die Retourenannahme wird ausschließlich durch Menschen bearbeitet. Das ist sehr aufwendig, da es hohe Kosten verursacht.

**Im Ist-Modell**
- **W1**, Medienbruch: Drei Kanäle ohne gemeinsame Datenbasis. Schritte: „Retoure im Webshop-Formular angemeldet“, „Retoure im Marktplatz-Tool angemeldet“, „Retoure formlos per E-Mail angemeldet“, „Marktplatz-Portal sichten, Daten abtippen“.
- **W3**, Medienbruch und Fehlerquelle: Die Annahme wird von Hand erfasst. Schritt: „Anmeldung im ERP manuell erfassen & prüfen“.
- **W2**, Medienbruch und Fehlerquelle (Ergänzung, Aufgabe): E-Mails kommen formlos, Angaben fehlen oder müssen nachgefragt werden. Schritt: „E-Mail lesen, Daten nachfragen, abtippen“.

## 2. Rückversandlabel

Die Rückversandlabel werden auch durch eine Person erstellt. Das ist wiederum zu aufwendig, da es sich nur um wenige Klicks handelt und das auch automatisiert werden kann.

**Im Ist-Modell**
- **W5**, Wartezeit und Medienbruch: Das Label wird von Hand im Versandtool erstellt und per E-Mail verschickt, die Kund:in wartet. Schritte: „Retourenlabel manuell im Versandtool erstellen“, „Label per E-Mail an Kund:in senden“.

## 3. Rückversand durch den Kunden

Dann gibt es den Zeitpunkt, an dem die Kleidung/Produkte durch den Kunden zurückversandt werden. Eine Rückversendung mit Sendungsverfolgung wird teurer sein, aber es sollte möglich sein, die Rückversandlabel zu tracken. Wenn das gemacht wird, könnte man herausfinden, welcher Umfang an Waren in den nächsten Tagen kommen wird. Ob das dazu führt, dass die Arbeit besser zu bewältigen ist, ist eher unwahrscheinlich. Eine andere Möglichkeit wäre, die Flut an Rückversendungen auszuwerten und an Tagen mit großem Aufkommen die Schichten so einzuteilen, dass die Zeit der manuellen Bearbeitung der Päckchen verkürzt wird. Die manuelle Öffnung und Auswertung des Zustands lässt sich nicht richtig einfach automatisieren.

**Im Ist-Modell**
- **W6**, Medienbruch: Kein Tracking, ShopEasy erfährt erst im Lager vom Paket. Schritt: „Paket trifft im Lager ein“.
- **W15**, Wartezeit: Das Aufkommen ist schlecht planbar, es kann sein das die Schichtplanung Spitzentage nicht nerücksichtigt. Schritt: „Paket trifft im Lager ein“.
- **W7**, Wartezeit und Fehlerquelle: Öffnen und Bewerten der Ware ist Handarbeit. Ergänzung (Aufgabe): Die Erfassung im WMS erfolgt oft verzögert und die Bewertung ist subjektiv. Schritt: „Zustand prüfen (neuwertig / beschädigt / getragen), im WMS erfassen“.

## 4. Prüfung der Retoure

Die Prüfung, ob die Retoure auch gerechtfertigt ist, wird wiederum durch einen Menschen gemacht. Das könnte durch eine Vorprüfung erfolgen, die zumindest schon einmal alle Retouren aussortiert, die aufgrund von Fristen nicht zulässig sind.

**Im Ist-Modell**
- **W4**, Fehlerquelle: Die Prüfung erfolgt nach Erfahrung, eine Vorprüfung der Fristen fehlt. Die Prüfung auf Frist und Artikel schon bei der Erfassung steht nicht in der Aufgabe, das ist unsere Annahme. Schritte: „Anmeldung im ERP manuell erfassen & prüfen“, „Anmeldung gültig?“, „Ablehnung per E-Mail mitteilen“.
- **W8**, Fehlerquelle (Ergänzung, Aufgabe): Die Entscheidung über Annahme oder Ablehnung ist nicht dokumentiert und folgt keinen festen Regeln. Schritte: „Annahme / Ablehnung entscheiden“, „Entscheidung?“.
- **W9**, Medienbruch und Wartezeit (Ergänzung, Aufgabe): Grenzfälle werden nur per E-Mail eskaliert und dokumentiert. Schritt: „Grenzfall per E-Mail eskalieren & dokumentieren“.
- **W10**, Fehlerquelle (Ergänzung, Aufgabe): Missbrauch, z. B. Ware tragen und zurückschicken, wird nicht systematisch erkannt; im Diagramm gibt es dafür keinen Schritt. Nächster Schritt: „Annahme / Ablehnung entscheiden“.

## 5. Retoure angenommen

Wenn die Retoure angenommen wird, muss diese von Hand in das ERP-System zurückgebucht werden, und auch die Zahlung wird von Hand veranlasst. Das sind Arbeiten, die auch von einem automatisierten Prozess bearbeitet werden können.

**Im Ist-Modell**
- **W11**, Medienbruch und Fehlerquelle: Die Rückbuchung im ERP erfolgt von Hand, Bestandsabweichungen sind möglich. Schritt: „Bestand im ERP buchen“.
- **W12**, Wartezeit und Medienbruch: Die Buchhaltung erstattet von Hand. Ergänzung (Aufgabe): Das ist der Haupttreiber der Durchlaufzeit von 8–12 Tagen. Schritte: „Erstattungsart?“, „Rückerstattung manuell veranlassen“, „Gutschein manuell im CRM eintragen“.

## 6. Retoure abgelehnt

Wenn die Retoure abgelehnt wird, müssen immer von Menschen auch die Standardfälle bearbeitet werden. Das ist zu aufwendig und kann automatisiert werden.

**Im Ist-Modell**
- **W16**, Fehlerquelle und Wartezeit: Auch eindeutige Standardfälle bearbeitet ein Mensch (Annahme, abgeleitet aus der Aufgabe). Schritte: „Annahme / Ablehnung entscheiden“, „Rückversand / Entsorgung / Spende veranlassen“.

## 7. Dokumentation und Auswertung

Das Dokumentieren in E-Mails ist leider nicht sehr gut auswertbar und muss deshalb zu Auswertungszwecken immer noch einmal in Excel übertragen werden, um eine monatliche Auswertung zu erstellen.

**Im Ist-Modell**
- **W14**, Medienbruch: Das Reporting ist manuell, monatlich und nur rückblickend. Schritte: „Monatsende“, „Monatliches Reporting in Excel erstellen“.
- **W13**, Wartezeit (Ergänzung, Aufgabe): Die Kund:in wird verspätet informiert und fragt häufig nach. Schritt: „Kund:in per E-Mail informieren (verzögert)“.

## 8. Allgemein

In dem Prozess gibt es viele manuelle Tätigkeiten, die man automatisieren könnte. Manuelle Tätigkeiten können häufig zu Fehlern bei der Übertragung führen. Es kann auch zu Fehleinschätzungen durch falsches Lesen oder Verrutschen in Spalten kommen. Diese Arbeiten sollten besser automatisiert werden.

(Retourengrundanalyse muss noch in den Prozess eingefügt werden.  zB Noch prüfen, ob der Hauptretourengrund zu klein ist, möglicherweise die Größe der Artikelbeschreibung anpassen.)

**Im Ist-Modell**
- **W17**, Medienbruch: Der Retourengrund wird nicht strukturiert erfasst und ausgewertet (Freitext aus den Kanälen). Schritte: die drei Startereignisse „Retoure im Webshop-Formular angemeldet“, „Retoure im Marktplatz-Tool angemeldet“, „Retoure formlos per E-Mail angemeldet“.
- Die Fehler beim Abtippen gehören zu **W3**, **W2** und **W11** (siehe oben).

## Übersicht nach Kategorien

| Kategorie | Schwachstellen |
|---|---|
| Medienbruch | W1, W2, W3, W5, W6, W9, W11, W12, W14, W17 |
| Wartezeit | W5, W7, W9, W12, W13, W15, W16 |
| Fehlerquelle | W2, W3, W4, W7, W8, W10, W11, W16 |
