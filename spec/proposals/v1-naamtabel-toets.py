#!/usr/bin/env python3
"""Volledigheids- en consistentietoets van de v0->v1-naamtabel (V1-preconditie P10).

Aanroep, vanuit de repowortel:

    python3 spec/proposals/v1-naamtabel-toets.py [--tabel PAD]

rc 0: de tabel spec/proposals/v1-naamtabel.json is totaal en consistent.
rc 1: minstens een bevinding; elke bevinding staat op stderr met de naam erbij.
rc 2: een bron ontbreekt of is onleesbaar.

Wat de toets eist, en waarom:

1. Totaliteit tegen het schema. Elke knoopdiscriminant (`node`-const/enum)
   en elke propertynaam die het v0-schema ergens declareert, heeft precies
   een rij; elke rij bestaat in het schema. De telmethode is die van de
   B6-meting (80/193 op 245f7152), zodat de aantallen vergelijkbaar blijven.
2. Elke plaats een doel. Een propertyrij met meer doelgroepen verdeelt de
   knopen waarin de naam voorkomt; die verdeling moet exact zijn, anders
   blijft een plaats bij de hernoeming ongenoemd of dubbel.
3. De `v0 name:`-regels van IR-SPEC zijn verantwoord: de identifiers die de
   toets van elke regel leest, staan in documentvolgorde in de tabel, en elk
   heeft een rij of een reden in `buiten_schema`. Een nieuwe of gewijzigde
   regel laat de toets dus falen tot de tabel hem kent.
4. Geen spec-knoopnaam die de tabel mist: elke v1-knoopnaam die IR-SPEC
   ch. 2-4 als knoop voert, is het doel van een knooprij of staat met reden
   in `spec_zonder_v0`.
5. Bewijs is letterlijk. `bewijs` en `kop` komen woordelijk (witruimte
   genormaliseerd) in IR-SPEC voor en noemen de namen die hun herkomstklasse
   belooft; elke v1-naam komt als woord in IR-SPEC voor.
6. Botsingen zijn verklaard: twee v0-namen op een doelnaam (per knoop, per
   property, en binnen een knoop) staan in `botsingen`; die lijst faalt als
   ze verouderd is, in beide richtingen.
"""
import argparse
import collections
import json
import os
import re
import sys

HERKOMST = {"v0name", "vermelding", "uitlijning", "identiteit", "vervalt", "gat"}
KLASSE = {"nl", "en", "neutraal"}

bevindingen = []


def fout(tekst):
    bevindingen.append(tekst)


def lees(pad):
    try:
        with open(pad, encoding="utf-8") as f:
            return f.read()
    except OSError as e:
        print("v1-naamtabel-toets: kan %s niet lezen: %s" % (pad, e), file=sys.stderr)
        sys.exit(2)


def schema_namen(schema):
    """Knoopdiscriminanten en property -> plaatsen, op de B6-telmethode.

    Een plaats is de `node`-const van het object dat de property draagt, of
    bij een genest object zonder discriminant de naam van de $defs-invoer;
    de root heet ROOT.
    """
    knopen = set()
    plaatsen = collections.defaultdict(set)

    def loop(o, ctx):
        if isinstance(o, dict):
            props = o.get("properties")
            if isinstance(props, dict):
                nd = props.get("node", {})
                tag = ctx
                if isinstance(nd, dict):
                    if "const" in nd:
                        knopen.add(nd["const"])
                        tag = nd["const"]
                    for w in nd.get("enum", []):
                        knopen.add(w)
                for k in props:
                    plaatsen[k].add(tag)
            for v in o.values():
                loop(v, ctx)
        elif isinstance(o, list):
            for x in o:
                loop(x, ctx)

    for k in schema.get("properties", {}):
        plaatsen[k].add("ROOT")
    for k, v in schema.get("properties", {}).items():
        loop(v, "ROOT")
    for naam, d in schema.get("$defs", {}).items():
        loop(d, naam)
    return knopen, plaatsen


def norm(t):
    return re.sub(r"\s+", " ", t)


def v0_name_regels(spec):
    """Identifiers per `v0 name:`/`v0 names:`-regel, in documentvolgorde.

    Het venster loopt tot het zinseinde (punt + witruimte) of een volgende
    dubbele punt; alleen kale identifiers tellen (geen `CONSTRUCTS.md`).
    """
    uit = []
    tekst = norm(spec)
    for m in re.finditer(r"v0 names?: ", tekst):
        rest = tekst[m.end():]
        eind = len(rest)
        for pat in (r"\.\s", r"\.$", r":\s"):
            mm = re.search(pat, rest)
            if mm:
                eind = min(eind, mm.start())
        tokens = [t for t in re.findall(r"`([^`]+)`", rest[:eind])
                  if re.fullmatch(r"[A-Za-z][A-Za-z0-9]*", t)]
        uit.append(tokens)
    return uit


