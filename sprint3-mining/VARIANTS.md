# Process variants

A variant is one exact sequence of activities. Numbers from `analysis/profile.py`; the Celonis variant explorer must show the same.

## 1. How many?

**254 variants** in 1,500 cases.

- The top variant has 495 cases (33.0 %). The top 5 cover 50.4 %, the top 10 cover 60.1 %.
- 157 variants occur exactly once. The process has a long tail of single cases.

## 2. Top 10

| # | Cases | Share | Cumulative | Description |
|---|---|---|---|---|
| 1 | 495 | 33.0 % | 33.0 % | Happy path: register, check, label, arrival, inspection, WMS, stock, PSP credit, PSP confirmation, e-mail, close (11 steps) |
| 2 | 111 | 7.4 % | 40.4 % | As 1, with "Marktplatz-Retoure manuell übernommen" (manual takeover from the Zalando/Amazon portal) |
| 3 | 68 | 4.5 % | 44.9 % | Inspection result, then "Rückversand, Entsorgung oder Spende", no refund |
| 4 | 41 | 2.7 % | 47.7 % | Refund as voucher: credit step plus "Gutschein im CRM erfasst" |
| 5 | 41 | 2.7 % | 50.4 % | Rejected at registration: register, check, "Ablehnung mitgeteilt", close (4 steps) |
| 6 | 40 | 2.7 % | 53.1 % | As 1, with "E-Mail-Anmeldung manuell erfasst" |
| 7 | 28 | 1.9 % | 54.9 % | As 1, with "Grenzfall eskaliert" and "Grenzfall entschieden" |
| 8 | 27 | 1.8 % | 56.7 % | E-mail registration, "Mehrfachmeldung erkannt" (duplicate), closed |
| 9 | 26 | 1.7 % | 58.5 % | Borderline case with return / disposal / donation |
| 10 | 24 | 1.6 % | 60.1 % | Stops after "Retourenlabel erstellt und versendet" (open or never received) |

## 3. Differences to our Ist-Prozess (`sprint1-bpm/ist-prozess.bpmn`)

**What matches.** Variant 1 follows the Ist-Prozess: register, check in the ERP, create and send the label, arrival, inspection, WMS, stock, refund, e-mail, close. The three entry channels (marketplace, webshop, e-mail), the rejection at registration, the borderline case and the voucher also exist.

**What the Ist-Prozess does not show.**

| Observed in the log | Cases | Ist-Prozess |
|---|---|---|
| Statusanfrage Kund:in + answered | 152 | not modelled |
| Erstattung fehlgeschlagen (PSP_TIMEOUT 34, SCHNITTSTELLENFEHLER 14, IBAN_FEHLT 12, KARTE_ABGELAUFEN 10) | 64 (78 events) | not modelled, only success |
| Label-Problem gemeldet + new label | 44 | not modelled |
| Mehrfachmeldung erkannt (duplicate) | 30 | not modelled |
| Rückfrage zu fehlenden Angaben + Angaben nachgereicht | 31 | only for e-mail |
| Zahlungsdaten angefragt + nachgereicht | 23 | not modelled |
| Reklamation eingegangen / bearbeitet; zur Ablehnung | 22 + 17 events | not modelled |
| Ablehnung revidiert / bestätigt | 12 / 5 | only as a hidden error source |
| Falsche Buchung korrigiert | 27 | not modelled |
| Zustandserfassung korrigiert | 14 | not modelled |
| Erinnerung an Fachkraft versendet (reminder for the borderline decision) | 31 | not modelled |
| Retoure nachträglich erfasst: parcel arrives before the registration | 17 | not modelled |
| Customer informed before the refund is confirmed | 45 | Ist-Prozess: information after the refund, "delayed" |
| Not closed in the log | 153 | every case ends |

**Other differences.**
- The Ist-Prozess ends the voucher path in the CRM. In the log every voucher case also has a credit event with `methode=GUTSCHEIN` (97 of 97), so the voucher is entered in two systems.
- The Ist-Prozess has a manual rejection path after the inspection ("Rückversand / Entsorgung / Spende"). The log shows it as 201 cases and it ends without a refund, as expected. The reason for the rejection (`ergebnis`) is in `detail`.
- The Excel step: every case is closed in EXCEL (1,350 close events). The monthly Excel report from the Ist-Prozess is not in the log, because it is a separate process.

## 4. Conclusion

The Ist-Prozess describes the standard path well (variant 1 to 7 = 55 % of the cases). It misses the exceptions, which are the cause of the long tail: failed refunds, status requests, label problems, duplicates and corrections. These loops are the main reason for the long lead time (see [WEAKNESSES.md](WEAKNESSES.md)).
