#!/usr/bin/env python3
"""Erzeugt BPMN 2.0 XML (inkl. Diagramm-Layout) fuer Ist- und Soll-Prozess.

Aufruf:  python3 tools/gen_bpmn.py bpmn
Das Skript prueft zusaetzlich das Layout (Ueberlappungen, Pfeile durch Elemente).
"""
import sys
from xml.sax.saxutils import escape, quoteattr

TASKS = {"task", "userTask", "serviceTask", "businessRuleTask", "sendTask", "receiveTask", "manualTask", "scriptTask"}
GATES = {"exclusiveGateway", "parallelGateway", "eventBasedGateway"}


def size(n):
    t = n["type"]
    if t in TASKS:
        return 110, 80
    if t in GATES:
        return 50, 50
    return 36, 36


class Model:
    def __init__(self, pid, pool_name, lanes, width):
        self.pid, self.pool_name, self.lanes, self.width = pid, pool_name, lanes, width
        self.nodes, self.flows, self.annots, self.assocs, self.stores = {}, [], [], [], []

    def node(self, id, type, name, cx, cy, lane, doc=None, ev=None, attached=None, cancel=True):
        self.nodes[id] = dict(id=id, type=type, name=name, cx=cx, cy=cy, lane=lane, doc=doc, ev=ev,
                              attached=attached, cancel=cancel)

    def store(self, id, name, cx, cy, task, dir, side, label="b"):
        """Datenspeicher (System) an einer Aufgabe. dir: 'in' = liest, 'out' = schreibt.
        side: Seite der Aufgabe, an der der Speicher haengt (t/b/r). label: t/b/r."""
        self.stores.append(dict(id=id, name=name, cx=cx, cy=cy, task=task, dir=dir, side=side, label=label))

    def store_edge(self, st):
        tp = self.anchor(st["task"], st["side"])
        opp = {"t": (st["cx"], st["cy"] + 25), "b": (st["cx"], st["cy"] - 25), "r": (st["cx"] - 25, st["cy"])}[st["side"]]
        if st["side"] in ("t", "b"):
            assert abs(tp[0] - opp[0]) < 0.5, f"Speicher {st['id']} nicht unter/ueber Aufgabe"
        else:
            assert abs(tp[1] - opp[1]) < 0.5, f"Speicher {st['id']} nicht neben Aufgabe"
        pts = [tp, opp]
        return pts if st["dir"] == "out" else pts[::-1]

    def store_label(self, st):
        cx, cy = st["cx"], st["cy"]
        if st["label"] == "b":
            return cx - 50, cy + 26, 100, 27
        if st["label"] == "t":
            return cx - 50, cy - 53, 100, 27
        return cx + 28, cy - 14, 90, 27

    def anchor(self, nid, side, off=0):
        n = self.nodes[nid]
        w, h = size(n)
        x, y = n["cx"], n["cy"]
        return {"l": (x - w / 2, y + off), "r": (x + w / 2, y + off),
                "t": (x + off, y - h / 2), "b": (x + off, y + h / 2)}[side]

    def flow(self, id, src, tgt, name=None, route=None, cond=None):
        self.flows.append(dict(id=id, src=src, tgt=tgt, name=name, route=route or ("hvh", None), cond=cond))

    def points(self, f):
        s, t = f["src"], f["tgt"]
        kind = f["route"][0]
        if kind == "hvh":
            sx, sy = self.anchor(s, "r")
            tx, ty = self.anchor(t, "l")
            if abs(sy - ty) < 0.5:
                return [(sx, sy), (tx, ty)]
            mx = f["route"][1] if f["route"][1] is not None else (sx + tx) / 2
            return [(sx, sy), (mx, sy), (mx, ty), (tx, ty)]
        if kind == "vh":  # von src oben/unten senkrecht, dann waagrecht in tgt links
            sx, sy = self.anchor(s, f["route"][1])
            tx, ty = self.anchor(t, "l")
            return [(sx, sy), (sx, ty), (tx, ty)]
        if kind == "hv":  # von src rechts waagrecht, dann senkrecht in tgt oben/unten
            sx, sy = self.anchor(s, "r")
            tx, ty = self.anchor(t, f["route"][1])
            return [(sx, sy), (tx, sy), (tx, ty)]
        if kind == "via":  # (src side, tgt side, [mittelpunkte], src offset)
            ss, ts, mids = f["route"][1], f["route"][2], f["route"][3]
            off = f["route"][4] if len(f["route"]) > 4 else 0
            return [self.anchor(s, ss, off)] + list(mids) + [self.anchor(t, ts)]
        raise ValueError(kind)

    # ---------- XML ----------
    def lane_bounds(self):
        out, y = {}, 0
        for lid, lname, h in self.lanes:
            out[lid] = (y, h)
            y += h
        return out, y

    def xml(self):
        lb, total_h = self.lane_bounds()
        L = []
        a = L.append
        a('<?xml version="1.0" encoding="UTF-8"?>')
        a('<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" '
          'xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" '
          'xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" '
          f'id="Definitions_{self.pid}" targetNamespace="http://bpmn.io/schema/bpmn">')
        a(f'  <bpmn:collaboration id="Collab_{self.pid}">')
        a(f'    <bpmn:participant id="Participant_{self.pid}" name={quoteattr(self.pool_name)} processRef="Process_{self.pid}" />')
        a('  </bpmn:collaboration>')
        a(f'  <bpmn:process id="Process_{self.pid}" isExecutable="false">')
        a('    <bpmn:laneSet id="LaneSet_1">')
        for lid, lname, h in self.lanes:
            a(f'      <bpmn:lane id="{lid}" name={quoteattr(lname)}>')
            for n in self.nodes.values():
                if n["lane"] == lid:
                    a(f'        <bpmn:flowNodeRef>{n["id"]}</bpmn:flowNodeRef>')
            a('      </bpmn:lane>')
        a('    </bpmn:laneSet>')
        inc, out = {}, {}
        for f in self.flows:
            out.setdefault(f["src"], []).append(f["id"])
            inc.setdefault(f["tgt"], []).append(f["id"])
        for n in self.nodes.values():
            t = n["type"]
            attrs = f'id="{n["id"]}"'
            if n["name"]:
                attrs += f' name={quoteattr(n["name"]).replace(chr(10), "&#10;")}'
            if t == "boundaryEvent":
                attrs += f' attachedToRef="{n["attached"]}"'
                if not n["cancel"]:
                    attrs += ' cancelActivity="false"'
            ev = n["ev"]
            evx = ""
            if ev == "message":
                evx = '<bpmn:messageEventDefinition />'
            elif ev == "error":
                evx = '<bpmn:errorEventDefinition />'
            elif ev and ev.startswith("timer:"):
                evx = (f'<bpmn:timerEventDefinition><bpmn:timeDuration xsi:type="bpmn:tFormalExpression">'
                       f'{ev[6:]}</bpmn:timeDuration></bpmn:timerEventDefinition>')
            a(f'    <bpmn:{t} {attrs}>')
            if n["doc"]:
                a(f'      <bpmn:documentation>{escape(n["doc"])}</bpmn:documentation>')
            for fid in inc.get(n["id"], []):
                a(f'      <bpmn:incoming>{fid}</bpmn:incoming>')
            for fid in out.get(n["id"], []):
                a(f'      <bpmn:outgoing>{fid}</bpmn:outgoing>')
            for st in self.stores:
                if st["task"] == n["id"]:
                    tag, ref = (("dataInputAssociation", "sourceRef") if st["dir"] == "in"
                                else ("dataOutputAssociation", "targetRef"))
                    a(f'      <bpmn:{tag} id="Assoc_{st["id"]}"><bpmn:{ref}>{st["id"]}</bpmn:{ref}></bpmn:{tag}>')
            if evx:
                a(f'      {evx}')
            a(f'    </bpmn:{t}>')
        for st in self.stores:
            a(f'    <bpmn:dataStoreReference id="{st["id"]}" name={quoteattr(st["name"])} />')
        for f in self.flows:
            nm = f' name={quoteattr(f["name"])}' if f["name"] else ""
            if f["cond"]:
                a(f'    <bpmn:sequenceFlow id="{f["id"]}"{nm} sourceRef="{f["src"]}" targetRef="{f["tgt"]}">')
                a(f'      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">{escape(f["cond"])}</bpmn:conditionExpression>')
                a('    </bpmn:sequenceFlow>')
            else:
                a(f'    <bpmn:sequenceFlow id="{f["id"]}"{nm} sourceRef="{f["src"]}" targetRef="{f["tgt"]}" />')
        for an in self.annots:
            a(f'    <bpmn:textAnnotation id="{an["id"]}"><bpmn:text>{escape(an["text"])}</bpmn:text></bpmn:textAnnotation>')
        for i, (src, tgt) in enumerate(self.assocs):
            a(f'    <bpmn:association id="Assoc_{i + 1}" sourceRef="{src}" targetRef="{tgt}" />')
        a('  </bpmn:process>')
        # Diagramm
        a(f'  <bpmndi:BPMNDiagram id="Diagram_{self.pid}">')
        a(f'    <bpmndi:BPMNPlane id="Plane_{self.pid}" bpmnElement="Collab_{self.pid}">')
        a(f'      <bpmndi:BPMNShape id="Participant_{self.pid}_di" bpmnElement="Participant_{self.pid}" isHorizontal="true">')
        a(f'        <dc:Bounds x="0" y="0" width="{self.width}" height="{total_h}" />')
        a('      </bpmndi:BPMNShape>')
        for lid, lname, h in self.lanes:
            y, hh = lb[lid]
            a(f'      <bpmndi:BPMNShape id="{lid}_di" bpmnElement="{lid}" isHorizontal="true">')
            a(f'        <dc:Bounds x="30" y="{y}" width="{self.width - 30}" height="{hh}" />')
            a('      </bpmndi:BPMNShape>')
        for n in self.nodes.values():
            w, h = size(n)
            extra = ' isMarkerVisible="true"' if n["type"] == "exclusiveGateway" else ""
            a(f'      <bpmndi:BPMNShape id="{n["id"]}_di" bpmnElement="{n["id"]}"{extra}>')
            a(f'        <dc:Bounds x="{n["cx"] - w / 2:g}" y="{n["cy"] - h / 2:g}" width="{w}" height="{h}" />')
            a('      </bpmndi:BPMNShape>')
        for st in self.stores:
            a(f'      <bpmndi:BPMNShape id="{st["id"]}_di" bpmnElement="{st["id"]}">')
            a(f'        <dc:Bounds x="{st["cx"] - 25:g}" y="{st["cy"] - 25:g}" width="50" height="50" />')
            lx, ly, lw, lh = self.store_label(st)
            a(f'        <bpmndi:BPMNLabel><dc:Bounds x="{lx:g}" y="{ly:g}" width="{lw}" height="{lh}" /></bpmndi:BPMNLabel>')
            a('      </bpmndi:BPMNShape>')
        for an in self.annots:
            a(f'      <bpmndi:BPMNShape id="{an["id"]}_di" bpmnElement="{an["id"]}">')
            a(f'        <dc:Bounds x="{an["x"]}" y="{an["y"]}" width="{an["w"]}" height="{an["h"]}" />')
            a('      </bpmndi:BPMNShape>')
        for f in self.flows:
            a(f'      <bpmndi:BPMNEdge id="{f["id"]}_di" bpmnElement="{f["id"]}">')
            for x, y in self.points(f):
                a(f'        <di:waypoint x="{x:g}" y="{y:g}" />')
            a('      </bpmndi:BPMNEdge>')
        for st in self.stores:
            a(f'      <bpmndi:BPMNEdge id="Assoc_{st["id"]}_di" bpmnElement="Assoc_{st["id"]}">')
            for x, y in self.store_edge(st):
                a(f'        <di:waypoint x="{x:g}" y="{y:g}" />')
            a('      </bpmndi:BPMNEdge>')
        for i, (src, tgt) in enumerate(self.assocs):
            an = next(x for x in self.annots if x["id"] == src)
            p1 = (an["x"] + an["w"] / 2, an["y"] + an["h"])
            p2 = self.anchor(tgt, "t")
            a(f'      <bpmndi:BPMNEdge id="Assoc_{i + 1}_di" bpmnElement="Assoc_{i + 1}">')
            a(f'        <di:waypoint x="{p1[0]:g}" y="{p1[1]:g}" />')
            a(f'        <di:waypoint x="{p2[0]:g}" y="{p2[1]:g}" />')
            a('      </bpmndi:BPMNEdge>')
        a('    </bpmndi:BPMNPlane>')
        a('  </bpmndi:BPMNDiagram>')
        a('</bpmn:definitions>')
        return "\n".join(L) + "\n"

    # ---------- Pruefungen ----------
    def validate(self):
        ids = set(self.nodes)
        for f in self.flows:
            assert f["src"] in ids and f["tgt"] in ids, f
        lanes = {l[0] for l in self.lanes}
        for n in self.nodes.values():
            assert n["lane"] in lanes, n["id"]
        for st in self.stores:
            assert st["task"] in ids, st
        inc = {f["tgt"] for f in self.flows}
        out = {f["src"] for f in self.flows}
        for n in self.nodes.values():
            if n["type"] not in ("startEvent", "boundaryEvent"):
                assert n["id"] in inc, f"kein Eingang: {n['id']}"
            if n["type"] != "endEvent":
                assert n["id"] in out, f"kein Ausgang: {n['id']}"

    def _box(self, n):
        w, h = size(n)
        return (n["cx"] - w / 2, n["cy"] - h / 2, n["cx"] + w / 2, n["cy"] + h / 2)

    def layout_check(self):
        """Meldet Ueberlappungen und Pfeile, die durch fremde Elemente laufen."""
        problems = []
        boxes = {nid: self._box(n) for nid, n in self.nodes.items() if n["type"] != "boundaryEvent"}
        for st in self.stores:
            boxes[st["id"]] = (st["cx"] - 25, st["cy"] - 25, st["cx"] + 25, st["cy"] + 25)
        # Lane-Zugehoerigkeit: Knoten muss in seiner Lane liegen
        lb, _ = self.lane_bounds()
        for nid, n in self.nodes.items():
            y0, h = lb[n["lane"]]
            _, by0, _, by1 = self._box(n)
            if n["type"] != "boundaryEvent" and (by0 < y0 or by1 > y0 + h):
                problems.append(f"{nid} liegt ausserhalb seiner Lane")
        ids = list(boxes)
        for i in range(len(ids)):
            for j in range(i + 1, len(ids)):
                a_, b_ = boxes[ids[i]], boxes[ids[j]]
                if a_[0] < b_[2] and b_[0] < a_[2] and a_[1] < b_[3] and b_[1] < a_[3]:
                    problems.append(f"Ueberlappung: {ids[i]} / {ids[j]}")

        def seg_hits(p, q, box):
            x0, y0, x1, y1 = box[0] + 1, box[1] + 1, box[2] - 1, box[3] - 1
            (px, py), (qx, qy) = p, q
            if abs(px - qx) < 0.5:  # senkrecht
                return x0 < px < x1 and min(py, qy) < y1 and max(py, qy) > y0
            return y0 < py < y1 and min(px, qx) < x1 and max(px, qx) > x0

        for f in self.flows:
            pts = self.points(f)
            for k in range(len(pts) - 1):
                for nid, box in boxes.items():
                    if nid in (f["src"], f["tgt"]):
                        continue
                    if self.nodes.get(nid, {}).get("type") == "boundaryEvent":
                        continue
                    if seg_hits(pts[k], pts[k + 1], box):
                        problems.append(f"Pfeil {f['id']} laeuft durch {nid}")
        return problems


