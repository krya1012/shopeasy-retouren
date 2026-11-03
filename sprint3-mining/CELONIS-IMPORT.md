# Celonis import and data integration

The import is done by a person in the Celonis web UI (academic account). Claude cannot log in. The menu names below can differ a little between Celonis versions.

## 1. Import steps

1. **Data Pool.** Data Integration → Data Pools → create a pool `shopeasy-retouren`.
2. **Upload the tables.** Add a data connection "Upload files". Upload the two CSV files as the tables `retouren_eventlog` and `retouren_metadaten`.
   - Separator `,`, encoding UTF-8, header row on.
   - Column types: `timestamp` as DateTime with format `YYYY-MM-DD HH24:MI:SS`; `order_date` as Date; `return_value_eur` as Float; `days_since_order` and `items_count` as Integer; all other columns as Text.
3. **Data Model.** Create a data model in the pool.
   - Add `retouren_eventlog` as the **activity table**: case ID `case_id`, activity `activity`, timestamp `timestamp`. Sorting column: `event_id`.
   - Add `retouren_metadaten` as the **case table** with key `case_id`.
   - Foreign key: `retouren_eventlog.case_id` to `retouren_metadaten.case_id` (many events to one case).
4. **Load** the data model. Check: 16,189 events and 1,500 cases.
5. **Analysis.** Create a Studio Analysis on the data model. Use "Process Explorer" for the variants, a table for the KPIs, and charts for the splits (see [KPI-RESULTS.md](KPI-RESULTS.md)).
6. **Screenshots.** Save to `screenshots/`: the data model, the variant explorer, the process explorer, one KPI table.

## 2. Check after the load

| Check | Expected value (from `analysis/profile.py`) |
|---|---|
| Events | 16,189 |
| Cases | 1,500 |
| Variants | 254 |
| Median throughput time, closed cases | 15.62 days |
| Cases without "Retoure abgeschlossen" | 153 |

## 3. What is integration here?

**Integration** means to bring data and systems that were built separately to work together. Our returns process runs over many systems, and no one of them shows the whole case. Process mining only works after this data is brought together in one log.

**Data integration tasks** in this sprint (and where they appear in the files):

| Task | Why it is needed | In our data |
|---|---|---|
| Extract from the source systems | Each system keeps only its own part of the case | 10 systems: WEBSHOP, MARKTPLATZ_PORTAL, EMAIL, TELEFON, ERP, WMS, VERSANDTOOL, PSP, CRM, EXCEL |
| Create one case ID | All events must point to the same case | `case_id` `RET-2026-…`. Orders and customers have their own IDs in the metadata (`order_id`, `customer_id`) |
| Common event structure | Case, activity, timestamp are the minimum for process mining | The columns `case_id`, `activity`, `timestamp`; the rest are attributes |
| Unify activity names | Two names for one step split a variant in two | Already unified here. In a real project a mapping table per system is needed |
| Unify time format and time zone | A wrong order of events gives wrong variants and times | One format, no zone given; we assume one local zone |
| Parse semi-structured fields | KPIs need columns, not text | Split `detail` (`betrag=71.90 EUR; methode=GUTSCHEIN`) into attributes |
| Join case attributes | Filters per channel, product or outcome | Join `retouren_metadaten` on `case_id` |
| Handle missing and wrong data | Empty values and wrong timestamps change the KPIs | 78 empty `resource`, 172 empty `observed_condition`, arrival timestamps in a 04:00–07:59 batch window |
| Load repeatedly | A real log grows every day | Here: one export. Later a scheduled extraction (delta load) |

**Link to Sprint 2.** The integration styles of Sprint 2 (REST, gRPC, messaging) are the way such events can be delivered continuously. The status events on RabbitMQ (`return.status.<STATUS>`) could feed a live event log, so the analysis would not depend on one-time CSV exports.

## 4. Limits

- The log is an export. It contains only what the systems wrote down. Decisions in e-mails and phone calls are visible only as the events `Grenzfall eskaliert`, `Grenzfall entschieden` and `Statusanfrage`.
- The warehouse arrival time can be a batch import time (see [DATA-ANALYSIS.md](DATA-ANALYSIS.md)).
