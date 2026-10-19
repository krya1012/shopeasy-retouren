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
    def __init__(self, pid, pool_name, lanes, width, y0=0):
        self.pid, self.pool_name, self.lanes, self.width, self.y0 = pid, pool_name, lanes, width, y0
        self.nodes, self.flows, self.annots, self.assocs, self.stores = {}, [], [], [], []
        self.externals, self.mflows = {}, []

    def external(self, id, name, x, y, w, h=60):
        """Eingeklappter Pool (Black Box) eines externen Teilnehmers."""
        self.externals[id] = dict(id=id, name=name, x=x, y=y, w=w, h=h)

    def msg(self, id, src, tgt, side="t", pts=None):
        """Nachrichtenfluss zwischen externem Pool und Knoten. side: Anschlussseite am Knoten."""
        self.mflows.append(dict(id=id, src=src, tgt=tgt, side=side, pts=pts))

    def mpoints(self, mf):
        if mf["pts"]:
            return mf["pts"]
        s, t = mf["src"], mf["tgt"]
        if s in self.externals:
            e = self.externals[s]
            end = self.anchor(t, mf["side"])
            return [(end[0], e["y"] + e["h"]), end]
        e = self.externals[t]
        start = self.anchor(s, mf["side"])
        return [start, (start[0], e["y"] + e["h"])]

    def node(self, id, type, name, cx, cy, lane, doc=None, ev=None, attached=None, cancel=True, lbl=None):
        self.nodes[id] = dict(id=id, type=type, name=name, cx=cx, cy=cy, lane=lane, doc=doc, ev=ev,
                              attached=attached, cancel=cancel, lbl=lbl)

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
            return cx - 60, cy + 26, 120, 27
        if st["label"] == "t":
            return cx - 60, cy - 53, 120, 27
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
        out, y = {}, self.y0
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
        for e in self.externals.values():
            a(f'    <bpmn:participant id="{e["id"]}" name={quoteattr(e["name"])} />')
        for mf in self.mflows:
            a(f'    <bpmn:messageFlow id="{mf["id"]}" sourceRef="{mf["src"]}" targetRef="{mf["tgt"]}" />')
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
            elif ev and ev.startswith("timercycle:"):
                evx = (f'<bpmn:timerEventDefinition><bpmn:timeCycle xsi:type="bpmn:tFormalExpression">'
                       f'{ev[11:]}</bpmn:timeCycle></bpmn:timerEventDefinition>')
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
        a(f'        <dc:Bounds x="0" y="{self.y0}" width="{self.width}" height="{total_h - self.y0}" />')
        a('      </bpmndi:BPMNShape>')
        for e in self.externals.values():
            a(f'      <bpmndi:BPMNShape id="{e["id"]}_di" bpmnElement="{e["id"]}" isHorizontal="true">')
            a(f'        <dc:Bounds x="{e["x"]}" y="{e["y"]}" width="{e["w"]}" height="{e["h"]}" />')
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
            if n["lbl"]:
                lx, ly, lw, lh = n["lbl"]
                a(f'        <bpmndi:BPMNLabel><dc:Bounds x="{lx}" y="{ly}" width="{lw}" height="{lh}" /></bpmndi:BPMNLabel>')
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
        for mf in self.mflows:
            a(f'      <bpmndi:BPMNEdge id="{mf["id"]}_di" bpmnElement="{mf["id"]}">')
            for x, y in self.mpoints(mf):
                a(f'        <di:waypoint x="{x:g}" y="{y:g}" />')
            a('      </bpmndi:BPMNEdge>')
        for st in self.stores:
            a(f'      <bpmndi:BPMNEdge id="Assoc_{st["id"]}_di" bpmnElement="Assoc_{st["id"]}">')
            for x, y in self.store_edge(st):
                a(f'        <di:waypoint x="{x:g}" y="{y:g}" />')
            a('      </bpmndi:BPMNEdge>')
        for i, (src, tgt) in enumerate(self.assocs):
            an = next(x for x in self.annots if x["id"] == src)
            if an["y"] + an["h"] <= self.nodes[tgt]["cy"]:
                p1, p2 = (an["x"] + an["w"] / 2, an["y"] + an["h"]), self.anchor(tgt, "t")
            else:
                p1, p2 = (an["x"] + an["w"] / 2, an["y"]), self.anchor(tgt, "b")
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
        for mf in self.mflows:
            ext, nid = (mf["src"], mf["tgt"]) if mf["src"] in self.externals else (mf["tgt"], mf["src"])
            assert ext in self.externals and nid in ids, mf
            if not mf["pts"]:
                e, n = self.externals[ext], self.nodes[nid]
                assert e["x"] <= n["cx"] <= e["x"] + e["w"], f"{mf['id']}: {nid} liegt ausserhalb von Pool {ext}"
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
            lx, ly, lw, lh = self.store_label(st)
            boxes[st["id"] + "_Label"] = (lx, ly, lx + lw, ly + lh)
        for an in self.annots:
            boxes[an["id"]] = (an["x"], an["y"], an["x"] + an["w"], an["y"] + an["h"])
        for nid, n in self.nodes.items():
            if n["lbl"]:
                lx, ly, lw, lh = n["lbl"]
                boxes[nid + "_Label"] = (lx, ly, lx + lw, ly + lh)
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
                    if nid in (f["src"], f["tgt"]) and not nid.endswith("_Label"):
                        continue
                    if self.nodes.get(nid, {}).get("type") == "boundaryEvent":
                        continue
                    if seg_hits(pts[k], pts[k + 1], box):
                        problems.append(f"Pfeil {f['id']} laeuft durch {nid}")
        for mf in self.mflows:
            pts = self.mpoints(mf)
            for k in range(len(pts) - 1):
                for nid, box in boxes.items():
                    if nid in (mf["src"], mf["tgt"]) or self.nodes.get(nid, {}).get("type") == "boundaryEvent":
                        continue
                    if seg_hits(pts[k], pts[k + 1], box):
                        problems.append(f"Nachrichtenfluss {mf['id']} laeuft durch {nid}")
        return problems


