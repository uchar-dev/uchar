#!/usr/bin/env python3
"""Volledigheids- en consistentietoets van de v0->v1-naamtabel (V1-preconditie P10).

Aanroep, vanuit de repowortel:

    python3 spec/proposals/v1-naamtabel-toets.py [--tabel PAD] [--wortel MAP]

--tabel wijst een andere tabel aan (voor mutatieproeven op een kopie);
--wortel is de repowortel waartegen schema en IR-SPEC gelezen worden.
Schema- en spec-pad liggen in deze toets vast; de tabel moet ze gelijk
noemen, zodat hij zich niet tegen een andere bron kan laten toetsen.

rc 0: de tabel spec/proposals/v1-naamtabel.json is totaal en consistent.
rc 1: minstens een bevinding; elke bevinding staat op stderr met de naam erbij.
rc 2: een bron ontbreekt of is onleesbaar.

Wat de toets eist, en waarom:

1. Totaliteit tegen het schema. Elke knoopdiscriminant (`node`-const/enum)
   en elke propertynaam die het schema ergens declareert (sinds M1
   `uchar-ir-v1`), heeft precies een rij; elke rij bestaat in het schema,
   behalve een gelande (7). De telmethode is die van de
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
   belooft: een v1-propertynaam als identifier binnen een backtick-span van
   bewijs of kop, een v1-knoopnaam op woordgrens; elke v1-naam komt als woord
   in IR-SPEC voor. `sectie` moet de sectie zijn waarin het bewijs staat.
   Een `identiteit` op een Nederlands geklasseerde naam faalt, tenzij de rij
   `onzeker` draagt; een `voorstel` buiten een gat mag alleen met `onzeker`.
6. Botsingen en onzekere doelen zijn verklaard: twee v0-namen op een doelnaam (per knoop, per
   property, en binnen een knoop) staan in `botsingen`, en elk doel met
   `onzeker: true` staat in de lijst `onzeker`; beide lijsten falen als ze
   verouderd zijn, in beide richtingen.
7. Gelande namen zijn geschiedenis, geen gat. Een M-stap die een v0-naam van
   de wire haalt, laat zijn rij staan met `geland: "M<n>"` — op de rij als
   de naam overal verdween, op een doelgroep met `knopen` als hij alleen op
   die plaatsen verdween. Voor zo'n rij of groep eist de toets het
   omgekeerde van 1 en 2: de naam (of de plaats) staat NIET meer in het
   schema. Een rij waarvan elke groep `geland` draagt, is als geheel
   geland: de naam verdween stap voor stap van de wire (sinds M8 voor
   namen die al eerder van sommige plaatsen gingen). Vorm en bewijs worden
   gewoon getoetst, zodat de geschiedenis
   letterlijk blijft kloppen met IR-SPEC; botsingen tellen alleen levende
   namen, want een hernoeming raakt een gelande naam niet meer. De nieuwe
   namen die een M-stap schrijft, krijgen hun eigen rij (meestal identiteit).
8. De landingen zijn geteld. Per M-stap noemt `geland_telling` hoeveel
   doelgroepen die stap draagt: een groep telt mee als zij of haar rij
   `geland` draagt (de naam verdween), of als haar noot "geland M<n>" zegt
   (een levende naam die de stap schreef of liet staan). De toets telt zelf
   en eist gelijkheid, in beide richtingen: een weggehaalde of ontmerkte
   landing en een ongetelde nieuwe falen allebei. Een levende gelande groep
   heeft bovendien een doel: herkomst `vervalt` of `gat` faalt, want de naam
   staat op de wire. Zonder dit bleven twee mutaties stil groen (gemeten
   2026-09-23): een gelande rij weghalen, en diagnostics/severity/line terug
   op `vervalt` zetten.
9. De enumwaarden zijn verantwoord (sectie `waarden`, sinds M8). Elke familie
   noemt haar plaatsen in het schema (`Knoop.veld`, een versierecord onder
   zijn schemanaam), haar paren `rijen` [v0, L5] en de paren `geweigerd`
   [v0, L5] die een verifierregel bij naam weigert. Elke `enum` van het
   schema staat op de plaats van precies een familie; op elke plaats is de
   schema-enum (zonder `null`) precies de L5-waarden van `rijen`, plus die
   van `geweigerd` waar de plaats niet in `geweigerd_op` staat; een `null` in
   de enum staat alleen op een plaats uit `null_blijft`, en een familie met
   `vrij` heeft op haar plaats een vrije string en geen enum. Een enum of
   const in een voorwaardelijk deelschema (`if`, `not`) op een familieplaats
   is een deelverzameling van de L5-waarden. De afbeelding is bijectief;
   geen v0-waarde staat nog in een enum of const van het schema; `bewijs`
   staat letterlijk in de genoemde sectie en zijn paren `v0` → `l5` zijn
   precies `rijen` plus `geweigerd` (een identiteitsfamilie heeft geen
   paren); elke L5-waarde staat in backticks in die sectie. De tabel
   `VALUES` van spec/proposals/v1-migratie/m5.py draagt dezelfde families,
   plaatsen en paren, en zijn `FREE` en `NULL_KEPT` zijn de families met
   `vrij` en de plaatsen van `null_blijft`: de migratie en de sectie kunnen
   niet uiteenlopen.

Bekende grenzen — wat de toets NIET waarmaakt, zodat de hernoeming (M8) er
niet meer vertrouwen in legt dan dit:

- Een `uitlijning` is een oordeel van de bouwer. De toets ziet dat het bewijs
  de v1-naam citeert, niet dat het geciteerde tabelveld semantisch het v0-veld
  is. Een verwisseling tussen twee velden die in hetzelfde fragment staan
  (bv. `from`/`to` in een tabelrij) laat hem groen.
- Rijen met `onzeker: true` zijn niet mechanisch uit te voeren: hun doel is
  wat de spec letterlijk zegt of wat de bouwer las, en de `noot` zegt waarom
  dat twijfelachtig is. Een hernoemer MOET ze overslaan of laten beslissen.
- `klasse` (nl / en / neutraal) is een ongetoetste keuze van de bouwer. B6
  legde zijn eigen klassering per naam niet vast, dus de aantallen zijn met
  B6 alleen in totaal (80/193) vergelijkbaar, niet per taal.
- Een property met een doel zonder `knopen` geldt voor alle plaatsen. Krijgt
  het schema een nieuwe knoop met die property, dan erft die plaats stil
  hetzelfde doel; alleen rijen met `knopen` merken een nieuwe plaats op.
"""
import argparse
import collections
import importlib.util
import json
import os
import re
import sys

