# Data analysis

Source: `retouren_eventlog.csv` and `retouren_metadaten.csv`. Numbers come from `analysis/profile.py`.

## 1. Event log

One row is one event. Columns:

| Column | Meaning | Note |
|---|---|---|
| `case_id` | return case, for example `RET-2026-10001` | 1,500 different values |
| `event_id` | event ID with a system prefix (`WEB-`, `ERP-`, `VSD-`, `WMS-` ...) | unique, no duplicates |
| `activity` | step name, in German | 38 different names |
| `timestamp` | `YYYY-MM-DD HH:MM:SS`, no time zone | 01.05.2026 08:15 to 30.10.2026 12:01 |
| `resource` | person or system (`SB-01`, `LAG-04`, `BH-02`, `FK-01`, `KUNDE`, `SYSTEM-PSP`) | 78 empty |
| `role` | Kunde, Sachbearbeitung, Lager, Buchhaltung, Fachkraft, System | complete |
| `system` | source system | 10 systems |
| `detail` | free text of key=value pairs, separated by `;` | 3,063 empty |

Events per source system: WMS 4,207, ERP 3,854, EMAIL 2,420, VERSANDTOOL 1,421, EXCEL 1,367, PSP 1,107, WEBSHOP 1,051, MARKTPLATZ_PORTAL 576, CRM 132, TELEFON 54.

Keys inside `detail`: `zustand`, `ergebnis`, `betrag`, `methode`, `grund`, `frist_ok`, `bestellung_gefunden`, `carrier`, `versuch`, `art`, `entscheidung`, `kanal`, `recherche_min`, `fehler`, `gutschein_wert`, `erinnerung`, `original`, `von`, `nach`.

## 2. Metadata

One row per case, 19 columns: `case_id`, `order_id`, `customer_id`, `customer_type`, `country`, `channel`, `order_marketplace`, `order_date`, `days_since_order`, `product_category`, `items_count`, `return_value_eur`, `payment_method`, `carrier`, `return_reason`, `reason_text`, `observed_condition`, `final_outcome`, `refund_method`.

| Attribute | Values (cases) |
|---|---|
| `channel` | WEBSHOP 1,051, ZALANDO 168, EMAIL 144, AMAZON 120, OHNE_ANMELDUNG 17 |
| `customer_type` | BESTANDSKUNDE 1,199, NEUKUNDE 301 |
| `country` | DE 1,281, AT 104, CH 61, NL 54 |
| `final_outcome` | REFUNDED 1,027, REJECTED_INSPECTION 193, VOUCHER 97, REJECTED_REGISTRATION 76, NOT_RECEIVED 66, DUPLICATE_CLOSED 30, REFUND_FAILED_OPEN 5, OPEN_NO_DECISION 4, OPEN_AT_EXPORT 2 |
| `refund_method` | PSP 1,034, NONE 369, VOUCHER 97 |
| `observed_condition` | AS_NEW 898, MINOR_WEAR 185, VISIBLY_WORN 91, DAMAGED 80, MISSING_TAGS 59, MISSING_PARTS 15, empty 172 |

The metadata gives the case attributes that the log does not have: channel, customer, product, value, reason and final outcome. It is the base for filters and splits in Celonis (KPI per channel, per category).

## 3. Data quality

| Check | Result |
|---|---|
| `case_id` in log and metadata | identical sets, 1,500 each, no orphan |
| Duplicate `event_id` | none |
| Order of events inside a case | file order equals time order in all cases |
| First event of a case | "Retoure angemeldet" in 1,483 cases; "Retourenpaket im Lager eingetroffen" in 17 cases (registered afterwards in Excel) |
| Cases closed | 1,347 have "Retoure abgeschlossen"; 153 do not |
| Empty `resource` | 78 events |
| Empty `carrier` | 123 cases (no label sent) |
| Empty `observed_condition` | 172 cases (parcel never inspected) |
| Time zone | none stated; assumed local time |
| Warehouse arrival times | 04:00–07:59 for 760 of the arrival events, so likely a batch import and not the real arrival time |
| Negative step gaps | 9 cases: PSP credit before the stock booking; 45 cases: customer informed before the PSP confirmation |

`final_outcome` is a label from the metadata. It is not an event, so it can be used to check the log (for example `NOT_RECEIVED` = no arrival event) but not as a process step.

## 4. Consequences for the analysis

- The activity names are already unified, so no mapping table is needed.
- `detail` must be split into columns before a KPI like K4 can use `fehler`, `versuch` or `entscheidung`.
- Closed and open cases must be separated. Lead-time KPIs use closed cases only.
- The 17 cases without a registration start in the middle of the process. They distort the lead time (registration to end) and must be reported separately.