# =====================================================================
# IST-PROZESS
# =====================================================================
def build_ist():
    # Pool ShopEasy beginnt bei y=140; darueber die eingeklappten Pools der externen Teilnehmer.
    # Die Lanes sind Rollen innerhalb von ShopEasy (Vorlesung: Pool = Teilnehmer, Lane = Rolle).
    m = Model("Ist", "ShopEasy GmbH – Retourenabwicklung (Ist-Prozess)",
              [("Lane_Service", "Kundenservice / Sachbearbeitung", 660), ("Lane_Lager", "Lager (WMS)", 230),
               ("Lane_Buch", "Buchhaltung", 380)], 2900, y0=140)
    S, Lg, B = "Lane_Service", "Lane_Lager", "Lane_Buch"
    m.external("Ext_Markt", "Marktplatz (Amazon, Zalando)", 30, 0, 260)
    m.external("Ext_Kunde", "Kund:in", 300, 0, 2600)

    # IT-gestuetzte Schritte sind "task" (undefiniert, Systeme als Datenspeicher), nur Schritte ohne IT sind "manualTask"
    m.node("s_mkt", "startEvent", "Retoure im Marktplatz-Tool angemeldet", 190, 185, S, ev="message", lbl=(214, 162, 96, 46))
    m.node("s_web", "startEvent", "Retoure im Webshop-Formular angemeldet", 330, 185, S, ev="message", lbl=(354, 162, 96, 46))
    m.node("s_mail", "startEvent", "Retoure formlos per E-Mail angemeldet", 470, 185, S, ev="message", lbl=(494, 162, 100, 46))
    m.node("t_mkt", "task", "Marktplatz-Portal sichten, Daten abtippen", 190, 340, S, doc="Eigene API je Marktplatz, keine Anbindung an das ERP (Medienbruch).")
    m.node("t_mail", "task", "E-Mail lesen, Daten nachfragen, abtippen", 470, 340, S, doc="Unstrukturierte Anmeldung; fehlende Angaben werden per E-Mail nachgefragt.")
    m.node("t_erp", "task", "Anmeldung im ERP manuell erfassen & prüfen", 640, 440, S, doc="Mehrfache manuelle Datenpflege; Prüfung nach Erfahrung der Sachbearbeiter:in.")
    m.node("gw_ok", "exclusiveGateway", "Anmeldung gültig?", 790, 440, S)
    m.node("t_rej", "task", "Ablehnung per E-Mail mitteilen", 950, 320, S, doc="Z. B. abgelaufene Frist oder nicht retournierbarer Artikel; wird von Hand aussortiert.")
    m.node("end_rej", "endEvent", "Retoure abgelehnt", 1080, 320, S)
    m.node("t_label", "task", "Retourenlabel manuell im Versandtool erstellen", 950, 440, S)
    m.node("t_send", "task", "Label per E-Mail an Kund:in senden", 1140, 440, S, doc="Danach schickt die Kund:in die Ware zurück; ShopEasy sieht keinen Sendungsstatus.")
    m.node("ev_wait", "intermediateCatchEvent", "Paket trifft im Lager ein", 1290, 880, Lg, ev="message", doc="Kein Status-Tracking zwischen Rückversand und Eintreffen im Lager.")
    m.node("t_gr", "task", "Wareneingang & Zustandsprüfung im WMS erfassen (oft verzögert)", 1440, 880, Lg, doc="Zustand: neuwertig / beschädigt / getragen. Subjektiv, Erfassung zeitversetzt.")
    m.node("t_dec", "manualTask", "Annahme / Ablehnung entscheiden", 1600, 440, S, doc="Entscheidung nicht dokumentiert und nicht regelbasiert.")
    m.node("gw1", "exclusiveGateway", "Ergebnis?", 1750, 440, S, lbl=(1688, 392, 56, 22))
    m.node("t_esc", "task", "Grenzfall per E-Mail eskalieren & dokumentieren", 1910, 360, S, doc="Dokumentation nur im E-Mail-Postfach.")
    m.node("t_stock", "task", "Bestand im ERP buchen", 1910, 440, S)
    m.node("t_reject", "manualTask", "Rückversand / Entsorgung / Spende veranlassen", 1910, 880, Lg)
    m.node("gw_ref", "exclusiveGateway", "Erstattungsart?", 2030, 1160, B, lbl=(2058, 1148, 100, 24))
    m.node("t_pay", "task", "Rückerstattung manuell veranlassen", 2190, 1080, B, doc="Manuelle Buchung; größter Anteil an der Durchlaufzeit von 8–12 Tagen.")
    m.node("t_voucher", "task", "Gutschein manuell im CRM eintragen", 2190, 1260, B)
    m.node("gw_ref_m", "exclusiveGateway", None, 2380, 1160, B)
    m.node("gw_m", "exclusiveGateway", None, 2460, 440, S)
    m.node("t_comm", "task", "Kund:in per E-Mail informieren (verzögert)", 2610, 440, S, doc="Häufige Nachfragen der Kund:innen.")
    m.node("end", "endEvent", "Retoure abgeschlossen", 2770, 440, S)
    # Reporting ist ein eigener Ablauf (monatlich), nicht Teil des Falls
    m.node("s_rep", "startEvent", "Monatsende", 190, 640, S, ev="timercycle:R/P1M")
    m.node("t_report", "task", "Monatliches Reporting in Excel erstellen", 350, 640, S, doc="Retourenquote, Bearbeitungsdauer, Gründe – manuell aus E-Mails und Listen zusammengestellt.")
    m.node("end_rep", "endEvent", "Report erstellt", 510, 640, S)

    # beteiligte Systeme (Seite 4 der Aufgabenstellung)
    m.store("DS_web", "Webshop-Frontend", 640, 350, "t_erp", "in", "t", "t")
    m.store("DS_mkt", "Marktplatz-Portale", 190, 440, "t_mkt", "in", "b", "b")
    m.store("DS_erp1", "ERP-System", 640, 530, "t_erp", "out", "b")
    m.store("DS_vers", "Versandtool", 950, 530, "t_label", "out", "b")
    m.store("DS_mail1", "E-Mail-Postfach", 1140, 530, "t_send", "out", "b")
    m.store("DS_wms", "WMS", 1440, 965, "t_gr", "out", "b")
    m.store("DS_mail2", "E-Mail-Postfach", 2030, 360, "t_esc", "out", "r", "b")
    m.store("DS_erp2", "ERP-System", 1910, 530, "t_stock", "out", "b")
    m.store("DS_zdl", "Zahlungsdienstleister", 2190, 1160, "t_pay", "out", "b")
    m.store("DS_crm", "CRM", 2190, 1340, "t_voucher", "out", "b")
    m.store("DS_mail3", "E-Mail-Postfach", 2610, 530, "t_comm", "out", "b")
    m.store("DS_xls", "Excel-Listen", 350, 730, "t_report", "out", "b")

    f = m.flow
    f("f1", "s_mkt", "t_mkt", route=("via", "b", "t", []))
    f("f2", "s_web", "t_erp", route=("vh", "b"))
    f("f3", "s_mail", "t_mail", route=("via", "b", "t", []))
    f("f4", "t_mkt", "t_erp", route=("hvh", 290))
    f("f5", "t_mail", "t_erp", route=("vh", "b"))
    f("f6", "t_erp", "gw_ok")
    f("f7", "gw_ok", "t_label", "ja")
    f("f8", "gw_ok", "t_rej", "nein", route=("vh", "t"))
    f("f9", "t_rej", "end_rej")
    f("f10", "t_label", "t_send")
    f("f11", "t_send", "ev_wait", route=("hvh", 1235))
    f("f12", "ev_wait", "t_gr")
    f("f13", "t_gr", "t_dec")
    f("f14", "t_dec", "gw1")
    f("f15", "gw1", "t_stock", "Annahme")
    f("f16", "gw1", "t_reject", "Ablehnung", route=("vh", "b"))
    f("f17", "gw1", "t_esc", "Grenzfall", route=("vh", "t"))
    f("f18", "t_esc", "t_dec", "erneut entscheiden", route=("via", "t", "t", [(1910, 295), (1600, 295)]))
    f("f19", "t_stock", "gw_ref", route=("hvh", 1985))
    f("f20", "gw_ref", "t_pay", "Geld zurück", route=("vh", "t"))
    f("f21", "gw_ref", "t_voucher", "Gutschein", route=("vh", "b"))
    f("f22", "t_pay", "gw_ref_m", route=("hv", "t"))
    f("f23", "t_voucher", "gw_ref_m", route=("hv", "b"))
    f("f24", "gw_ref_m", "gw_m", route=("hv", "b"))
    f("f25", "t_reject", "gw_m", route=("hv", "b"))
    f("f26", "gw_m", "t_comm")
    f("f27", "t_comm", "end")
    f("f28", "s_rep", "t_report")
    f("f29", "t_report", "end_rep")

    # Nachrichtenfluesse zu den externen Teilnehmern
    m.msg("m1", "Ext_Markt", "s_mkt")
    m.msg("m2", "Ext_Kunde", "s_web")
    m.msg("m3", "Ext_Kunde", "s_mail")
    m.msg("m4", "t_rej", "Ext_Kunde")
    m.msg("m5", "t_send", "Ext_Kunde")
    m.msg("m6", "Ext_Kunde", "ev_wait")
    m.msg("m7", "t_comm", "Ext_Kunde")
    m.validate()
    return m