HERKOMST = {"v0name", "vermelding", "uitlijning", "identiteit", "vervalt", "gat"}
GELAND = re.compile(r"M[1-8]")
KLASSE = {"nl", "en", "neutraal"}
BRONNEN = {"schema": "spec/schema/uchar-ir-v1.schema.json", "spec": "spec/IR-SPEC.md"}

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


def norm_met_regels(spec):
    """norm(spec) plus het bronregelnummer van elk teken daarvan."""
    uit, regels = [], []
    regel, in_wit = 1, False
    for ch in spec:
        if ch.isspace():
            if not in_wit:
                uit.append(" ")
                regels.append(regel)
            in_wit = True
        else:
            uit.append(ch)
            regels.append(regel)
            in_wit = False
        if ch == "\n":
            regel += 1
    return "".join(uit), regels


def secties(spec):
    """Per bronregel de sectie (`2`, `2.2`, `4.3a`) waarin hij staat."""
    uit, huidig = [], ""
    for l in spec.split("\n"):
        m = re.match(r"^##+ (\d+(?:\.\d+[a-z]?)?)\.? ", l)
        if m:
            huidig = m.group(1)
        uit.append(huidig)
    return uit


def backtick_tokens(tekst):
    return {t for span in re.findall(r"`([^`]+)`", tekst or "")
            for t in re.findall(r"[A-Za-z][A-Za-z0-9]*", span)}


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


WAARDE_PAAR = re.compile(r"`([^`]+)` → `([^`]+)`")
M5 = "spec/proposals/v1-migratie/m5.py"