# =====================================================================
# IST-PROZESS
# =====================================================================
def build_ist():
    m = Model("Ist", "ShopEasy GmbH – Retourenabwicklung (Ist-Prozess)",
              [("Lane_Kunde", "Kund:in", 240), ("Lane_Service", "Kundenservice / Sachbearbeitung", 260),
               ("Lane_Lager", "Lager (WMS)", 220), ("Lane_Buch", "Buchhaltung", 380)], 2900)
    K, S, Lg, B = "Lane_Kunde", "Lane_Service", "Lane_Lager", "Lane_Buch"
    m.node("s_web", "startEvent", "Retoure im Webshop-Formular angemeldet", 190, 40, K, ev="message")
    m.node("s_mkt", "startEvent", "Retoure im Marktplatz-Tool angemeldet (Amazon/Zalando)", 190, 120, K, ev="message")
    m.node("s_mail", "startEvent", "Retoure formlos per E-Mail angemeldet", 190, 200, K, ev="message")
    m.node("t_mkt", "manualTask", "Marktplatz-Portal sichten, Daten abtippen", 330, 290, S, doc="Eigene API je Marktplatz, keine Anbindung an das ERP (Medienbruch).")
    m.node("t_mail", "manualTask", "E-Mail lesen, Daten nachfragen, abtippen", 330, 460, S, doc="Unstrukturierte Anmeldung; fehlende Angaben werden per E-Mail nachgefragt.")
    m.node("t_erp", "manualTask", "Anmeldung im ERP manuell erfassen & prüfen", 520, 370, S, doc="Mehrfache manuelle Datenpflege; Prüfung nach Erfahrung der Sachbearbeiter:in.")
    m.node("t_label", "manualTask", "Retourenlabel manuell im Versandtool erstellen", 690, 370, S)
    m.node("t_send", "manualTask", "Label per E-Mail an Kund:in senden", 850, 370, S)
    m.node("t_ship", "task", "Ware zurücksenden (kein Status-Tracking)", 1010, 120, K, doc="Weder Kund:in noch ShopEasy sehen den Status der Sendung.")
    m.node("ev_wait", "intermediateCatchEvent", "Paket trifft im Lager ein", 1150, 580, Lg, ev="message")
    m.node("t_gr", "manualTask", "Wareneingang & Zustandsprüfung im WMS erfassen (oft verzögert)", 1300, 580, Lg, doc="Zustand: neuwertig / beschädigt / getragen. Subjektiv, Erfassung zeitversetzt.")
    m.node("t_dec", "manualTask", "Annahme/Ablehnung entscheiden", 1470, 370, S, doc="Entscheidung nicht dokumentiert und nicht regelbasiert.")
    m.node("gw1", "exclusiveGateway", "Ergebnis?", 1620, 370, S)
    m.node("t_esc", "manualTask", "Grenzfall per E-Mail eskalieren & dokumentieren", 1780, 290, S, doc="Dokumentation nur im E-Mail-Postfach.")
    m.node("t_stock", "manualTask", "Bestandsbuchung im ERP", 1780, 370, S)
    m.node("t_reject", "manualTask", "Rückversand / Entsorgung / Spende veranlassen", 1780, 580, Lg)
    m.node("gw_ref", "exclusiveGateway", "Erstattungsart?", 1900, 850, B)
    m.node("t_pay", "manualTask", "Rückerstattung manuell über Zahlungsdienstleister", 2060, 770, B, doc="Manuelle Buchung; größter Anteil an der Durchlaufzeit von 8–12 Tagen.")
    m.node("t_voucher", "manualTask", "Gutschein manuell im CRM eintragen", 2060, 950, B)
    m.node("gw_ref_m", "exclusiveGateway", None, 2250, 850, B)
    m.node("gw_m", "exclusiveGateway", None, 2330, 370, S)
    m.node("t_comm", "manualTask", "Kund:in per E-Mail informieren (verzögert)", 2480, 370, S, doc="Häufige Nachfragen der Kund:innen.")
    m.node("t_report", "manualTask", "Monatliches Reporting in Excel erstellen", 2650, 370, S, doc="Retourenquote, Bearbeitungsdauer, Gründe – manuell zusammengestellt.")
    m.node("end", "endEvent", "Retoure abgeschlossen", 2800, 370, S)

    # beteiligte Systeme (Seite 4 der Aufgabenstellung)
    m.store("DS_web", "Webshop-Frontend", 520, 285, "t_erp", "in", "t", "t")
    m.store("DS_mkt", "Marktplatz-Portale", 330, 366, "t_mkt", "in", "b", "b")
    m.store("DS_erp1", "ERP-System", 520, 460, "t_erp", "out", "b")
    m.store("DS_vers", "Versandtool", 690, 460, "t_label", "out", "b")
    m.store("DS_mail1", "E-Mail-Postfach", 850, 460, "t_send", "out", "b")
    m.store("DS_wms", "WMS", 1300, 665, "t_gr", "out", "b")
    m.store("DS_mail2", "E-Mail-Postfach", 1890, 290, "t_esc", "out", "r", "b")
    m.store("DS_erp2", "ERP-System", 1780, 460, "t_stock", "out", "b")
    m.store("DS_zdl", "Zahlungsdienstleister", 2060, 850, "t_pay", "out", "b")
    m.store("DS_crm", "CRM", 2060, 1030, "t_voucher", "out", "b")
    m.store("DS_mail3", "E-Mail-Postfach", 2480, 460, "t_comm", "out", "b")
    m.store("DS_xls", "Excel-Listen", 2650, 460, "t_report", "out", "b")

    f = m.flow
    f("f1", "s_web", "t_erp", route=("hvh", 430))
    f("f2", "s_mkt", "t_mkt", route=("hvh", 255))
    f("f3", "s_mail", "t_mail", route=("hvh", 225))
    f("f4", "t_mkt", "t_erp", route=("hvh", 430))
    f("f5", "t_mail", "t_erp", route=("hvh", 430))
    f("f6", "t_erp", "t_label")
    f("f7", "t_label", "t_send")
    f("f8", "t_send", "t_ship")
    f("f9", "t_ship", "ev_wait")
    f("f10", "ev_wait", "t_gr")
    f("f11", "t_gr", "t_dec")
    f("f12", "t_dec", "gw1")
    f("f13", "gw1", "t_stock", "Annahme")
    f("f14", "gw1", "t_reject", "Ablehnung", route=("vh", "b"))
    f("f15", "gw1", "t_esc", "Grenzfall", route=("vh", "t"))
    f("f16", "t_esc", "t_dec", "erneut entscheiden", route=("via", "t", "t", [(1780, 225), (1470, 225)]))
    f("f17", "t_stock", "gw_ref", route=("hvh", 1855))
    f("f18", "gw_ref", "t_pay", "Geld zurück", route=("vh", "t"))
    f("f19", "gw_ref", "t_voucher", "Gutschein", route=("vh", "b"))
    f("f20", "t_pay", "gw_ref_m", route=("hv", "t"))
    f("f21", "t_voucher", "gw_ref_m", route=("hv", "b"))
    f("f22", "gw_ref_m", "gw_m", route=("hv", "b"))
    f("f23", "t_reject", "gw_m", route=("hv", "b"))
    f("f24", "gw_m", "t_comm")
    f("f25", "t_comm", "t_report")
    f("f26", "t_report", "end")
    m.validate()
    return m


