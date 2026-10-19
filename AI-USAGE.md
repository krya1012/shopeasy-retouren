# Dokumentation der KI-Nutzung

Laut Aufgabenstellung führt undokumentierte KI-Nutzung zum Nicht-Bestehen. Jeder Eintrag: Datum, Werkzeug, Zweck, Prüfstatus. Jede Person dokumentiert ihre eigene KI-Nutzung hier.
Die 2-seitige persönliche Zusammenfassung (Sprint 6) wird ohne KI erstellt.

| Datum | Werkzeug | Zweck / Artefakt | Prüfstatus (Team) |
|---|---|---|---|
| 2026-10-12 | Claude Code | Analyse der Aufgabenstellung, Repo-Grundgerüst, Entwurf Project Charter, Sprint-1-Artefakte (Ist-/Soll-BPMN, Schwachstellen, KPI-Katalog) | offen – siehe Prüfliste |
| 2026-10-19 | Claude Code | Abgleich der Aufgabenstellung mit dem Ist-Modell; Skript `tools/gen_bpmn.py` (erzeugt beide BPMN-Dateien und prüft das Layout); Ist-Modell um Datenspeicher für die Systeme und das Gateway „Erstattungsart?“ ergänzt | offen – siehe Prüfliste |
| 2026-10-19 | Claude Code | Überarbeitung `docs/sprint1/schwachstellenanalyse.md` (Gliederung nach Medienbrüchen/Wartezeiten/Fehlerquellen, Element-IDs, Priorisierung, W15–W17) und KPI K16. Inhalte aus dem Dokument „Prozess Analyse der Shop Easy GmbH“ eines Teammitglieds wurden dabei in eigener Formulierung übernommen; dieses Dokument enthält selbst eine Zusammenfassung, die mit Claude erstellt wurde. | offen – siehe Prüfliste |
| 2026-10-19 | Claude Code | Überarbeitung `docs/sprint1/kpi-katalog.md` (Kern-KPIs, Fehlerquote in drei Teilen, Log-Schema, Zuordnung zu Modellelementen, Vorlage für Sprint 3). Alle Zielwerte sind Vorschläge der KI | offen – siehe Prüfliste |
| 2026-10-19 | Claude Code | `docs/sprint1/soll-prozess-realisierung.md` (Wahl der Umsetzungsart je Soll-Aktivität mit Begründung und Alternativen) sowie zwei Anmerkungen (W13, W15) im Soll-Modell über `tools/gen_bpmn.py` | offen – siehe Prüfliste |
| 2026-10-19 | Claude Code | Review von Sprint 1 und Korrekturen: `tools/gen_bpmn.py` erweitert (externe Pools, Nachrichtenflüsse, Beschriftungen); Ist-Modell überarbeitet (Pools statt Kund:innen-Lane, Prüf-Gateway mit Ablehnung, Reporting als eigener Ablauf, `task` statt `manualTask` bei IT-gestützten Schritten); Soll-Modell (Deadlock am parallelen Join behoben, Gateways nach Benutzeraufgaben, Datenspeicher); KPI K1 geteilt, K18/K19 ergänzt; Diagramme mit bpmn-to-image als PNG gerendert; `.gitignore` | offen – siehe Prüfliste |
| 2026-10-19 | Claude Code | Prüfung der Modelle mit lokalem Camunda 8.10.4 (`c8ctl`: Lint, Deploy, Testläufe auf ausführbaren Prüfkopien, die nicht im Repo liegen); dabei Schemafehler im Ist-Modell (Datenassoziation ohne Ziel) behoben. Die `.bpmn`-Dateien und PNGs hat Claude Code per Skript geschrieben, weil die KI den Camunda Modeler nicht bedienen kann; das Skript `tools/gen_bpmn.py` wurde danach aus dem Repo entfernt, der Camunda Modeler ist ab jetzt die Quelle der Modelle | offen – siehe Prüfliste |
| 2026-10-19 | Claude Code | Quellenbasis für die Modellierungsregeln im Review (Manual Task ohne IT, Pool = Teilnehmer / Lane = Rolle, Kennzahlenfamilien): die Lernblock-Zusammenfassungen im Kursordner (`DVG_02_BPM_Lernbloecke.md`, `DVG_06_Prozessautomatisierung_Teil1_Lernbloecke.md`). Die Dateien nennen keinen Autor und sind vermutlich KI-erstellt. Claude hat die drei Aussagen am 19.10. gegen die Originalfolien abgeglichen und bestätigt: `06_Prozessautomatisierung_Teil 1.pdf` Folie 43 (Pools und Lanes) und Folie 48 (manuelle Aktivität ohne IT gegenüber Steuerung durch die Process Engine), `02_BPM.pdf` Folie 35 (Kennzahlenfamilien) | offen – siehe Prüfliste |

## Prüfliste vor Abgabe (bis 20.10.2026, 10:00)

Nach der Prüfung ersetzt ihr „offen – siehe Prüfliste“ in der Tabelle durch das Ergebnis, z. B. „Gelesen und geprüft von Anna am 20.10.; geändert: Priorisierung von W4 auf hoch“.

- [ ] **1. Entwurf vom 12.10.** (Charter, Ist-/Soll-BPMN, Schwachstellen, KPI): alle Dokumente in `docs/` inhaltlich lesen. Geprüft von: ____ am: ____ Geändert: ____
- [ ] **2. Ist-Modell** (19.10.): `bpmn/ist-prozess.bpmn` im Camunda Modeler öffnen; Layout und Inhalt gegen die zehn Schritte der Aufgabenstellung prüfen. Geprüft von: ____ am: ____ Geändert: ____
- [ ] **3. Schwachstellenanalyse:** Priorisierung (Auswirkung/Aufwand) und Top 5 im Team abstimmen. Inhalte aus dem Dokument eines Teammitglieds („Prozess Analyse der Shop Easy GmbH“) wurden in eigener Formulierung übernommen; das Teammitglied bestätigt das. Geprüft von: ____ am: ____ Geändert: ____
- [ ] **4. KPI-Katalog:** Zielwerte (Spalte „Ziel (V)“) sind Vorschläge der KI; Kern-KPIs bestätigen oder anpassen. Geprüft von: ____ am: ____ Geändert: ____
- [ ] **5. Soll-Prozess-Realisierung:** Begründungen und Annahmen (Versandtool und Zahlungsdienstleister haben eine API, das ERP nicht) prüfen, ggf. mit dem Dozenten klären. Geprüft von: ____ am: ____ Geändert: ____
- [ ] **6. Review und Korrekturen** (19.10.): Soll- und Ist-Modell sowie die PNGs im Modeler prüfen; Zielwerte K1b ≤ 1 Tag, K18 und K19 bestätigen. Geprüft von: ____ am: ____ Geändert: ____
- [ ] **7. Camunda-8-Prüfung** (19.10.): zwei Lint-Korrekturen im Ist-Modell im Modeler umsetzen (`t_esc` etwas nach oben schieben; Label „erneut entscheiden“ an der Verbindung `t_esc` → `t_dec` entfernen), speichern und die PNGs neu exportieren. Geprüft von: ____ am: ____ Geändert: ____
- [ ] **8. Quellenbasis:** klären, wer die `DVG_*_Lernbloecke.md` erstellt hat (KI?), und die Folienangaben der Zeile oben stichprobenartig nachsehen. Geprüft von: ____ am: ____ Geändert: ____