def spec_knoopnamen(spec):
    """v1-knoopnamen die IR-SPEC ch. 2-4 als knoop voert, met regelnummer."""
    regels = spec.split("\n")
    try:
        s = next(i for i, l in enumerate(regels) if l.startswith("## 2. The model layer"))
        e = next(i for i, l in enumerate(regels) if l.startswith("## 5. Stratification"))
    except StopIteration:
        print("v1-naamtabel-toets: IR-SPEC mist de kop van ch. 2 of ch. 5", file=sys.stderr)
        sys.exit(2)
    namen = {}
    in_tabel = False
    for i in range(s, e):
        l = regels[i]
        if l.startswith("| Node | Fields |"):
            in_tabel = True
            continue
        if in_tabel:
            if not l.startswith("|"):
                in_tabel = False
            else:
                c = l.split("|")[1].strip()
                if re.fullmatch(r"[A-Z][A-Za-z]+", c):
                    namen.setdefault(c, i + 1)
                continue
        for pat in (r"^(?:A |An |The )?\*\*([A-Z][A-Za-z]+)\*\*",
                    r"\*\*([A-Z][a-z]+(?:[A-Z][a-z]+)+)\*\* \(",
                    r"^\| `([A-Z][A-Za-z]+)` \|",
                    r"^\| Field of `([A-Z][A-Za-z]+)` \|"):
            for m in re.finditer(pat, l):
                namen.setdefault(m.group(1), i + 1)
        m = re.match(r"^### [23]\.\d+ (.*)", l)
        if m:
            titel = re.split(r" — | \(", m.group(1))[0]
            delen = titel.split(" and ")
            if all(re.fullmatch(r"[A-Z][A-Za-z]+", d) for d in delen):
                for d in delen:
                    namen.setdefault(d, i + 1)
    return namen


