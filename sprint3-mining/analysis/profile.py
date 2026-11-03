"""Read-only profile of the Sprint 3 return event log.

Reproduces the numbers used in the Sprint 3 docs, so the Celonis results
can be cross-checked. Standard library only.

Usage (from the repository root):
    python3 -I sprint3-mining/analysis/profile.py [data_dir]

data_dir defaults to sprint3-mining/ and must contain
retouren_eventlog.csv and retouren_metadaten.csv.
"""
import csv
import statistics as st
import sys
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path

DATA = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent
DAY = 86400

REG = "Retoure angemeldet"
LABEL = "Retourenlabel erstellt und versendet"
ARRIVED = "Retourenpaket im Lager eingetroffen"
CHECKED = "Zustand geprüft"
WMS = "Ergebnis im WMS erfasst"
STOCK = "Ware eingelagert, Bestand im ERP korrigiert"
CREDIT = "Gutschrift angestoßen und an PSP übertragen"
PSP_OK = "Erstattung durch PSP bestätigt"
INFORMED = "Kund:in per E-Mail informiert"
CLOSED = "Retoure abgeschlossen"
ESC = "Grenzfall eskaliert"
DEC = "Grenzfall entschieden"
VOUCHER = "Gutschein im CRM erfasst"
DISPOSAL = "Rückversand, Entsorgung oder Spende veranlasst"

# Activities that signal rework or an error (proxy for K4, see KPI-RESULTS.md).
ERROR_ACTIVITIES = {
    "Falsche Buchung korrigiert", "Zustandserfassung korrigiert",
    "Ablehnung revidiert", "Rückfrage zu fehlenden Angaben",
    "Zahlungsdaten angefragt", "Retourenlabel erneut erstellt",
    "Erstattung fehlgeschlagen", "Mehrfachmeldung erkannt",
}


def median(xs):
    return round(st.median(xs), 2) if xs else None


def pct(a, b):
    return round(100 * a / b, 1) if b else None


def load():
    with open(DATA / "retouren_eventlog.csv", encoding="utf-8", newline="") as f:
        events = list(csv.DictReader(f))
    with open(DATA / "retouren_metadaten.csv", encoding="utf-8", newline="") as f:
        meta = {m["case_id"]: m for m in csv.DictReader(f)}
    cases = defaultdict(list)
    for e in events:
        e["t"] = datetime.fromisoformat(e["timestamp"])
        cases[e["case_id"]].append(e)
    for c in cases.values():
        c.sort(key=lambda e: (e["t"], e["event_id"]))
    return events, meta, cases