def schema_enums(schema):
    """(plaats, waarden, voorwaardelijk) per `enum` op een property, en per `const` in een
    voorwaardelijk deelschema; de plaats is `node`-const.property of, zonder discriminant, de
    $defs-naam.property. Daarnaast per plaats de property-schema's buiten een voorwaarde."""
    uit, velden = [], collections.defaultdict(list)

    def loop(o, tag, cond):
        if isinstance(o, list):
            for x in o:
                loop(x, tag, cond)
            return
        if not isinstance(o, dict):
            return
        props = o.get("properties")
        if isinstance(props, dict):
            nd = props.get("node")
            if isinstance(nd, dict) and "const" in nd:
                tag = nd["const"]
            for k, v in props.items():
                if k == "node" or not isinstance(v, dict):
                    continue
                plaats = "%s.%s" % (tag, k)
                if not cond:
                    velden[plaats].append(v)
                if isinstance(v.get("enum"), list):
                    uit.append((plaats, v["enum"], cond))
                if cond and "const" in v:
                    uit.append((plaats, [v["const"]], cond))
                if cond and isinstance(v.get("not"), dict) and "const" in v["not"]:
                    uit.append((plaats, [v["not"]["const"]], cond))
        for k, v in o.items():
            if k == "properties" and isinstance(v, dict):
                for pv in v.values():
                    loop(pv, tag, cond)
            else:
                loop(v, tag, cond or k in ("if", "then", "else", "not"))

    for naam, d in schema.get("$defs", {}).items():
        loop(d, naam, False)
    return uit, velden


def spelling(w):
    """De spelling van een waarde zoals IR-SPEC haar in backticks schrijft."""
    return json.dumps(w) if isinstance(w, bool) or w is None else str(w)