# =====================================================================
# SOLL-PROZESS
# =====================================================================
def build_soll():
    m = Model("Soll", "ShopEasy GmbH – Retourenabwicklung (Soll-Prozess)",
              [("Lane_Kanal", "Kund:in / Eingangskanäle", 260), ("Lane_WF", "Retouren-Workflow (automatisiert)", 360),
               ("Lane_Lager", "Lager", 160), ("Lane_SB", "Sachbearbeitung / Kundenservice", 280)], 3200)
    K, W, Lg, SB = "Lane_Kanal", "Lane_WF", "Lane_Lager", "Lane_SB"
    # Umsetzungsart steht im Namen (zweite Zeile) und in der Dokumentation
    m.node("s_web", "startEvent", "Retoure im Webshop angemeldet", 190, 60, K, ev="message", doc="Umsetzung: System/Service (Webshop sendet Retourenmeldung per REST an Retouren-Plattform).")
    m.node("s_mkt", "startEvent", "Marktplatz-Retoure eingegangen (REST)", 190, 130, K, ev="message", doc="Umsetzung: System/Service (Sprint 2: Mock-REST-API Amazon/Zalando).")
    m.node("s_mail", "startEvent", "Retoure per E-Mail angemeldet", 190, 200, K, ev="message")
    m.node("t_parse", "serviceTask", "E-Mail auslesen und strukturieren\n«KI»", 260, 540, W, doc="Umsetzung: KI (LLM-Extraktion von Bestellnummer, Artikel, Grund aus Freitext), Sprint 5.")
    m.node("t_create", "serviceTask", "Retourenfall zentral anlegen\n«System»", 420, 400, W, doc="Umsetzung: System/Service. Gemeinsame Datenbasis, Fall-ID für alle Kanäle.")
    m.node("t_ai", "serviceTask", "Retourengrund klassifizieren & Betrugsscore berechnen\n«KI-Agent»", 580, 400, W, doc="Umsetzung: KI-Agent (n8n), Sprint 5. Klassifikation des Retourengrunds aus Freitext, Erkennung auffälliger Muster (Wardrobing).")
    m.node("t_rule", "businessRuleTask", "Anmeldung prüfen (Frist, Artikel, Kundenhistorie)\n«Regel/DMN»", 740, 400, W, doc="Umsetzung: Regeln (DMN). Entscheidung zulässig / Verdacht / unzulässig.")
    m.node("gw1", "exclusiveGateway", "Prüfergebnis?", 900, 400, W)
    m.node("t_deny", "sendTask", "Ablehnung mitteilen\n«System»", 1060, 310, W, doc="Umsetzung: System (Nachricht über Broker, E-Mail an Kund:in).")
    m.node("end_deny", "endEvent", "Retoure abgelehnt", 1230, 310, W)
    m.node("t_man1", "userTask", "Verdacht manuell prüfen\n«Mensch»", 1060, 880, SB, doc="Umsetzung: Mensch. Entscheidung wird begründet dokumentiert.")
    m.node("t_deny2", "sendTask", "Ablehnung mitteilen\n«System»", 1220, 980, SB)
    m.node("end_deny2", "endEvent", "Retoure abgelehnt", 1380, 980, SB)
    m.node("gw_join", "exclusiveGateway", None, 1060, 400, W)
    m.node("t_label", "serviceTask", "Retourenlabel erzeugen\n«System»", 1200, 400, W, doc="Umsetzung: System/Service (Versanddienstleister-API).")
    m.node("b_label", "boundaryEvent", "Versand-API nicht erreichbar", 1230, 440, W, ev="error", attached="t_label")
    m.node("t_label_man", "userTask", "Label manuell erstellen\n«Mensch»", 1360, 880, SB, doc="Fehlerbehandlung: Fallback bei Ausfall der Versand-API.")
    m.node("t_notify", "sendTask", "Label & Status „Retoure genehmigt“ senden\n«System/Messaging»", 1360, 400, W, doc="Umsetzung: Messaging (Sprint 2: Status-Update über Message Broker).")
    m.node("gw_evt", "eventBasedGateway", None, 1500, 400, W)
    m.node("ev_arrive", "intermediateCatchEvent", "Paket im Lager eingetroffen", 1620, 400, W, ev="message", doc="Korrelation über Retouren-ID/Tracking.")
    m.node("ev_timeout", "intermediateCatchEvent", "21 Tage ohne Wareneingang", 1620, 310, W, ev="timer:P21D")
    m.node("t_expired", "sendTask", "Retoure als nicht eingegangen schließen\n«System»", 1780, 310, W)
    m.node("end_expired", "endEvent", "Fall geschlossen (Frist abgelaufen)", 1940, 310, W)
    m.node("t_gr", "userTask", "Zustand prüfen & per Erfassungs-Client buchen\n«Mensch + Service»", 1780, 700, Lg, doc="Umsetzung: Mensch am Scanner, Daten über gRPC-Service (Sprint 2) mit definierter Zustandsskala.")
    m.node("t_decide", "businessRuleTask", "Annahme/Ablehnung entscheiden\n«Regel/DMN»", 1940, 400, W, doc="Umsetzung: Regeln (DMN) auf Zustand, Warenwert, Betrugsscore.")
    m.node("gw2", "exclusiveGateway", "Ergebnis?", 2100, 400, W)
    m.node("gw_acc", "exclusiveGateway", None, 2260, 400, W)
    m.node("t_reject", "userTask", "Weiterverwendung festlegen (Rückversand / Spende / Entsorgung)\n«Mensch»", 2260, 700, Lg)
    m.node("t_man2", "userTask", "Grenzfall prüfen & Entscheidung dokumentieren\n«Mensch»", 2260, 880, SB, doc="Umsetzung: Mensch, KI-Empfehlung als Entscheidungshilfe. Ersetzt Eskalation per E-Mail.")
    m.node("gw_par", "parallelGateway", None, 2400, 400, W)
    m.node("t_stock", "serviceTask", "Bestand im ERP buchen / korrigieren\n«RPA»", 2560, 330, W, doc="Umsetzung: RPA (UiPath-Bot im ERP-Frontend), Sprint 5.")
    m.node("t_refund", "serviceTask", "Rückerstattung auslösen / Gutschein anlegen\n«System»", 2560, 470, W, doc="Umsetzung: System/Service (Zahlungsdienstleister-API, CRM).")
    m.node("b_refund", "boundaryEvent", "Zahlung fehlgeschlagen", 2590, 510, W, ev="error", attached="t_refund")
    m.node("t_refund_man", "userTask", "Rückerstattung manuell klären\n«Mensch»", 2720, 880, SB, doc="Fehlerbehandlung: Fallback bei fehlgeschlagener Zahlung.")
    m.node("gw_pj", "parallelGateway", None, 2720, 400, W)
    m.node("t_end_notify", "sendTask", "Kund:in über Ergebnis informieren\n«System/Messaging»", 2880, 400, W, doc="Umsetzung: Messaging. Status-Events speisen auch Dashboard / Process Mining (ersetzt Excel-Reporting).")
    m.node("end", "endEvent", "Retoure abgeschlossen", 3030, 400, W)
    m.annots.append(dict(id="Ann_1", x=2700, y=110, w=360, h=90,
                         text="Alle Statuswechsel werden als Events publiziert → Echtzeit-Dashboard und Process Mining ersetzen das manuelle monatliche Excel-Reporting."))
    m.assocs.append(("Ann_1", "t_end_notify"))
    # W15: Tracking/Aufkommen fuer Schichtplanung, W13: Status bei jedem Statuswechsel
    m.annots.append(dict(id="Ann_2", x=1180, y=110, w=360, h=90,
                         text="Tracking-Events und erwartetes Retourenaufkommen je Tag fließen in die Schichtplanung des Lagers (W15). Kosten der Sendungsverfolgung gegen Nutzen abwägen."))
    m.assocs.append(("Ann_2", "t_notify"))
    m.annots.append(dict(id="Ann_3", x=300, y=100, w=240, h=100,
                         text="Statusnachricht an Kund:in bei jedem Statuswechsel: angelegt, Label, eingegangen, Entscheidung, erstattet (W13)."))
    m.assocs.append(("Ann_3", "t_create"))

    f = m.flow
    f("f1", "s_web", "t_create", route=("hvh", 284))
    f("f2", "s_mkt", "t_create", route=("hvh", 284))
    f("f3", "s_mail", "t_parse")
    f("f4", "t_parse", "t_create", route=("hvh", 340))
    f("f5", "t_create", "t_ai")
    f("f6", "t_ai", "t_rule")
    f("f7", "t_rule", "gw1")
    f("f8", "gw1", "gw_join", "zulässig")
    f("f9", "gw1", "t_deny", "unzulässig", route=("vh", "t"))
    f("f10", "gw1", "t_man1", "Verdacht", route=("vh", "b"))
    f("f11", "t_deny", "end_deny")
    f("f12", "t_man1", "gw_join", "freigegeben", route=("via", "t", "b", [], 0), cond="=freigegeben = true")
    f("f13", "t_man1", "t_deny2", "abgelehnt", route=("via", "b", "l", [(1060, 980)]), cond="=freigegeben = false")
    f("f14", "t_deny2", "end_deny2")
    f("f15", "gw_join", "t_label")
    f("f16", "b_label", "t_label_man", route=("via", "b", "l", [(1230, 880)]))
    f("f17", "t_label_man", "t_notify", route=("via", "t", "b", []))
    f("f18", "t_label", "t_notify")
    f("f19", "t_notify", "gw_evt")
    f("f20", "gw_evt", "ev_arrive")
    f("f21", "gw_evt", "ev_timeout", route=("vh", "t"))
    f("f22", "ev_timeout", "t_expired")
    f("f23", "t_expired", "end_expired")
    f("f24", "ev_arrive", "t_gr", route=("hvh", 1709))
    f("f25", "t_gr", "t_decide", route=("hvh", 1860))
    f("f26", "t_decide", "gw2")
    f("f27", "gw2", "gw_acc", "Annahme")
    f("f28", "gw2", "t_reject", "Ablehnung", route=("vh", "b"))
    f("f29", "gw2", "t_man2", "Grenzfall", route=("via", "b", "l", [(2100, 880)]))
    f("f30", "t_man2", "t_reject", "abgelehnt", route=("via", "t", "b", []), cond="=angenommen = false")
    f("f31", "t_man2", "gw_acc", "angenommen", route=("via", "r", "b", [(2420, 880), (2420, 470), (2260, 470)]), cond="=angenommen = true")
    f("f32", "gw_acc", "gw_par")
    f("f33", "gw_par", "t_stock", route=("hvh", 2467))
    f("f34", "gw_par", "t_refund", route=("hvh", 2467))
    f("f35", "t_stock", "gw_pj", route=("hvh", 2652))
    f("f36", "t_refund", "gw_pj", route=("hvh", 2652))
    f("f37", "b_refund", "t_refund_man", route=("via", "b", "l", [(2590, 880)]))
    f("f38", "t_refund_man", "gw_pj", route=("via", "t", "b", []))
    f("f39", "gw_pj", "t_end_notify")
    f("f40", "t_reject", "t_end_notify", route=("via", "r", "b", [(2880, 700)]))
    f("f41", "t_end_notify", "end")
    m.validate()
    return m


if __name__ == "__main__":
    outdir = sys.argv[1]
    for name, builder in (("ist-prozess", build_ist), ("soll-prozess", build_soll)):
        model = builder()
        problems = model.layout_check()
        open(f"{outdir}/{name}.bpmn", "w", encoding="utf-8").write(model.xml())
        print(f"{name}: geschrieben, {len(problems)} Layout-Hinweise")
        for p in problems:
            print("  -", p)
