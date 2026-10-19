# Schwachstellenanalyse Ist-Prozess

Bezug: `sprint1-bpm/ist-prozess.bpmn`. Die W-Nummern stehen dort als Anmerkungen im Diagramm; die Spalte „Ist-Element“ nennt die Elemente mit ihrer ID. Die Maßnahmen beziehen sich auf `sprint1-bpm/soll-prozess.bpmn` (Begründungen in `soll-prozess-realisierung.md`).

**Kategorien** (Vorgabe der Aufgabe): **MB** Medienbruch · **WZ** Wartezeit · **FQ** Fehlerquelle.

**„(Annahme)“** markiert, was nicht in der Aufgabenstellung steht, sondern von uns abgeleitet ist. Das ist im Team oder mit dem Dozenten zu bestätigen.

## 1. Übersicht

| Kategorie | Schwachstellen |
|---|---|
| Medienbruch (MB) | W1, W2, W3, W5, W6, W9, W11, W12, W14, W17 |
| Wartezeit (WZ) | W5, W7, W9, W12, W13, W15, W16 |
| Fehlerquelle (FQ) | W2, W3, W4, W7, W8, W10, W11, W16 |

Der Kern: Die Daten wechseln zwischen Webshop, Marktplätzen, E-Mail, ERP, Versandtool, WMS, CRM, Zahlungsdienstleister und Excel und werden dabei abgetippt. Das erzeugt Fehler und Rückfragen. Eine verzögerte Zustandserfassung und Entscheidung führt zu später Rückerstattung und zu Nachfragen der Kund:innen.

## 2. Schwachstellen

| Nr. | Typ | Ist-Element | Problem | Maßnahme im Soll | KPI |
|---|---|---|---|---|---|
| W1 | MB | Startereignisse `s_web`, `s_mkt`, `s_mail`; „Marktplatz-Portal sichten, Daten abtippen“ (`t_mkt`) | Drei Kanäle mit eigenen Formaten und ohne gemeinsame Datenbasis | Alle Kanäle münden in einen zentralen Retourenfall: `s_mkt` (REST), `t_create` (System/Service) | K1 je Kanal |
| W2 | MB, FQ | „E-Mail lesen, Daten nachfragen, abtippen“ (`t_mail`) | Freitext, fehlende Angaben, Rückfragen (Rückfragen: Annahme) | `t_parse` (KI) | K4 |
| W3 | MB, FQ | „Anmeldung im ERP manuell erfassen & prüfen“ (`t_erp`) | Doppelerfassung, Tippfehler | `t_create` (System/Service) | K4 |
| W4 | FQ | `t_erp`, „Anmeldung gültig?“ (`gw_ok`) | Prüfung nach Erfahrung, keine feste Vorprüfung (Annahme) | `t_rule` (Regeln/DMN) | K4 |
| W5 | WZ, MB | „Retourenlabel manuell im Versandtool erstellen“ (`t_label`), „Label per E-Mail an Kund:in senden“ (`t_send`) | Label von Hand erstellt und per E-Mail verschickt, Kund:in wartet | `t_label` (Service), Fallback `t_label_man` (Mensch), `t_notify` (Messaging) | K1, K13 |
| W6 | MB | `t_send`, „Paket trifft im Lager ein“ (`ev_wait`) | Kein Status-Tracking bis zum Eintreffen im Lager | `t_notify` und `t_notify_in` (Messaging), Warten auf `ev_arrive` mit Timer `ev_timeout` und `t_expired` | K1 |
| W7 | WZ, FQ | „Zustand prüfen (neuwertig / beschädigt / getragen), im WMS erfassen“ (`t_gr`) | Erfassung im WMS oft verzögert, Zustand subjektiv bewertet | `t_gr` (Mensch, feste Zustandsskala), `t_gr_rec` (gRPC-Service) | K2, K6–K8 |
| W8 | FQ | „Annahme / Ablehnung entscheiden“ (`t_dec`), „Entscheidung?“ (`gw1`) | Entscheidung nicht dokumentiert und nicht regelbasiert | `t_rule` und `t_decide` (Regeln/DMN), Grenzfälle an `t_man2` | K2 |
| W9 | MB, WZ | „Grenzfall per E-Mail eskalieren & dokumentieren“ (`t_esc`) | Eskalation nur per E-Mail, später Übertrag in Excel (Excel: Annahme) | `t_man2` (Mensch, im System dokumentiert) | K3 |
| W10 | FQ | `t_dec` (eine Betrugsprüfung fehlt) | Missbrauch (Wardrobing) wird nicht systematisch erkannt | `t_ai` (KI-Betrugsscore), Verdacht geht an `t_man1` (Mensch) | K8 |
| W11 | MB, FQ | „Bestand im ERP buchen“ (`t_stock`) | Rückbuchung von Hand im ERP-Frontend, Bestandsabweichungen | `t_stock` (RPA) | K4 |
| W12 | WZ, MB | „Erstattungsart?“ (`gw_ref`), „Rückerstattung manuell veranlassen“ (`t_pay`), „Gutschein manuell im CRM eintragen“ (`t_voucher`) | Buchhaltung erstattet von Hand; Haupttreiber der Durchlaufzeit (8–12 Tage laut Aufgabe) | `t_refund` (Service), Fallback `t_refund_man` (Mensch) | K1, K12 |
| W13 | WZ | „Kund:in per E-Mail informieren (verzögert)“ (`t_comm`) | Information verspätet, häufige Nachfragen | `t_notify`, `t_notify_in`, `t_end_notify`, `t_deny`, `t_deny2` (Messaging) | K13 |
| W14 | MB | Monatsende (`s_rep`), „Monatliches Reporting in Excel erstellen“ (`t_report`) | Reporting manuell, monatlich, nur rückblickend | `t_report` (Service, Dashboard aus Events), `t_end_notify` (Statusereignisse) | K5 |
| W15 | WZ | `ev_wait`, `t_gr` | Aufkommen im Wareneingang nicht planbar, Rückstau (Annahme) | Tracking-Events (`ev_arrive`, `ev_timeout`) für die Schichtplanung (Anmerkung im Soll-Modell) | – |
| W16 | FQ, WZ | `t_dec`, „Rückversand / Entsorgung / Spende veranlassen“ (`t_reject`) | Auch eindeutige Standardfälle bearbeitet ein Mensch (Annahme) | `t_decide` (DMN) für Standardfälle, Grenzfälle an `t_man2`, Weiterverwendung `t_reject` (Mensch) | K3, K9–K11 |
| W17 | MB | Startereignisse `s_web`, `s_mkt`, `s_mail` | Retourengrund nicht strukturiert erfasst, keine Auswertung (Annahme) | `t_parse`, `t_ai` (KI) | – |