def main():
    events, meta, cases = load()
    n = len(cases)

    def first(c, act):
        ts = [e["t"] for e in cases[c] if e["activity"] == act]
        return min(ts) if ts else None

    def gaps(a, b):
        out = []
        for c in cases:
            ta, tb = first(c, a), first(c, b)
            if ta and tb:
                out.append((tb - ta).total_seconds() / DAY)
        return out

    print("== DATA ==")
    print("events", len(events), "| cases", n, "| metadata rows", len(meta))
    print("period", min(e["timestamp"] for e in events), "to", max(e["timestamp"] for e in events))
    print("activities", len({e["activity"] for e in events}))
    print("case ids only in log:", len(set(cases) - set(meta)), "| only in metadata:", len(set(meta) - set(cases)))
    print("empty cells in log:", {k: sum(1 for e in events if not e[k]) for k in ("resource", "detail")})
    print("systems:", dict(Counter(e["system"] for e in events).most_common()))

    print("\n== VARIANTS ==")
    var = Counter(tuple(e["activity"] for e in cases[c]) for c in cases)
    print("variants:", len(var))
    cum = 0
    for i, (v, k) in enumerate(var.most_common(10), 1):
        cum += k
        print(f"#{i}: {k} cases ({pct(k, n)} %, cumulative {pct(cum, n)} %), {len(v)} steps")
        print("    " + " > ".join(v))
    print("variants with 1 case:", sum(1 for k in var.values() if k == 1))
    print("first activity:", dict(Counter(cases[c][0]["activity"] for c in cases)))

    print("\n== K1 lead time (registration to end), closed cases ==")
    closed = [c for c in cases if first(c, CLOSED)]
    k1 = [(first(c, CLOSED) - cases[c][0]["t"]).total_seconds() / DAY for c in closed]
    print("closed", len(closed), "| open", n - len(closed))
    print("median days", median(k1), "| mean", round(st.mean(k1), 2), "| P90", round(sorted(k1)[int(0.9 * len(k1))], 2))
    for ch in ("WEBSHOP", "ZALANDO", "AMAZON", "EMAIL", "OHNE_ANMELDUNG"):
        xs = [(first(c, CLOSED) - cases[c][0]["t"]).total_seconds() / DAY for c in closed if meta[c]["channel"] == ch]
        print(f"  {ch}: n={len(xs)} median={median(xs)}")
    reg_psp = gaps(REG, PSP_OK)
    print("registration to PSP confirmation: n", len(reg_psp), "median", median(reg_psp),
          "| within 8-12 d", pct(sum(8 <= x <= 12 for x in reg_psp), len(reg_psp)), "%",
          "| over 12 d", pct(sum(x > 12 for x in reg_psp), len(reg_psp)), "%")

    print("\n== Step gaps (median days, n) ==")
    for a, b in [(REG, LABEL), (LABEL, ARRIVED), (ARRIVED, WMS), (WMS, STOCK),
                 (STOCK, CREDIT), (CREDIT, PSP_OK), (PSP_OK, INFORMED), (ESC, DEC)]:
        g = gaps(a, b)
        print(f"{a} -> {b}: n={len(g)} median={median(g)} negative={sum(x < 0 for x in g)}")

    print("\n== K3 / K4 ==")
    esc = {e["case_id"] for e in events if e["activity"] == ESC}
    err = {e["case_id"] for e in events if e["activity"] in ERROR_ACTIVITIES}
    print("K3 borderline cases:", len(esc), pct(len(esc), n), "%")
    print("K4 proxy, cases with a correction/error activity:", len(err), pct(len(err), n), "%")
    for a in sorted(ERROR_ACTIVITIES):
        print("   ", a, len({e["case_id"] for e in events if e["activity"] == a}))
    print("cases with a repeated activity:", sum(1 for c in cases if len({e["activity"] for e in cases[c]}) < len(cases[c])))

    print("\n== K6-K8 condition (inspected cases only) ==")
    insp = [m for m in meta.values() if m["observed_condition"]]
    for k, v in Counter(m["observed_condition"] for m in insp).most_common():
        print(f"{k}: {v} ({pct(v, len(insp))} %)")
    print("inspected:", len(insp), "| no condition:", n - len(insp))

    print("\n== K9-K11 and outcomes ==")
    print("disposal step:", dict(Counter(e["detail"] for e in events if e["activity"] == DISPOSAL)))
    print("final_outcome:", dict(Counter(m["final_outcome"] for m in meta.values()).most_common()))

    print("\n== Weak-point checks ==")
    print("customer informed before PSP confirmation:",
          sum(1 for c in cases if first(c, PSP_OK) and first(c, INFORMED) and first(c, INFORMED) < first(c, PSP_OK)))
    print("PSP credit before stock booking:",
          sum(1 for c in cases if first(c, STOCK) and first(c, CREDIT) and first(c, CREDIT) < first(c, STOCK)))
    print("voucher cases with PSP credit as well:",
          sum(1 for c in cases if first(c, VOUCHER) and first(c, CREDIT)), "of", sum(1 for c in cases if first(c, VOUCHER)))
    print("PSP credit by weekday:", dict(Counter(e["t"].strftime("%a") for e in events if e["activity"] == CREDIT)))
    print("warehouse arrival by hour (top 5):", Counter(e["t"].hour for e in events if e["activity"] == ARRIVED).most_common(5))
    print("status requests:", len({e["case_id"] for e in events if e["activity"] == "Statusanfrage Kund:in"}), "cases")
    print("refund failed:", len({e["case_id"] for e in events if e["activity"] == "Erstattung fehlgeschlagen"}), "cases")
    print("label problem:", len({e["case_id"] for e in events if e["activity"] == "Label-Problem gemeldet"}), "cases")
    print("workload by resource:", dict(Counter(e["resource"] for e in events).most_common(12)))


if __name__ == "__main__":
    main()