def toets_waarden(tabel, schema, spec, sectie_van, spec_n, spec_regel, wortel):
    families = tabel.get("waarden")
    if not isinstance(families, list) or not families:
        fout("`waarden` ontbreekt of is leeg: de enumfamilies van M8 staan in de naamtabel")
        return 0
    enums, velden = schema_enums(schema)
    regels = spec.split("\n")
    per_plaats, gezien, alle_v0 = {}, set(), []
    for f in families:
        naam = f.get("familie")
        if naam in gezien:
            fout("waarden: familie `%s` staat er meer dan eens" % naam)
        gezien.add(naam)
        rijen, geweigerd = f.get("rijen", []), f.get("geweigerd", [])
        paren = [tuple(p) for p in rijen + geweigerd]
        if any(len(p) != 2 for p in paren):
            fout("waarden `%s`: een paar is geen [v0, L5]" % naam)
            continue
        v0s, l5s = [p[0] for p in paren], [p[1] for p in paren]
        for kant, lijst in (("v0", v0s), ("L5", l5s)):
            dubbel = sorted({spelling(x) for x in lijst if lijst.count(x) > 1})
            if dubbel:
                fout("waarden `%s`: de afbeelding is niet bijectief, %s-waarde(n) %s dubbel"
                     % (naam, kant, ", ".join(dubbel)))
        identiteit = all(a == b for a, b in paren)
        alle_v0 += [a for a, b in paren if a != b]
        for pl in f.get("plaatsen", []):
            if pl in per_plaats:
                fout("waarden: plaats %s staat in twee families (`%s`, `%s`)" % (pl, per_plaats[pl], naam))
            per_plaats[pl] = naam
            if f.get("veld") and not pl.endswith("." + f["veld"]):
                fout("waarden `%s`: plaats %s is niet het veld `%s`" % (naam, pl, f["veld"]))
        for pl in f.get("geweigerd_op", []) + f.get("null_blijft", []):
            if pl not in f.get("plaatsen", []):
                fout("waarden `%s`: %s is geen plaats van de familie" % (naam, pl))
        # bewijs: letterlijk in de sectie, en zijn paren zijn de familie
        frag = norm(f.get("bewijs") or "")
        secties_ = set()
        start = spec_n.find(frag) if frag else -1
        while start >= 0:
            secties_.add(sectie_van[spec_regel[start] - 1])
            start = spec_n.find(frag, start + 1)
        if not secties_:
            fout("waarden `%s`: `bewijs` staat niet letterlijk in IR-SPEC: %r" % (naam, f.get("bewijs")))
        elif f.get("sectie") not in secties_:
            fout("waarden `%s`: `sectie` %s, maar het bewijs staat in %s"
                 % (naam, f.get("sectie"), ", ".join(sorted(secties_))))
        genoemd = sorted(WAARDE_PAAR.findall(f.get("bewijs") or ""))
        verwacht = [] if identiteit else sorted((spelling(a), spelling(b)) for a, b in paren)
        if genoemd != verwacht:
            fout("waarden `%s`: het bewijs noemt de paren %s, de familie draagt %s"
                 % (naam, genoemd, verwacht))
        tekst = "\n".join(l for i, l in enumerate(regels) if sectie_van[i] == f.get("sectie"))
        tokens = backtick_tokens(tekst)
        for w in l5s:
            if spelling(w) not in tokens:
                fout("waarden `%s`: de L5-waarde `%s` staat niet in backticks in IR-SPEC %s"
                     % (naam, spelling(w), f.get("sectie")))
        # het schema, per plaats
        vrij = f.get("vrij", False)
        for pl in f.get("plaatsen", []):
            decl = [w for p_, w, c in enums if p_ == pl and not c]
            if vrij:
                if decl or not velden.get(pl) or any(v.get("type") != "string" for v in velden[pl]):
                    fout("waarden `%s`: %s is in het schema geen vrije string" % (naam, pl))
                continue
            if len(decl) != 1:
                fout("waarden `%s`: het schema draagt op %s %d enum(s), verwacht een" % (naam, pl, len(decl)))
                continue
            enum = decl[0]
            if None in enum and pl not in f.get("null_blijft", []):
                fout("waarden `%s`: de enum op %s laat `null` toe, maar de plaats staat niet in "
                     "`null_blijft`" % (naam, pl))
            toe = {spelling(b) for a, b in rijen}
            if pl not in f.get("geweigerd_op", []):
                toe |= {spelling(b) for a, b in geweigerd}
            if {spelling(w) for w in enum if w is not None} != toe:
                fout("waarden `%s`: de enum op %s is %s, de familie verwacht %s"
                     % (naam, pl, sorted(spelling(w) for w in enum if w is not None), sorted(toe)))
    for pl, waarden_, cond in enums:
        if pl not in per_plaats:
            if not cond:
                fout("waarden: de enum op %s hoort bij geen familie" % pl)
            continue
        f = next(x for x in families if x.get("familie") == per_plaats[pl])
        l5 = {spelling(b) for a, b in f.get("rijen", []) + f.get("geweigerd", [])}
        if cond and not {spelling(w) for w in waarden_} <= l5:
            fout("waarden `%s`: een voorwaarde op %s noemt %s, geen L5-waarde van de familie"
                 % (per_plaats[pl], pl, sorted(spelling(w) for w in waarden_)))
    # een v0-string staat nergens meer; een v0-boolean (de jaar-vlag) dekt de plaatsvergelijking
    v0_strings = {w for w in alle_v0 if isinstance(w, str)}
    for pl, waarden_, cond in enums:
        for w in waarden_:
            if isinstance(w, str) and w in v0_strings:
                fout("waarden: de v0-waarde %s staat nog in het schema, op %s" % (spelling(w), pl))
    # de tabel van m5.py: dezelfde families, plaatsen en paren
    pad = os.path.join(wortel, M5)
    try:
        spec_m5 = importlib.util.spec_from_file_location("m5", pad)
        m5 = importlib.util.module_from_spec(spec_m5)
        spec_m5.loader.exec_module(m5)
    except Exception as e:  # een onleesbaar script is een bevinding, geen stille groene toets
        fout("waarden: %s laadt niet: %s" % (M5, e))
        return len(families)
    tabel_m5 = getattr(m5, "VALUES", {})
    for f in families:
        naam = f.get("familie")
        paren = [tuple(p) for p in f.get("rijen", []) + f.get("geweigerd", [])]
        if all(a == b for a, b in paren):
            if naam in tabel_m5:
                fout("waarden `%s`: een identiteitsfamilie, maar m5.py draagt een tabel" % naam)
            continue
        if naam not in tabel_m5:
            fout("waarden `%s`: m5.py draagt de familie niet" % naam)
            continue
        plaatsen_m5, paren_m5 = tabel_m5[naam]
        if sorted(plaatsen_m5) != sorted(f.get("plaatsen", [])):
            fout("waarden `%s`: m5.py noemt de plaatsen %s, de naamtabel %s"
                 % (naam, sorted(plaatsen_m5), sorted(f.get("plaatsen", []))))
        if sorted((spelling(a), b) for a, b in paren_m5.items()) != sorted((spelling(a), b) for a, b in paren):
            fout("waarden `%s`: de paren van m5.py en van de naamtabel verschillen" % naam)
    for naam in tabel_m5:
        if naam not in gezien:
            fout("waarden: m5.py draagt de familie `%s`, de naamtabel niet" % naam)
    if sorted(getattr(m5, "FREE", ())) != sorted(f["familie"] for f in families if f.get("vrij")):
        fout("waarden: FREE van m5.py is niet de verzameling families met `vrij`")
    if sorted(getattr(m5, "NULL_KEPT", ())) != sorted(pl for f in families for pl in f.get("null_blijft", [])):
        fout("waarden: NULL_KEPT van m5.py is niet de verzameling plaatsen van `null_blijft`")
    return len(families)


