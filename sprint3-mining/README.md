# Sprint 3: Process mining (Celonis)

Period 03.11.–17.11.2026. Hand-in in ILIAS by 11:30. Task: `Aufgabe DvG WS26_Sprint 3.pdf` (slide 14). All documentation is in English.

## Task and where the answer is

| Task item | Document |
|---|---|
| Analyse the given event and metadata | [DATA-ANALYSIS.md](DATA-ANALYSIS.md) |
| Import into Celonis; explain the integration / data integration tasks | [CELONIS-IMPORT.md](CELONIS-IMPORT.md) |
| Real process variants: how many, differences to our Ist-Prozess | [VARIANTS.md](VARIANTS.md) |
| Weak points: compute the Sprint 1 KPIs, find unknown weak points | [KPI-RESULTS.md](KPI-RESULTS.md), [WEAKNESSES.md](WEAKNESSES.md) |
| AI use log | [AI-USAGE.md](AI-USAGE.md) |

## Input data

| File | Content |
|---|---|
| `retouren_eventlog.csv` | 16,189 events, 1,500 cases, 38 activities, 01.05.–30.10.2026 |
| `retouren_metadaten.csv` | one row per case (1,500 rows), 19 case attributes |

## Headline results

- **254 variants** in 1,500 cases. The happy path covers 33 % of the cases; 157 variants occur exactly once.
- **Median lead time 15.6 days** (registration to closing), P90 24.1 days. The task states 8–12 days for the refund.
- Time to the PSP refund confirmation: median 11.3 days; 43 % of the refunds take longer than 12 days.
- 153 cases (10 %) are not closed in the log.
- 14 % of the cases contain a correction or error step; 9.8 % are borderline cases.
- New weak points that Sprint 1 did not know: the PSP run only on Tuesday and Thursday, failed refunds, status requests, customers informed before the refund, cases registered only after the parcel arrived.

## Cross-check script

`analysis/profile.py` computes every number in these documents from the two CSV files, with the Python standard library only. Run it from the repository root:

```
python3 -I sprint3-mining/analysis/profile.py
```

The numbers in the Celonis analysis must match the script. A difference needs an explanation (filter, definition).

## Open points for the team

1. Celonis steps (login, upload, data model, screenshots) must be done by a person. See [CELONIS-IMPORT.md](CELONIS-IMPORT.md). Screenshots go to `screenshots/`.
2. Fill in the Celonis columns in [KPI-RESULTS.md](KPI-RESULTS.md) after the import.
3. Decide whether the lecturer's PDF and the two CSV files are committed to the public repository. They are untracked now.