def doelen(rij):
    for g in rij.get("doel", []):
        v1 = g.get("v1")
        yield g, v1


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--tabel", default="spec/proposals/v1-naamtabel.json")
    ap.add_argument("--wortel", default=".")
    args = ap.parse_args()

    global tabel
    tabel = json.loads(lees(os.path.join(args.wortel, args.tabel)))
    schema = json.loads(lees(os.path.join(args.wortel, tabel["bronnen"]["schema"])))
    spec = lees(os.path.join(args.wortel, tabel["bronnen"]["spec"]))
    spec_n = norm(spec)
    spec_woorden = set(re.findall(r"[A-Za-z][A-Za-z0-9]*", spec))

    s_knopen, s_plaatsen = schema_namen(schema)

    # 1. totaliteit
    for soort, rijen, schemaset in (("knoop", tabel["knopen"], s_knopen),
                                    ("property", tabel["properties"], set(s_plaatsen))):
        teller = collections.Counter(r["v0"] for r in rijen)
        for naam in sorted(schemaset):
            if teller[naam] == 0:
                fout("%s `%s`: staat in het schema maar heeft geen rij in de tabel" % (soort, naam))
        for naam, n in sorted(teller.items()):
            if n > 1:
                fout("%s `%s`: heeft %d rijen, moet er precies een hebben" % (soort, naam, n))
            if naam not in schemaset:
                fout("%s `%s`: heeft een rij maar bestaat niet in het schema" % (soort, naam))

    # 2. rijvorm, bewijs en de verdeling van plaatsen
    def toets_groep(soort, v0, g):
        h = g.get("herkomst")
        v1 = g.get("v1")
        wie = "%s `%s`" % (soort, v0) + (" op %s" % ",".join(g["knopen"]) if "knopen" in g else "")
        if h not in HERKOMST:
            fout("%s: onbekende herkomst %r" % (wie, h))
            return
        if h in ("vervalt", "gat"):
            if v1 is not None:
                fout("%s: herkomst %s draagt geen v1-naam, vond %r" % (wie, h, v1))
        elif not isinstance(v1, str) or not v1:
            fout("%s: herkomst %s eist een v1-naam" % (wie, h))
            return
        if h == "gat" and not g.get("voorstel"):
            fout("%s: een gat draagt een `voorstel`" % wie)
        if h == "identiteit" and v1 != v0:
            fout("%s: identiteit maar v1 is `%s`" % (wie, v1))
        for veld in ("bewijs", "kop"):
            frag = g.get(veld)
            if frag is not None and norm(frag) not in spec_n:
                fout("%s: `%s` staat niet letterlijk in IR-SPEC: %r" % (wie, veld, frag))
        bewijs = g.get("bewijs")
        kop = g.get("kop")
        if h != "identiteit" and h != "gat" and bewijs is None:
            fout("%s: herkomst %s eist een `bewijs`" % (wie, h))
        via = g.get("via")
        if via is not None:
            # een veld dat met zijn drager vervalt: het bewijs noemt de drager
            if h != "vervalt":
                fout("%s: `via` hoort alleen bij herkomst vervalt" % wie)
            elif not any(via == r["v0"] and all(x["herkomst"] == "vervalt" for x in r["doel"])
                         for r in tabel["knopen"]):
                fout("%s: `via` `%s` is geen vervallen knoop" % (wie, via))
        genoemd = via or v0
        if h in ("v0name", "vermelding", "vervalt") and bewijs is not None:
            if genoemd.lower() not in bewijs.lower():
                fout("%s: het bewijs noemt `%s` niet" % (wie, genoemd))
        if h == "v0name" and not re.search(r"v0 names?:", bewijs or ""):
            fout("%s: herkomst v0name maar het bewijs is geen `v0 name:`-regel" % wie)
        if isinstance(v1, str):
            laatste = v1.split(".")[-1]
            if h in ("vermelding", "uitlijning", "v0name"):
                if laatste not in (bewijs or "") and laatste not in (kop or ""):
                    fout("%s: bewijs en kop noemen de v1-naam `%s` niet" % (wie, laatste))
            if h == "identiteit" and bewijs is not None and v1 not in bewijs:
                fout("%s: het bewijs noemt de naam niet" % wie)
            for deel in v1.split("."):
                if deel not in spec_woorden:
                    fout("%s: v1-naam `%s` komt in IR-SPEC niet voor" % (wie, deel))

    for rij in tabel["knopen"]:
        if rij.get("klasse") not in KLASSE:
            fout("knoop `%s`: onbekende klasse %r" % (rij["v0"], rij.get("klasse")))
        if not rij.get("doel"):
            fout("knoop `%s`: geen doel" % rij["v0"])
        for g in rij["doel"]:
            if "knopen" in g:
                fout("knoop `%s`: een knooprij verdeelt geen plaatsen" % rij["v0"])
            toets_groep("knoop", rij["v0"], g)

    for rij in tabel["properties"]:
        v0 = rij["v0"]
        if rij.get("klasse") not in KLASSE:
            fout("property `%s`: onbekende klasse %r" % (v0, rij.get("klasse")))
        gs = rij.get("doel") or []
        if not gs:
            fout("property `%s`: geen doel" % v0)
            continue
        for g in gs:
            toets_groep("property", v0, g)
        if v0 not in s_plaatsen:
            continue
        if len(gs) > 1 or "knopen" in gs[0]:
            gezien = collections.Counter()
            for g in gs:
                if "knopen" not in g:
                    fout("property `%s`: meer doelgroepen, maar een groep noemt geen `knopen`" % v0)
                gezien.update(g.get("knopen", []))
            for plek in sorted(s_plaatsen[v0]):
                if gezien[plek] == 0:
                    fout("property `%s`: plaats %s heeft geen doel" % (v0, plek))
            for plek, n in sorted(gezien.items()):
                if n > 1:
                    fout("property `%s`: plaats %s staat in %d doelgroepen" % (v0, plek, n))
                if plek not in s_plaatsen[v0]:
                    fout("property `%s`: plaats %s draagt deze property niet in het schema" % (v0, plek))

    # 3. de v0 name-regels
    gelezen = v0_name_regels(spec)
    verklaard = tabel["v0_name_regels"]
    if gelezen != verklaard:
        for i in range(max(len(gelezen), len(verklaard))):
            a = gelezen[i] if i < len(gelezen) else None
            b = verklaard[i] if i < len(verklaard) else None
            if a != b:
                fout("`v0 name:`-regel %d: IR-SPEC leest %s, de tabel verantwoordt %s" % (i + 1, a, b))
    rijnamen = {r["v0"]: r for r in tabel["knopen"]}
    propnamen = {r["v0"]: r for r in tabel["properties"]}
    for tokens in gelezen:
        for t in tokens:
            if t in rijnamen:
                hs = {g["herkomst"] for g in rijnamen[t]["doel"]}
                if not hs <= {"v0name", "gat"}:
                    fout("`v0 name:`-token `%s`: de knooprij draagt herkomst %s, niet v0name/gat" % (t, sorted(hs)))
            elif t in propnamen:
                pass
            elif t not in tabel["buiten_schema"]:
                fout("`v0 name:`-token `%s`: geen rij en geen reden in `buiten_schema`" % t)
    for t in tabel["buiten_schema"]:
        if not any(t in tokens for tokens in gelezen):
            fout("`buiten_schema` `%s`: staat op geen `v0 name:`-regel meer (verouderd)" % t)

    # 4. geen spec-knoopnaam die de tabel mist
    doelnamen = {v1 for r in tabel["knopen"] for _, v1 in doelen(r) if v1}
    voorstellen = {g.get("voorstel") for r in tabel["knopen"] for g in r["doel"]}
    sk = spec_knoopnamen(spec)
    for naam, regel in sorted(sk.items(), key=lambda x: x[1]):
        if naam not in doelnamen and naam not in tabel["spec_zonder_v0"]:
            fout("spec-knoop `%s` (IR-SPEC regel %d): geen doel van een knooprij en niet in `spec_zonder_v0`" % (naam, regel))
    for naam in tabel["spec_zonder_v0"]:
        if naam not in sk:
            fout("`spec_zonder_v0` `%s`: de toets vindt hem niet meer als knoop in IR-SPEC ch. 2-4 (verouderd)" % naam)
        if naam in doelnamen:
            fout("`spec_zonder_v0` `%s`: is ook het doel van een knooprij" % naam)

    # 5. botsingen
    def botsing(rijen):
        m = collections.defaultdict(set)
        for r in rijen:
            for _, v1 in doelen(r):
                if v1:
                    m[v1].add(r["v0"])
        return {k: sorted(v) for k, v in sorted(m.items()) if len(v) > 1}

    binnen = collections.defaultdict(lambda: collections.defaultdict(set))
    for r in tabel["properties"]:
        for g in r["doel"]:
            if not g.get("v1"):
                continue
            for plek in g.get("knopen", sorted(s_plaatsen.get(r["v0"], []))):
                binnen[plek][g["v1"]].add(r["v0"])
    b_binnen = {}
    for plek in sorted(binnen):
        for v1, v0s in sorted(binnen[plek].items()):
            if len(v0s) > 1:
                b_binnen["%s.%s" % (plek, v1)] = sorted(v0s)

    berekend = {"knopen": botsing(tabel["knopen"]),
                "properties": botsing(tabel["properties"]),
                "binnen_knoop": b_binnen}
    for soort in ("knopen", "properties", "binnen_knoop"):
        verklaard_b = {k: sorted(v) for k, v in tabel["botsingen"].get(soort, {}).items()}
        for k, v in berekend[soort].items():
            if verklaard_b.get(k) != v:
                fout("botsing (%s) `%s` <- %s staat niet (zo) in `botsingen`" % (soort, k, ", ".join(v)))
        for k in verklaard_b:
            if k not in berekend[soort]:
                fout("`botsingen` (%s) `%s`: geen botsing meer (verouderd)" % (soort, k))

    # rapport
    def tel(rijen):
        klassen = collections.Counter(r["klasse"] for r in rijen)
        herk = collections.Counter(g["herkomst"] for r in rijen for g in r["doel"])
        return klassen, herk

    kk, kh = tel(tabel["knopen"])
    pk, ph = tel(tabel["properties"])
    print("schema: %d knoopdiscriminanten, %d propertynamen" % (len(s_knopen), len(s_plaatsen)))
    print("knopen:     %d rijen; klasse %s; doelgroepen per herkomst %s" % (
        len(tabel["knopen"]), dict(sorted(kk.items())), dict(sorted(kh.items()))))
    print("properties: %d rijen; klasse %s; doelgroepen per herkomst %s" % (
        len(tabel["properties"]), dict(sorted(pk.items())), dict(sorted(ph.items()))))
    print("v0 name-regels in IR-SPEC: %d; spec-knoopnamen ch. 2-4: %d" % (len(gelezen), len(sk)))
    print("botsingen: %d knoop, %d property, %d binnen een knoop" % tuple(
        len(berekend[s]) for s in ("knopen", "properties", "binnen_knoop")))
    if bevindingen:
        for b in bevindingen:
            print("FAIL %s" % b, file=sys.stderr)
        print("v1-naamtabel-toets: %d bevinding(en)" % len(bevindingen), file=sys.stderr)
        return 1
    print("v1-naamtabel-toets: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