def rij_geland(rij):
    """De M-stap(pen) waarmee de naam van de wire ging: `geland` op de rij, of elke groep geland."""
    if rij.get("geland"):
        return rij["geland"]
    stappen = [g.get("geland") for g in rij.get("doel", [])]
    if stappen and all(stappen):
        return "/".join(sorted(set(stappen)))
    return None


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
    if tabel.get("bronnen") != BRONNEN:
        fout("`bronnen` in de tabel is %r; de toets leest vast %r" % (tabel.get("bronnen"), BRONNEN))
    schema = json.loads(lees(os.path.join(args.wortel, BRONNEN["schema"])))
    spec = lees(os.path.join(args.wortel, BRONNEN["spec"]))
    spec_n, spec_regel = norm_met_regels(spec)
    assert spec_n == norm(spec)
    sectie_van = secties(spec)
    spec_woorden = set(re.findall(r"[A-Za-z][A-Za-z0-9]*", spec))

    s_knopen, s_plaatsen = schema_namen(schema)

    # 7. de vorm van `geland`, op rij en op groep
    for soort in ("knopen", "properties"):
        for r in tabel[soort]:
            for w in [r.get("geland")] + [g.get("geland") for g in r.get("doel", [])]:
                if w is not None and not (isinstance(w, str) and GELAND.fullmatch(w)):
                    fout("%s `%s`: `geland` is %r, verwacht een M-stap (M1..M8)" % (soort, r["v0"], w))
            if r.get("geland") and any(g.get("geland") for g in r.get("doel", [])):
                fout("%s `%s`: `geland` staat op de rij en op een groep; kies een van de twee" % (soort, r["v0"]))
            for g in r.get("doel", []):
                if g.get("geland") and "knopen" not in g:
                    fout("%s `%s`: een gelande groep noemt zijn `knopen`" % (soort, r["v0"]))

    # 8. de landingen geteld, in beide richtingen
    telling = collections.Counter()
    for soort in ("knopen", "properties"):
        for r in tabel[soort]:
            for g in r.get("doel", []):
                w = g.get("geland") or r.get("geland")
                if not w:
                    m = re.search(r"\bgeland (M[1-8])\b", g.get("noot") or "")
                    if m:
                        w = m.group(1)
                        if g.get("herkomst") in ("vervalt", "gat") or g.get("v1") is None:
                            fout("%s `%s`: de noot zegt geland %s, dus de naam staat op de wire, "
                                 "maar de groep heeft herkomst %r en doel %r"
                                 % (soort, r["v0"], w, g.get("herkomst"), g.get("v1")))
                if isinstance(w, str) and GELAND.fullmatch(w):
                    telling[w] += 1
    verwacht = tabel.get("geland_telling")
    if not isinstance(verwacht, dict):
        fout("`geland_telling` ontbreekt of is geen object {M-stap: aantal doelgroepen}")
    else:
        for w in sorted(set(verwacht) | set(telling)):
            if verwacht.get(w) != telling.get(w):
                fout("`geland_telling` noemt %s doelgroep(en) geland bij %s, de tabel draagt er %d"
                     % (verwacht.get(w), w, telling.get(w, 0)))

    # 1. totaliteit (een gelande rij omgekeerd: hij staat niet meer in het schema)
    for soort, rijen, schemaset in (("knoop", tabel["knopen"], s_knopen),
                                    ("property", tabel["properties"], set(s_plaatsen))):
        teller = collections.Counter(r["v0"] for r in rijen)
        geland_ = {r["v0"]: rij_geland(r) for r in rijen if rij_geland(r)}
        for naam in sorted(schemaset):
            if teller[naam] == 0:
                fout("%s `%s`: staat in het schema maar heeft geen rij in de tabel" % (soort, naam))
            elif naam in geland_:
                fout("%s `%s`: geland bij %s, maar staat nog in het schema" % (soort, naam, geland_[naam]))
        for naam, n in sorted(teller.items()):
            if n > 1:
                fout("%s `%s`: heeft %d rijen, moet er precies een hebben" % (soort, naam, n))
            if naam not in schemaset and naam not in geland_:
                fout("%s `%s`: heeft een rij maar bestaat niet in het schema" % (soort, naam))

    # 2. rijvorm, bewijs en de verdeling van plaatsen
    def toets_groep(soort, v0, g, klasse):
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
        onzeker = g.get("onzeker", False)
        if onzeker not in (True, False):
            fout("%s: `onzeker` is true of afwezig" % wie)
        if onzeker and not g.get("noot"):
            fout("%s: `onzeker` eist een `noot` met de reden" % wie)
        if h == "identiteit" and klasse == "nl" and not onzeker:
            fout("%s: identiteit op een Nederlands geklasseerde naam" % wie)
        if h != "gat" and g.get("voorstel") and not onzeker:
            fout("%s: een `voorstel` buiten een gat mag alleen met `onzeker`" % wie)
        if "sectie" in g:
            if g.get("bewijs") is None:
                fout("%s: `sectie` zonder `bewijs` is ongetoetst" % wie)
            else:
                frag = norm(g["bewijs"])
                gevonden = set()
                start = spec_n.find(frag)
                while start >= 0:
                    gevonden.add(sectie_van[spec_regel[start] - 1])
                    start = spec_n.find(frag, start + 1)
                if gevonden and g["sectie"] not in gevonden:
                    fout("%s: `sectie` %s, maar het bewijs staat in %s" % (
                        wie, g["sectie"], ", ".join(sorted(gevonden))))
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
            samen = (bewijs or "") + " " + (kop or "")
            if h in ("vermelding", "uitlijning", "v0name", "identiteit") and (bewijs or kop):
                if soort == "property":
                    genoemd_v1 = laatste in backtick_tokens(samen)
                else:
                    genoemd_v1 = re.search(r"(?<![A-Za-z0-9])%s(?![A-Za-z0-9])" % re.escape(laatste), samen)
                if not genoemd_v1:
                    fout("%s: bewijs en kop noemen de v1-naam `%s` niet%s" % (
                        wie, laatste, " als identifier in backticks" if soort == "property" else " op woordgrens"))
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
            toets_groep("knoop", rij["v0"], g, rij.get("klasse"))

    for rij in tabel["properties"]:
        v0 = rij["v0"]
        if rij.get("klasse") not in KLASSE:
            fout("property `%s`: onbekende klasse %r" % (v0, rij.get("klasse")))
        gs = rij.get("doel") or []
        if not gs:
            fout("property `%s`: geen doel" % v0)
            continue
        for g in gs:
            toets_groep("property", v0, g, rij.get("klasse"))
        if v0 not in s_plaatsen:
            continue
        if len(gs) > 1 or "knopen" in gs[0]:
            gezien = collections.Counter()
            for g in gs:
                if "knopen" not in g:
                    fout("property `%s`: meer doelgroepen, maar een groep noemt geen `knopen`" % v0)
                elif g.get("geland"):
                    for plek in g["knopen"]:
                        if plek in s_plaatsen[v0]:
                            fout("property `%s`: plaats %s geland bij %s, maar draagt de property nog"
                                 % (v0, plek, g["geland"]))
                    continue
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

    # 5. botsingen, over levende namen: een gelande naam staat niet meer op de wire
    def levend(r, g):
        return not r.get("geland") and not g.get("geland")

    def botsing(rijen):
        m = collections.defaultdict(set)
        for r in rijen:
            for g, v1 in doelen(r):
                if v1 and levend(r, g):
                    m[v1].add(r["v0"])
        return {k: sorted(v) for k, v in sorted(m.items()) if len(v) > 1}

    binnen = collections.defaultdict(lambda: collections.defaultdict(set))
    for r in tabel["properties"]:
        for g in r["doel"]:
            if not g.get("v1") or not levend(r, g):
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

    # onzeker: de markering is verklaard, zodat hij niet stil wegvalt
    berekend_o = sorted("%s@%s" % (r["v0"], "/".join(g["knopen"]) if "knopen" in g else "*")
                        for s_ in ("knopen", "properties") for r in tabel[s_]
                        for g in r["doel"] if g.get("onzeker"))
    verklaard_o = sorted(tabel.get("onzeker", []))
    for k in berekend_o:
        if k not in verklaard_o:
            fout("onzeker doel `%s` staat niet in de lijst `onzeker`" % k)
    for k in verklaard_o:
        if k not in berekend_o:
            fout("`onzeker` `%s`: het doel draagt de markering niet (meer)" % k)

    # 9. de enumwaarden
    n_families = toets_waarden(tabel, schema, spec, sectie_van, spec_n, spec_regel, args.wortel)

    # rapport
    def tel(rijen):
        klassen = collections.Counter(r["klasse"] for r in rijen)
        herk = collections.Counter(g["herkomst"] for r in rijen for g in r["doel"])
        return klassen, herk

    plaatsen = collections.Counter()
    for r in tabel["properties"]:
        for g in r["doel"]:
            plaatsen[g["herkomst"]] += len(g.get("knopen", s_plaatsen.get(r["v0"], [])))
    onzeker_n = sum(1 for s_ in ("knopen", "properties") for r in tabel[s_] for g in r["doel"] if g.get("onzeker"))
    kk, kh = tel(tabel["knopen"])
    pk, ph = tel(tabel["properties"])
    print("schema: %d knoopdiscriminanten, %d propertynamen" % (len(s_knopen), len(s_plaatsen)))
    print("knopen:     %d rijen; klasse %s; doelgroepen per herkomst %s" % (
        len(tabel["knopen"]), dict(sorted(kk.items())), dict(sorted(kh.items()))))
    print("properties: %d rijen; klasse %s; doelgroepen per herkomst %s" % (
        len(tabel["properties"]), dict(sorted(pk.items())), dict(sorted(ph.items()))))
    print("propertyplaatsen per herkomst %s; onzeker gemarkeerde doelgroepen: %d" % (
        dict(sorted(plaatsen.items())), onzeker_n))
    print("v0 name-regels in IR-SPEC: %d; spec-knoopnamen ch. 2-4: %d" % (len(gelezen), len(sk)))
    print("botsingen: %d knoop, %d property, %d binnen een knoop" % tuple(
        len(berekend[s]) for s in ("knopen", "properties", "binnen_knoop")))
    gelande = collections.Counter(
        w for s_ in ("knopen", "properties") for r in tabel[s_]
        for w in [r.get("geland")] + [g.get("geland") for g in r["doel"]] if w)
    print("geland (rijen en groepen per M-stap): %s; geland_telling (doelgroepen): %s"
          % (dict(sorted(gelande.items())), dict(sorted(telling.items()))))
    print("waarden: %d enumfamilie(s)" % n_families)
    if bevindingen:
        for b in bevindingen:
            print("FAIL %s" % b, file=sys.stderr)
        print("v1-naamtabel-toets: %d bevinding(en)" % len(bevindingen), file=sys.stderr)
        return 1
    print("v1-naamtabel-toets: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