# =====================================================================
# SOLL-PROZESS
# =====================================================================
def build_soll():
    m = Model("Soll", "ShopEasy GmbH – Retourenabwicklung (Soll-Prozess)",
              [("Lane_WF", "Retouren-Workflow (automatisiert)", 620), ("Lane_Lager", "Lager", 230),
               ("Lane_SB", "Sachbearbeitung / Kundenservice", 330)], 3200, y0=140)
    W, Lg, SB = "Lane_WF", "Lane_Lager", "Lane_SB"
    m.external("Ext_Markt", "Marktplatz (Amazon, Zalando)", 30, 0, 260)
    m.external("Ext_Kunde", "Kund:in", 300, 0, 2900)
    # Umsetzungsart steht im Namen (zweite Zeile) und in der Dokumentation
    m.node("s_mkt", "startEvent", "Marktplatz-Retoure eingegangen (REST)", 190, 185, W, ev="message", lbl=(214, 162, 100, 46), doc="Umsetzung: System/Service (Sprint 2: Mock-REST-API Amazon/Zalando).")
    m.node("s_web", "startEvent", "Retoure im Webshop angemeldet", 350, 185, W, ev="message", lbl=(374, 162, 100, 46), doc="Umsetzung: System/Service (Webshop sendet Retourenmeldung per REST an Retouren-Plattform).")
    m.node("s_mail", "startEvent", "Retoure per E-Mail angemeldet", 510, 185, W, ev="message", lbl=(534, 162, 100, 46))
    m.node("t_parse", "serviceTask", "E-Mail auslesen und strukturieren\n«KI»", 510, 330, W, doc="Umsetzung: KI (LLM-Extraktion von Bestellnummer, Artikel, Grund aus Freitext), Sprint 5.")
    m.node("t_create", "serviceTask", "Retourenfall zentral anlegen\n«System»", 350, 470, W, doc="Umsetzung: System/Service. Gemeinsame Datenbasis, Fall-ID für alle Kanäle.")
    m.node("t_ai", "serviceTask", "Retourengrund klassifizieren & Betrugsscore berechnen\n«KI-Agent»", 560, 470, W, doc="Umsetzung: KI-Agent (n8n), Sprint 5. Klassifikation des Retourengrunds aus Freitext, Erkennung auffälliger Muster (Wardrobing).")
    m.node("t_rule", "businessRuleTask", "Anmeldung prüfen (Frist, Artikel, Kundenhistorie)\n«Regel/DMN»", 740, 470, W, doc="Umsetzung: Regeln (DMN). Entscheidung zulässig / Verdacht / unzulässig.")
    m.node("gw1", "exclusiveGateway", "Prüfergebnis?", 900, 470, W, lbl=(802, 442, 72, 20))
    m.node("t_deny", "sendTask", "Ablehnung mitteilen\n«System»", 1060, 380, W, doc="Umsetzung: System (Nachricht über Broker, E-Mail an Kund:in).")
    m.node("end_deny", "endEvent", "Retoure abgelehnt", 1230, 380, W)
    m.node("gw_join", "exclusiveGateway", None, 1060, 470, W)
    m.node("t_label", "serviceTask", "Retourenlabel erzeugen\n«System»", 1200, 470, W, doc="Umsetzung: System/Service (Versanddienstleister-API).")
    m.node("b_label", "boundaryEvent", "Versand-API nicht erreichbar", 1235, 510, W, ev="error", attached="t_label", lbl=(1105, 514, 125, 30))
    m.node("t_notify", "sendTask", "Label & Status „genehmigt“ senden\n«Messaging»", 1360, 470, W, doc="Umsetzung: Messaging (Sprint 2: Status-Update über Message Broker).")
    m.node("gw_evt", "eventBasedGateway", None, 1500, 470, W)
    m.node("ev_arrive", "intermediateCatchEvent", "Paket im Lager eingetroffen", 1620, 470, W, ev="message", doc="Korrelation über Retouren-ID/Tracking.")
    m.node("ev_timeout", "intermediateCatchEvent", "21 Tage ohne Wareneingang", 1700, 380, W, ev="timer:P21D")
    m.node("t_expired", "sendTask", "Retoure als nicht eingegangen schließen\n«System»", 1860, 380, W)
    m.node("end_expired", "endEvent", "Fall geschlossen (Frist abgelaufen)", 2020, 380, W)
    m.node("t_decide", "businessRuleTask", "Annahme/Ablehnung entscheiden\n«Regel/DMN»", 1940, 470, W, doc="Umsetzung: Regeln (DMN) auf Zustand, Warenwert, Betrugsscore.")
    m.node("gw2", "exclusiveGateway", "Ergebnis?", 2100, 470, W, lbl=(2062, 420, 76, 22))
    m.node("gw_acc", "exclusiveGateway", None, 2260, 470, W)
    m.node("gw_par", "parallelGateway", None, 2400, 470, W)
    m.node("t_stock", "serviceTask", "Bestand im ERP buchen / korrigieren\n«RPA»", 2560, 400, W, doc="Umsetzung: RPA (UiPath-Bot im ERP-Frontend), Sprint 5.")
    m.node("t_refund", "serviceTask", "Rückerstattung / Gutschein auslösen\n«System»", 2560, 540, W, doc="Umsetzung: System/Service (Zahlungsdienstleister-API, CRM).")
    m.node("b_refund", "boundaryEvent", "Zahlung fehlgeschlagen", 2530, 580, W, ev="error", attached="t_refund", lbl=(2550, 583, 100, 30))
    m.node("gw_rm", "exclusiveGateway", None, 2680, 540, W, doc="Führt Rückerstattung und manuelle Klärung zusammen; so erreicht nur ein Token den parallelen Join.")
    m.node("gw_pj", "parallelGateway", None, 2780, 470, W)
    m.node("t_end_notify", "sendTask", "Kund:in über Ergebnis informieren\n«Messaging»", 2930, 470, W, doc="Umsetzung: Messaging. Status-Events speisen auch Dashboard / Process Mining (ersetzt Excel-Reporting).")
    m.node("end", "endEvent", "Retoure abgeschlossen", 3080, 470, W)
    m.node("t_gr", "userTask", "Zustand prüfen & per Erfassungs-Client buchen\n«Mensch + Service»", 1780, 840, Lg, doc="Umsetzung: Mensch am Scanner, Daten über gRPC-Service (Sprint 2) mit definierter Zustandsskala.")
    m.node("t_reject", "userTask", "Weiterverwendung festlegen (Rückversand / Spende / Entsorgung)\n«Mensch»", 2260, 840, Lg)
    m.node("t_man1", "userTask", "Verdacht manuell prüfen\n«Mensch»", 1060, 1090, SB, doc="Umsetzung: Mensch. Entscheidung wird begründet dokumentiert.")
    m.node("gw_m1", "exclusiveGateway", "Verdacht bestätigt?", 1190, 1090, SB, lbl=(1118, 1118, 68, 30))
    m.node("t_deny2", "sendTask", "Ablehnung mitteilen\n«System»", 1280, 1190, SB)
    m.node("end_deny2", "endEvent", "Retoure abgelehnt", 1440, 1190, SB)
    m.node("t_label_man", "userTask", "Label manuell erstellen\n«Mensch»", 1360, 1090, SB, doc="Fehlerbehandlung: Fallback bei Ausfall der Versand-API.")
    m.node("t_man2", "userTask", "Grenzfall prüfen & Entscheidung dokumentieren\n«Mensch»", 2260, 1090, SB, doc="Umsetzung: Mensch, KI-Empfehlung als Entscheidungshilfe. Ersetzt Eskalation per E-Mail.")
    m.node("gw_m2", "exclusiveGateway", "Angenommen?", 2400, 1090, SB)
    m.node("t_refund_man", "userTask", "Rückerstattung manuell klären\n«Mensch»", 2680, 1090, SB, doc="Fehlerbehandlung: Fallback bei fehlgeschlagener Zahlung.")

    # beteiligte Systeme: nur dort gezeigt, wo ein Service oder Roboter sie anspricht
    m.store("DS_case", "Retouren-Datenbank", 350, 565, "t_create", "out", "b", "b")
    m.store("DS_wms", "WMS", 1780, 930, "t_gr", "out", "b", "b")
    m.store("DS_erp", "ERP-System", 2560, 310, "t_stock", "out", "t", "t")

    m.annots.append(dict(id="Ann_1", x=2970, y=650, w=220, h=100,
                         text="Alle Statuswechsel werden als Events publiziert → Echtzeit-Dashboard und Process Mining ersetzen das manuelle monatliche Excel-Reporting."))
    m.assocs.append(("Ann_1", "end"))
    # W15: Tracking/Aufkommen fuer Schichtplanung, W13: Status bei jedem Statuswechsel
    m.annots.append(dict(id="Ann_2", x=1390, y=650, w=270, h=100,
                         text="Tracking-Events und erwartetes Retourenaufkommen je Tag fließen in die Schichtplanung des Lagers (W15). Kosten der Sendungsverfolgung gegen Nutzen abwägen."))
    m.assocs.append(("Ann_2", "t_notify"))
    m.annots.append(dict(id="Ann_3", x=450, y=650, w=240, h=100,
                         text="Statusnachricht an Kund:in bei jedem Statuswechsel: angelegt, Label, eingegangen, Entscheidung, erstattet (W13)."))
    m.assocs.append(("Ann_3", "t_create"))

    f = m.flow
    f("f1", "s_web", "t_create", route=("via", "b", "t", []))
    f("f2", "s_mkt", "t_create", route=("vh", "b"))
    f("f3", "s_mail", "t_parse", route=("via", "b", "t", []))
    f("f4", "t_parse", "t_create", route=("via", "l", "t", [(350, 330)]))
    f("f5", "t_create", "t_ai")
    f("f6", "t_ai", "t_rule")
    f("f7", "t_rule", "gw1")
    f("f8", "gw1", "gw_join", "zulässig")
    f("f9", "gw1", "t_deny", "unzulässig", route=("vh", "t"))
    f("f10", "gw1", "t_man1", "Verdacht", route=("vh", "b"))
    f("f11", "t_deny", "end_deny")
    f("f12", "t_man1", "gw_m1")
    f("f13", "gw_m1", "gw_join", "nein", route=("via", "t", "b", [(1190, 575), (1060, 575)]), cond="=verdachtBestaetigt = false")
    f("f14", "gw_m1", "t_deny2", "ja", route=("via", "b", "l", [(1190, 1190)]), cond="=verdachtBestaetigt = true")
    f("f15", "t_deny2", "end_deny2")
    f("f16", "gw_join", "t_label")
    f("f17", "b_label", "t_label_man", route=("via", "b", "l", [(1235, 1090)]))
    f("f18", "t_label_man", "t_notify", route=("via", "t", "b", []))
    f("f19", "t_label", "t_notify")
    f("f20", "t_notify", "gw_evt")
    f("f21", "gw_evt", "ev_arrive")
    f("f22", "gw_evt", "ev_timeout", route=("vh", "t"))
    f("f23", "ev_timeout", "t_expired")
    f("f24", "t_expired", "end_expired")
    f("f25", "ev_arrive", "t_gr", route=("hvh", 1680))
    f("f26", "t_gr", "t_decide", route=("hvh", 1860))
    f("f27", "t_decide", "gw2")
    f("f28", "gw2", "gw_acc", "Annahme")
    f("f29", "gw2", "t_reject", "Ablehnung", route=("vh", "b"))
    f("f30", "gw2", "t_man2", "Grenzfall", route=("via", "b", "l", [(2100, 1090)]))
    f("f31", "t_man2", "gw_m2")
    f("f32", "gw_m2", "gw_acc", "ja", route=("via", "r", "b", [(2490, 1090), (2490, 560), (2260, 560)]), cond="=angenommen = true")
    f("f33", "gw_m2", "t_reject", "nein", route=("via", "t", "b", [(2400, 935), (2260, 935)]), cond="=angenommen = false")
    f("f34", "gw_acc", "gw_par")
    f("f35", "gw_par", "t_stock", route=("hvh", 2467))
    f("f36", "gw_par", "t_refund", route=("hvh", 2467))
    f("f37", "t_stock", "gw_pj", route=("hvh", 2717))
    f("f38", "t_refund", "gw_rm")
    f("f39", "b_refund", "t_refund_man", route=("via", "b", "l", [(2530, 1090)]))
    f("f40", "t_refund_man", "gw_rm", route=("via", "t", "b", []))
    f("f41", "gw_rm", "gw_pj", route=("vh", "t"))
    f("f42", "gw_pj", "t_end_notify")
    f("f43", "t_reject", "t_end_notify", route=("via", "r", "b", [(2930, 840)]))
    f("f44", "t_end_notify", "end")

    # Nachrichtenfluesse zu den externen Teilnehmern
    m.msg("m1", "Ext_Markt", "s_mkt")
    m.msg("m2", "Ext_Kunde", "s_web")
    m.msg("m3", "Ext_Kunde", "s_mail")
    m.msg("m4", "t_deny", "Ext_Kunde")
    m.msg("m5", "t_deny2", "Ext_Kunde")
    m.msg("m6", "t_notify", "Ext_Kunde")
    m.msg("m7", "Ext_Kunde", "ev_arrive")
    m.msg("m8", "t_expired", "Ext_Kunde")
    m.msg("m9", "t_end_notify", "Ext_Kunde")
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
