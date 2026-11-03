# KPI results

KPI definitions: [../sprint1-bpm/KPI-KATALOG.md](../sprint1-bpm/KPI-KATALOG.md) (K1–K13). Values are computed with `analysis/profile.py` (script column). The column "Celonis" is for the value from the Celonis analysis and must be filled in by the person who runs the import.

Rules (from the KPI catalogue): time KPIs only for closed cases, median; open cases counted separately. Period: 01.05.–30.10.2026. Closed cases: 1,347 of 1,500; open: 153.

| KPI | Script value | Celonis | Calculation and filter | Target |
|---|---|---|---|---|
| K1 Lead time | **median 15.6 days** (mean 16.4, P90 24.1) | | "Retoure angemeldet" to "Retoure abgeschlossen", 1,347 closed cases. By channel: webshop 15.1, e-mail 15.7, Amazon 16.7, Zalando 17.8 days | reduce (task states 8–12 days to the refund) |
| K2 Processing time after arrival | **1.8 days** (n = 1,328) | | "Paket im Lager eingetroffen" to "Ergebnis im WMS erfasst". The log has no separate decision event, so the WMS entry is the proxy. Borderline cases: median 2.2 days from escalation to decision | reduce |
| K3 Borderline rate | **9.8 %** (147 of 1,500) | | Cases with "Grenzfall eskaliert" | reduce |
| K4 Error rate | **14.3 %** (214 of 1,500), proxy | | Cases with at least one of: Falsche Buchung korrigiert, Zustandserfassung korrigiert, Ablehnung revidiert, Rückfrage zu fehlenden Angaben, Zahlungsdaten angefragt, Retourenlabel erneut erstellt, Erstattung fehlgeschlagen, Mehrfachmeldung erkannt. There is no error flag in the log. The proxy goes beyond the KPI-KATALOG definition (it adds PSP errors, label errors and duplicates) | reduce |
| K5 Return rate | not computable | | The data has only returns, no orders without a return. The task value of 25–35 % stays the reference | observe |
| K6 As-new rate | **67.6 %** (898 of 1,328) | | `observed_condition = AS_NEW` among inspected cases (172 cases have no condition) | observe |
| K7 Damaged rate | **6.0 %** (80 of 1,328) | | `observed_condition = DAMAGED`. Missing tags 4.4 %, missing parts 1.1 % are separate values | observe |
| K8 Worn rate | **6.9 %** (91 of 1,328) | | `observed_condition = VISIBLY_WORN`. Minor wear is another 13.9 % | observe |
| K9 Return-to-customer rate | **10.1 %** (136 of 1,347) | | `art=RUECKVERSAND` in "Rückversand, Entsorgung oder Spende veranlasst", share of closed cases | observe |
| K10 Disposal rate | **1.5 %** (20 of 1,347) | | `art=ENTSORGUNG` | observe |
| K11 Donation rate | **3.3 %** (45 of 1,347) | | `art=SPENDE` | observe |
| K12 Time to refund | **3.9 days** (n = 1,029) | | "Ergebnis im WMS erfasst" to "Erstattung durch PSP bestätigt", refunds via PSP only. Parts: WMS to credit 2.9 days, credit to confirmation 0.9 days. For vouchers the registration to voucher takes 11.8 days (n = 97) | reduce |
| K13 Time to label | **1.7 days** (n = 1,377) | | "Retoure angemeldet" to "Retourenlabel erstellt und versendet" | reduce |

## Reading the values

- The lead time is 15.6 days, but the task says that customers wait 8–12 days for the refund. These are two different measures. The registration to the PSP confirmation is a median of 11.3 days: 45.8 % of the refunds are inside 8–12 days, 43.0 % are longer. So the task value is correct for the refund, and the time until the case is closed is another 4.7 days later (median from PSP confirmation to "abgeschlossen").
- Where the time goes (median days, closed path): registration to label 1.7; label to arrival 3.0; arrival to WMS result 1.8; WMS result to stock 0.7; stock to PSP credit 1.2; credit to confirmation 0.9. The largest single block is the customer's shipping time (3.0 days), then the internal steps after the inspection.
- Amazon and Zalando returns take 1.7 and 2.8 days longer than webshop returns (median). A possible reason is the manual takeover step "Marktplatz-Retoure manuell übernommen". This is not tested yet.

## Open points

1. Definition of the start (registration or arrival, calendar or working days) is still a question for the lecturer. The script uses registration and calendar days.
2. K4 definition: confirm with the team that the proxy is acceptable.
3. Fill in the Celonis column and compare. A difference above 1 % needs an explanation.