## 3. Wichtigste Schwachstellen

**Vorschlag der KI, im Team abzustimmen.** Maßstab: großer Effekt auf Durchlaufzeit, Fehler oder Missbrauch bei vertretbarem Aufwand.

1. **W12 Rückerstattung:** größter Einzeltreiber der Durchlaufzeit (8–12 Tage laut Aufgabe).
2. **W7 Zustandserfassung:** verzögert alle nachfolgenden Schritte.
3. **W8 Entscheidung:** Grundlage für nachvollziehbare Ergebnisse.
4. **W3 Erfassung im ERP:** Fehlerquelle und Zeitverlust.
5. **W10 Betrugserkennung:** Ziel 3 des Managements.

W1 (drei Kanäle) hat ebenfalls hohe Wirkung, aber hohen Aufwand; sie wird durch die Mock-REST-API aus Sprint 2 prototypisch umgesetzt.

| Ziel des Managements | Schwachstellen |
|---|---|
| 1 Durchlaufzeiten verkürzen | W3, W5, W7, W8, W9, W12, W16 |
| 2 Transparenz schaffen | W6, W13, W14, W15, W17 |
| 3 Missbrauch frühzeitig erkennen | W10 |

## 4. Abdeckung der bekannten Schwachstellen aus der Aufgabenstellung

| Bekannte Schwachstelle | Behandelt durch |
|---|---|
| Medienbrüche zwischen Kanälen, keine gemeinsame Datenbasis | W1, W2, W3, W17 |
| Keine Ende-zu-Ende-Transparenz | W6, W13, W14, W15 |
| Subjektive, undokumentierte Entscheidungen | W4, W7, W8, W9, W16 |
| Mehrfache manuelle Datenpflege (ERP, WMS, CRM, Excel) | W3, W11, W12, W14 |
| Kein Erkennen von Betrugsmustern | W10 |
| Lange Durchlaufzeit (8–12 Tage) | W5, W7, W12, W15 |

## 5. Annahmen, die im Team oder mit dem Dozenten zu klären sind

- Eine Ablehnung schon bei der Anmeldung (Frist, Artikel; `gw_ok`, `t_rej`) steht so nicht in der Aufgabe (W4).
- Die Entscheidung über Annahme oder Ablehnung liegt im Kundenservice.
- Versandtool und Zahlungsdienstleister haben eine Schnittstelle, das ERP nicht (Grundlage der Umsetzungsart im Soll).
- Ab wann zählen die „8–12 Tage“: ab Anmeldung oder ab Wareneingang?
