# Weak points

W1–W17 are the weak points of Sprint 1 ([../sprint1-bpm/SCHWACHSTELLENANALYSE.md](../sprint1-bpm/SCHWACHSTELLENANALYSE.md)). Numbers come from `analysis/profile.py` and [KPI-RESULTS.md](KPI-RESULTS.md).

## 1. Sprint 1 weak points: confirmed by data?

| W | Weak point | Evidence in the log | Result |
|---|---|---|---|
| W1 | Three channels, no common data | 288 marketplace takeovers and 144 e-mail registrations are typed in by hand; lead time webshop 15.1, e-mail 15.7, Amazon 16.7, Zalando 17.8 days | confirmed |
| W2 | E-mails are informal, data missing | 31 cases with "Rückfrage zu fehlenden Angaben"; 30 duplicate reports (27 of them are e-mail registrations, variant 8) | confirmed |
| W3 | Manual entry in the ERP | "Anmeldung geprüft" is done by a clerk (SB-01 to SB-05); 1,470 events | consistent, no error figure for typing itself |
| W4 | Check by experience, no pre-check of deadlines | 76 rejections at registration (5.1 %) | partly: rejections exist, the cost is small |
| W5 | Label is manual, customer waits | median 1.7 days to the label; 44 label problems with a second label | confirmed |
| W6 | No tracking, news only at arrival | 17 cases where the parcel arrives before any registration; 66 cases never arrive (NOT_RECEIVED) | confirmed |
| W7 | Inspection is manual and slow | 1.8 days from arrival to the WMS result; 14 corrections of the condition | confirmed |
| W8 | Decision not documented | no decision event for standard cases; only 143 borderline decisions are visible | confirmed (data gap) |
| W9 | Borderline cases only by e-mail | 147 cases (9.8 %); 2.2 days from escalation to decision; 31 reminders to the specialist | confirmed |
| W10 | Abuse not detected | 6.9 % worn, 13.9 % minor wear, 193 rejections after inspection; no check step in the log | open: rate is visible, no detection step exists |
| W11 | Manual stock booking, deviations | 27 "Falsche Buchung korrigiert" | confirmed |
| W12 | Manual refund is the main time driver | 3.9 days WMS to PSP confirmation; 43 % of refunds are over 12 days | confirmed |
| W13 | Customer informed late, asks often | 152 status requests (10 %); median 2.4 days from refund to the e-mail | confirmed |
| W14 | Reporting is manual and late | not in the log (separate Excel process) | not measurable here |
| W15 | Peaks are hard to plan | arrivals: Monday 341, Saturday 233, Tuesday only 86 | confirmed, see 2.5 |
| W16 | Standard cases are handled by humans | the standard path (33 % of cases) passes through Sachbearbeitung, Lager and Buchhaltung; no decision rule or automation step appears in the log | confirmed |
| W17 | Return reason is free text | 227 cases (15.1 %) have the reason OTHER | confirmed |

## 2. New weak points (not known in Sprint 1)

1. **PSP credit runs in batches.** 1,105 of 1,204 credit events (92 %) happen on Tuesday or Thursday (711 and 394). A refund that is ready on Wednesday waits for Thursday. This is part of the 2.9 days from the WMS result to the credit.
2. **Failed refunds.** 64 cases (78 events) with "Erstattung fehlgeschlagen". Causes: PSP_TIMEOUT 34, SCHNITTSTELLENFEHLER 14, IBAN_FEHLT 12, KARTE_ABGELAUFEN 10. The first two are interface problems, the last two are missing or wrong customer data. 5 cases stay open with `REFUND_FAILED_OPEN`.
3. **Cases are closed late.** The median time from PSP confirmation to "Retoure abgeschlossen" is 4.7 days (n = 976). The close step is an Excel entry, and 153 cases are never closed, including 24 that stop after the label.
4. **Customer informed before the refund.** In 45 cases the e-mail is sent before the PSP confirmation. If the refund then fails, the customer has the wrong information.
5. **Return peaks on Monday and Saturday.** See W15. The warehouse arrival times between 04:00 and 07:59 (760 events) look like a batch import. If so, the Monday and Saturday peaks may also come from the import and not from real arrivals. This needs to be checked with the warehouse.
6. **Vouchers are booked twice.** All 97 voucher cases have a credit event with `methode=GUTSCHEIN` in the PSP interface and a manual entry in the CRM. The lead time for a voucher is 11.8 days (registration to the voucher).
7. **Status requests cost time.** 152 requests by e-mail (63), phone (54) and ticket (35). Each has a research effort `recherche_min`. The customer calls because there is no tracking (W6, W13).
8. **Credit before stock booking.** In 9 cases the refund is started before the stock booking. This breaks the intended order of the process (control weakness).
9. **Duplicates and cases without registration.** 30 duplicate reports and 17 cases registered after arrival suggest that the entry point has no automatic check.
10. **Long tail of variants.** 157 of 254 variants occur once. This means that the process has no stable rules for exceptions.

## 3. Discussion

The Sprint 1 weak points are mostly confirmed, but the data changes the priority. W12 (refund) stays the main driver, yet the main reasons are now clear: the batch rhythm of the PSP (Tuesday/Thursday), failed refunds and late closing. These can be fixed by automation (Sprint 4 workflow, Sprint 5 RPA). The customer's shipping time (3.0 days) cannot be changed by ShopEasy.

Weak points that the data cannot prove: W14 (reporting), W3 (typing errors) and W10 (abuse detection). For W8 the missing decision event is itself the result.

## 4. Possible links to the next sprints

| Weak point | Idea |
|---|---|
| PSP batch, failed refunds, double voucher entry | automatic refund orchestration with retry (Sprint 4) |
| Status requests, late information | status messages from the process engine, using the Sprint 2 events |
| Duplicates, manual takeover, e-mail entry | RPA or AI extraction for the entry points (Sprint 5) |
| Borderline cases, abuse | decision rules (DMN) and an AI recommendation (Sprint 4/5) |
