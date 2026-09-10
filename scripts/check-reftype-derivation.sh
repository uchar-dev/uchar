#!/bin/sh
# The reference-type derivation harness (RUNTIME-SEMANTICS ch. 2.10, "the consumer clause").
#
# A v0 IR node repeats its type on the wire at positions where that type is a COPY of a
# declaration — an AttribuutRef, a ParameterRef, a DimensieCel read, a binder read — or a
# DERIVATION over its operands — an arithmetic node, a Let, a value aggregation. WF-091 makes
# the copy mechanically true for the reference positions; nothing crosses the derived ones.
# The clause says a consumer must read the SOURCE, not the repetition. This script is what
# holds all three executors to it:
#
#   1. every corpus/golden/**/ir.json is copied into a temporary directory with a systematic
#      LIE grafted onto every twistable type annotation (below);
#   2. ref, clingo and BOTH Drools transpiler branches — the exec-model back-end and the DRL text
#      compiled to a KieBase — replay the golden's ORIGINAL case files against the LYING copy;
#   3. each executor must produce EXACTLY the outcome it produces on the untouched ir.json —
#      the same pinned values, or the same per-form refusal. A twist that changes an answer is
#      an executor reading the repetition instead of the declaration, and the gate goes red
#      with the diverging executor and golden named.
#   4. the goldens that legitimately survive with a DIFFERENT outcome are pinned in the
#      explicit survivor list below, each with the reason its outcome may move. A survivor
#      outside the list is a failure; a listed survivor that no longer diverges, or no longer
#      exists, is a stale entry and also a failure (the check-noop pattern).
#   5. an empty twist set is a failure, never silently green.
#   6. every unit-bearing annotation the twister MEETS must sit either in the twist table below
#      or in the explicit, reason-carrying exception set (where the annotation IS the source).
#      Table and clamp key on the POSITION CLASS, which for a Binair is (node, op) and for a
#      Functie is (node, kind) — ch. 2.10 splits those two node kinds by OPERATOR, not by node.
#      PLUS/MIN/VERMINDERD_MET/MAAL and the six value aggregations derive their result unit;
#      GEDEELD_DOOR, TOT_DE_MACHT, PERCENTAGE_VAN, an absolute value, a root and the date/time
#      extractors are ch. 2.10's FOURTH class, where v0 derives nothing. Keying on the node alone
#      would twist a unit-bearing GEDEELD_DOOR as though it sat at a derived position, and then
#      report whatever came out as a consumer defect — a wrong diagnosis where the honest answer
#      is "v0 states no join rule here" (or, when nothing observable moved, silence where the
#      fourth-class witness should have been announced). A position in neither set is one this
#      harness cannot probe, so nothing holds a consumer to the declaration there — the gate
#      fails with the class and the golden named, and says WHICH of the two arrivals it is.
#      That clamp is what keeps a new wire form, and the first fourth-class witness, from
#      entering the corpus with its consumer side silently unmeasured; measured 2026-09-04 it
#      costs nothing (zero of either).
#
# --- what a PASS means, and what it does not ------------------------------------------------
# This harness measures INDIFFERENCE: with every declaration held byte-identical, no executor's
# answer moves when the repetition lies. It does NOT measure NECESSITY — it cannot separate a
# consumer that derives from one that ignores units altogether, because both are indifferent to
# a unit lie. Necessity is pinned elsewhere and must stay there: gates 5 and 6 hold ref and
# clingo to the corpus's ABSOLUTE hand-written expectations, and gate 12 holds the Drools
# backend to exactly those same values, so an executor that dropped unit restatement is red
# there on the UNTOUCHED corpus long before it is green here. Indifference is the half no other
# gate can see, precisely because a lying document is one no other gate may hold.
#
# With ONE exception, and this script carries it: the DRL-TEXT branch has no value gate at all —
# gate 12 replays the exec-model path only. Comparing that branch against its own untouched status
# line would therefore be satisfied by a branch that had stopped restating units altogether, whose
# baseline is simply wrong in both runs. So its BASELINE is pinned here too, for exactly the goldens
# this harness probes (the DRL_BASIS_UIT list below).
#
# Four twist classes are, for BOTH DROOLS legs, a no-op by construction: the Java IR records for
# ParameterRef, DimensieCel, Afgerond and Begrensd DERIVE their type() (from the parameter's
# declaration, from the inner reference, from the basis) and hold no wire annotation at all, so the
# graft cannot reach either of them — it still probes ref and clingo, which do read the wire there.
# The Drools side of those four rests on the EffectieveEenheidWitnessTest witnesses, which build the
# nodes directly. Said out loud so a green line is not read as coverage it has not got.
#
# And the same honesty about HOW MUCH of the two Drools legs a green line covers. On the UNTOUCHED
# corpus the derivation never disagrees with the annotation — WF-091 makes the repetition faithful,
# so effType(e) equals e.type() at every call (measured 2026-09-04 with a StackWalker probe on
# EffectieveEenheid.van, attributing each call to its transpiler frame: zero divergences over every
# replayable golden, in BOTH branches). On the LYING copies the same probe shows the derivation
# overriding the annotation at a handful of call sites, and those are the whole of what this harness
# holds each branch to (re-measured 2026-09-04 over the 11 twistable replayable goldens):
#   exec-model branch — 5 sites
#     - the WRITE boundary (a consequence value restated into its doel's declared unit) — 20 hits;
#     - the ADDITIVE OPERAND boundary (the rhs of a +/- restated into the lhs unit) — 14 hits;
#     - the AGGREGATION restatement (a fold's per-element and total unit into the doel's) — 2 hits;
#     - the ATTRIBUTE-vs-ATTRIBUTE comparison (§5.1 Tabel 32 operand restatement) — 2 hits;
#     - the ATTRIBUTE-vs-PARAMETER comparison (the same Tabel 32 restatement against a global) — 1 hit.
#   DRL-text branch — 3 sites
#     - the WRITE boundary (regel, omrekenWaarde over value and doel) — 20 hits;
#     - the ADDITIVE OPERAND boundary (binairTerm) — 10 hits;
#     - the §5.1 Tabel 32 comparison operand restatement (conditie) — 4 hits.
# Every other effType call in either file is a pass-through on this corpus, so its derivation is
# STATED but not measured here. For the DRL branch, measured TRANSPILE-ONLY over every golden the
# loader accepts (118; a replay-based sweep misses the no-case goldens): 12 of its 26 converted call
# sites are REACHED at all, 3 of those ever override. The OTHER 23 rest on the backend's JUnit
# witnesses (gate 12's obligation), not on a green line here, and they are two different facts:
# 9 ARE reached but on this corpus always UNITLESS — the temporal §4.1/§5.1.4 rescalings, the Begrensd
# value arm, the §8.2 timeline comparison and its geheelPeriodeCall sibling (reached only through a
# no-case golden), the two concatenation-aggregation sites — while the remaining 14 no golden
# reaches at all. A leg that stops PLAYING entirely fails the floor below; a leg that merely shrinks
# stays green and only the printed count moves.
#
# THE BEGRENSD ITEM OF THAT LIST LAPSED ON 2026-09-11 and is corrected rather than left to rot. The
# corpus gained jutterswet-vangstklem, its first UNIT-BEARING clamp (a `t` bound over a `kg` base, one
# declared system, factor 1000), so DrlTranspiler's clamp arm —
# `omrekenWaarde(waarde(bound), effType(bound), effType(basis))` — now restates where it used to no-op.
# Measured on that golden and not read off the code: the untouched document PLAYS in the DRL-text
# branch, and relabelling its minimum bound `t` -> `kg` in the IR moves the DRL-text answer from
# 1000,00 to 250,00. So the always-UNITLESS group is one smaller and that arm rests on a golden now,
# not on the JUnit witnesses alone. The four counts of the paragraph above (12 reached, 3 ever
# overriding, 9 unitless, 14 unreached) are NOT restated here: they came from a StackWalker probe run
# on 2026-09-04 over an 11-golden twistable set, and which bucket the clamp arm moved INTO — an
# overriding site, or a reached-but-non-overriding one — was not re-probed. Read them as that dated
# measurement, with this correction on top, and re-probe before quoting them as current.
#
# The DRL leg is NOT a repetition of the exec one. It reaches a golden the exec branch cannot play
# at all (jutterswet-vondsttarief, which the exec transpiler SKIPs as "waarde buiten scope" while the
# text branch lowers and plays it), and its three live sites are different code. Measured by mutation
# on 2026-09-04 — EffectieveEenheid.van() put back on the node's own annotation, the state before the
# derivation existed — the DRL leg alone goes red on NINE of the eleven twistable goldens, including
# that one. The two that stay green under the mutant are the two the text branch markers (the ch. 4.10
# Let: lichterloon, vondstrecht), which is the honest reading of a branch that does not lower them.
#
# The verifier does NOT run here. This harness asks an EXECUTION question — is the outcome a
# function of the declarations? — outside the validity question, exactly as gate 7's --noop
# mode asks its question outside evaluation. The lying copies are deliberately documents the
# verifier would reject under WF-091; that is what makes them a probe.
#
# --- the twist ------------------------------------------------------------------------------
# Facet: the UNIT, and only the unit. Measured (2026-09-04), and the reason each other facet is
# out:
#   - SOORT: a soort lie is a different document — ref routes its ch. 1.2d empty-equality and
#     its date arithmetic on the static soort, so twisting it would measure ref's routing, not
#     a declaration reading. WF-078/WF-091 cross it in the verifier instead.
#   - PRECISION: the ASP translator sizes its integer encoding off the annotation's decimals
#     (value_scale in backends/clingo/lib/translate.ml), so a precision lie makes clingo
#     overflow or refuse rather than answer — measured: 21 of 402 cases. And it would probe
#     nothing here: the Drools back-ends never read Datatype.decimalen or Datatype.geheel at
#     all, only Afgerond.decimalen, which is a NODE field and not a type annotation.
# So the unit is both the facet all three consumers can be indifferent to and the only one this
# backend reads. The lie: every twistable annotation carrying a unit with a canonical and a
# factor gets a SISTER unit — same canonical, abbreviation suffixed, factorNaarCanoniek
# multiplied by a power of ten fixed PER POSITION CLASS. Same canonical keeps the units
# inter-convertible, so a consumer that trusts the annotation happily restates by the wrong
# factor (a loud wrong number) instead of refusing; distinct per-class factors keep two
# annotations from cancelling each other at a restatement boundary. Nothing else in the
# document moves — every DECLARATION (an objecttype's attributen, a parameter's datatype, an
# eenheidstelsel) is left byte-identical, which is precisely what makes the derived answer the
# right answer.
#
# Twisted positions (the class factor in brackets):
#   reference positions — AttribuutRef [10] (bare, navigated, and the one a rule names as its
#   doel), ParameterRef [100], DimensieCel [1000], BinderRef [10000];
#   composite positions — Binair/{PLUS,MIN,VERMINDERD_MET,MAAL} [1e5], Let [1e6],
#   Functie/{SOM,TOTAAL,MINIMUM,MAXIMUM,EERSTE,LAATSTE} [1e7], Afgerond [1e8], Begrensd [1e9]
#   (the ch. 4.10 Let/BinderRef nodes reach this backend as carriers, and their wire annotation
#   is what the carrier holds).
# The factor stays per NODE KIND even though membership is per (node, op): a restatement boundary
# sits between two node kinds, never between two operators of one kind, so splitting the factor as
# well would buy nothing and would move the twist set the corpus already pins.
#
# NOT twisted, each because the annotation IS the source there (ch. 2.10's named exceptions), and
# each therefore EXEMPT from the clamp of step 6:
#   - a Literal: its type is the lexeme's own carrier ("3,5 EUR"), a copy of nothing. All three
#     executors read it, and must.
#   - the DURATION operand of a date +/- duration Binair: ref (eval.ml) and the ASP translator
#     both route the calendar unit off that operand's static type (WF-035). Twisting it would
#     measure that routing, not a declaration reading.
#   - a TIJDSDUUR / ABSOLUTE_TIJDSDUUR Functie's own type: its unit is the OPERATOR's parameter
#     ("de tijdsduur van A tot B in jaren", WF-038), not a derived result type.
#
# Neither twisted nor exempt is NOT silently skipped any more (step 6), and the clamp separates
# two different arrivals:
#   - a ch. 2.10 FOURTH-CLASS branch of a node kind the table otherwise carries — a unit-bearing
#     Binair/GEDEELD_DOOR, /TOT_DE_MACHT, /PERCENTAGE_VAN, or a Functie absolute value, root or
#     date/time extractor. v0 derives no result unit there, so a twist would probe a rule that
#     does not exist; the gate reports the witness instead. What that witness must buy is a
#     derivation rule (or a fence) — never a wider licence to read the wire.
#   - a node kind outside the table altogether: every v1 node the corpus does not yet carry with a
#     unit (Fold, Match, Construct, ...) trips on its first unit-bearing witness, and must then be
#     given a twist class or an exception with its reason. NB the ASP translator already reads
#     Fold's and Match's own type for its integer SCALE (value_scale, ASP-TRANSLATION.md S2/S4) —
#     that is the reading the clamp will surface first.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

# --- the survivor list (reason-carrying; fails when stale) -----------------------------------
# Entries are "<golden-dir>|<executor>|<reason>". EMPTY today: measured 2026-09-04, all three
# executors are indifferent over the whole twistable set. An entry here is an admission that one
# executor's answer depends on a repetition rather than a declaration, and must say why that is
# the right reading — not merely that it is the current one.
SURVIVORS=''

# --- the DRL-branch baseline list (reason-carrying; fails when stale) ------------------------
# A twistable golden's DRL-TEXT baseline must be PLAYED (the branch ran and matched the corpus's
# ABSOLUTE expectations — the same ones gates 5, 6 and 12 pin) or EXCLUDE (the branch does not carry
# the form, and says which). Anything else is a value the corpus does not pin anywhere, and must be
# listed here with the reason. Entries are "<golden-dir>|<reason>"; an entry whose golden is gone, or
# whose baseline is back in line, is stale and fails. EMPTY today, measured 2026-09-04: of the 11
# twistable replayable goldens the text branch PLAYS 9 and markers 2 (the ch. 4.10 Let, which it does
# not lower). NB this pins the branch on the twistable subset only — over the WHOLE corpus it has 7
# value divergences and 2 fire-time errors, which is gate-12 territory and deliberately not re-fought
# here.
DRL_BASIS_UIT=''

# --- the DRL-text branch and ch. 5.10: the divergence class is EMPTY (measured 2026-09-07) ---
# This gate owns the DRL-TEXT branch, so it records the branch's relationship to
# RUNTIME-SEMANTICS ch. 5.10. Until 2026-09-07 that relationship was a divergence, and this file
# carried a watchlist over every TEN_HOOGSTE carrier in the corpus.
#
# THE MAINTAINER DECISION OF 2026-09-07 CLOSED IT. Including at-most's TRUE trigger (`at most d`
# stops once h + r <= d, not only once h > d) made ch. 5.10 exactly the dual of at-least — which is
# what the text branch computes: it renders `at most d` as `!(minstens(members, d+1))`, and
# at-least's own two triggers map one-to-one onto at-most's two sides. The branch had been
# implementing the dual all along; the norm now says the same thing.
#
# REMEASURED over the whole read-set model, both directions, all five quantifiers, every threshold
# (including d > n) and every truth assignment, n = 1..7 — 6646 combinations:
#     verdict divergences vs ch. 5.10 ................. 0
#     executor reads a member ch. 5.10 skips .......... 0   (would be a fault the norm does not have)
#     executor skips a member ch. 5.10 reads .......... 0   (would be an answer where the norm faults)
# for the DRL-text branch AND for clingo. Confirmed end-to-end on the very probe that used to be
# the smoking gun (TEN HOOGSTE 1 over one member that divides by zero): ref, clingo, the exec-model
# branch and the DRL-text branch now all write 1 — they agree, where three of them used to fault
# and the fourth answered.
#
# So there is no shape left to watch, and the watchlist that stood here is gone rather than left to
# rot. The live check on this branch is the DRL_BASIS_UIT baseline above, which still requires the
# text branch to PLAY or EXCLUDE per twistable golden.
#
# WHAT "0 divergences" DOES AND DOES NOT BUY: the reservations against reading this as a proof — and
# what a second, independent model did and did not confirm — are recorded with the measurement in
# backlog/done/20260901-samengesteld-kortsluiting.md (card Results, the decision round). They live
# there rather than here so they survive the follow-up card being withdrawn.

RUNNER_REF=_build/default/ref/bin/runner.exe
RUNNER_CLINGO=_build/default/backends/clingo/bin/clingo_runner.exe

if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
command -v dune >/dev/null 2>&1 || {
    echo 'FAIL: need dune (OCaml). Install via opam: opam install dune yojson zarith' >&2; exit 2; }
command -v clingo >/dev/null 2>&1 || { echo 'FAIL: need clingo on PATH' >&2; exit 2; }
command -v java >/dev/null 2>&1 || { echo 'FAIL: need a JDK/JRE 21+' >&2; exit 2; }
command -v mvn >/dev/null 2>&1 || { echo 'FAIL: need mvn (the Drools backend jar is rebuilt here)' >&2; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo 'FAIL: need python3 (the twister)' >&2; exit 2; }

if ! dune build ref backends 2>&1; then
    echo 'check-reftype-derivation: FAIL (ref/clingo build)' >&2
    exit 1
fi
# Rebuild the jar so the harness can never probe a jar that predates the source under review
# (the gate-12 lesson). The backend's own JUnit suite is gate 12's obligation, not this one's,
# so it is skipped here — this harness asks only whether the OUTCOME follows the declarations.
if ! mvn -q -o -f backends/drools/pom.xml package -DskipTests >/dev/null 2>&1; then
    echo 'check-reftype-derivation: FAIL (Drools backend build)' >&2
    exit 1
fi
JAR=$(ls backends/drools/target/uchar-drools-backend-*-shaded.jar 2>/dev/null | head -1)
[ -n "$JAR" ] || { echo 'FAIL: the build produced no shaded jar' >&2; exit 1; }

TWISTER=$(mktemp) || exit 2
WORK=$(mktemp -d) || exit 2
LIST=$(mktemp) || exit 2
CASES=$(mktemp) || exit 2
ONGEDEKT=$(mktemp) || exit 2   # "<golden> <node>" per unit-bearing annotation outside table + exceptions
RAKERS=$(mktemp) || exit 2     # "<golden>|<executor>" per survivor entry that ACTUALLY diverged
DRLRAKERS=$(mktemp) || exit 2  # "<golden>" per DRL-baseline entry whose baseline is ACTUALLY off
trap 'rm -rf "$TWISTER" "$WORK" "$LIST" "$CASES" "$ONGEDEKT" "$RAKERS" "$DRLRAKERS"' EXIT

cat > "$TWISTER" <<'PY'
import json, os, sys
from decimal import Decimal

# the position classes and their restatement-factor exponents (see the script header). The exponent
# is per NODE KIND; MEMBERSHIP is per position class, which for Binair/Functie is finer (below).
KLASSE = {"AttribuutRef": 1, "ParameterRef": 2, "DimensieCel": 3, "BinderRef": 4,
          "Binair": 5, "Let": 6, "Functie": 7, "Afgerond": 8, "Begrensd": 9}
# ch. 2.10 clause 2 splits Binair and Functie by OPERATOR, not by node: only these branches DERIVE a
# result unit (an additive result carries the left unit / a unitless side adopts the other; a MAAL
# cancels or forms the opaque product; §4.6 Eenheid1 hands a value aggregation its first operand's).
# Every OTHER op/kind of those two node kinds is ch. 2.10's FOURTH class — v0 derives nothing there,
# so it is neither twistable nor exempt, and the clamp reports it.
AFGELEIDE_OP = {"PLUS", "MIN", "VERMINDERD_MET", "MAAL"}
AFGELEIDE_KIND = {"SOM", "TOTAAL", "MINIMUM", "MAXIMUM", "EERSTE", "LAATSTE"}
# The reason-carrying exception set of the step-6 clamp: node kinds OUTSIDE the twist table whose
# annotation IS the source, so leaving them unprobed is a statement and not a gap.
UITZONDERING = {"Literal": "the lexeme's own carrier (\"3,5 EUR\") is a copy of no declaration"}
# WF-038: a tijdsduur's own unit is the operator's parameter, not a derived result type
GEEN_FUNCTIE = {"TIJDSDUUR", "ABSOLUTE_TIJDSDUUR"}
DATUM = ("DatumInDagen", "DatumTijd")
ACHTERVOEGSEL = "-verdraaid"


def positieklasse(x):
    """The key table and clamp agree on: (node, op) for a Binair, (node, kind) for a Functie, the
       bare node kind otherwise. Printed as the clamp's witness, so a report names the BRANCH."""
    node = x.get("node")
    if node == "Binair":
        return "Binair/%s" % x.get("op")
    if node == "Functie":
        return "Functie/%s" % x.get("kind")
    return node


def afgeleid(x):
    """True iff this position's result unit is one ch. 2.10 clause 2 DERIVES — the only positions a
       twist may probe, because a twist asks whether a consumer follows a rule that exists."""
    node = x.get("node")
    if node == "Binair":
        return x.get("op") in AFGELEIDE_OP
    if node == "Functie":
        return x.get("kind") in AFGELEIDE_KIND
    return node in KLASSE


class RuweNum:
    """A JSON number with its original lexeme, so untouched numbers re-emit unchanged."""
    __slots__ = ("raw",)

    def __init__(self, s):
        self.raw = s


def getal(v):
    return Decimal(v.raw) if isinstance(v, RuweNum) else (None if v is None else Decimal(str(v)))


def zusje(e, exp):
    """A convertible SISTER of unit e: same canonical, distinct abbreviation, factor x 10^exp."""
    if not isinstance(e, dict):
        return None
    a, c, f = e.get("afkorting"), e.get("canoniek"), e.get("factorNaarCanoniek")
    if a is None or c is None or f is None or ACHTERVOEGSEL in a:
        return None
    uit = dict(e)
    uit["afkorting"] = a + ACHTERVOEGSEL + str(exp)
    uit["factorNaarCanoniek"] = RuweNum(str(getal(f) * (10 ** exp)))
    return uit


def verdraai(dt, exp):
    if not isinstance(dt, dict) or dt.get("node") not in ("Numeriek", "Percentage"):
        return dt, None
    u = zusje(dt.get("eenheid"), exp)
    if u is None:
        return dt, None
    uit = dict(dt)
    uit["eenheid"] = u
    return uit, "%s %s->%s" % (dt.get("node"), dt["eenheid"].get("afkorting"), u["afkorting"])


def datum_getypeerd(e):
    return isinstance(e, dict) and isinstance(e.get("type"), dict) and e["type"].get("node") in DATUM


def eenheiddragend(dt):
    return isinstance(dt, dict) and dt.get("node") in ("Numeriek", "Percentage") \
        and dt.get("eenheid") is not None


def loop(x, telling, sla_eigen_over=False):
    if isinstance(x, list):
        for v in x:
            loop(v, telling)
        return
    if not isinstance(x, dict):
        return
    node = x.get("node")
    # WF-035: the duration operand of a date +/- duration carries the calendar unit ref routes on
    duur = None
    if node == "Binair" and x.get("op") in ("PLUS", "MIN", "VERMINDERD_MET"):
        l, r = x.get("links"), x.get("rechts")
        if datum_getypeerd(l) and not datum_getypeerd(r):
            duur = r
        elif datum_getypeerd(r) and not datum_getypeerd(l):
            duur = l
    vrijgesteld = sla_eigen_over or node in UITZONDERING \
        or (node == "Functie" and x.get("kind") in GEEN_FUNCTIE)
    if node is not None and eenheiddragend(x.get("type")):
        # step 6, the clamp: account for EVERY unit-bearing annotation, so a position that is
        # neither twisted nor exempt cannot pass unnoticed. The two arrivals are reported apart:
        # a FOURTH-CLASS branch of a carried node kind (v0 derives no result unit there) and a
        # node kind the table does not carry at all (a v1 form reaching the corpus).
        telling["eenheid"] += 1
        if vrijgesteld:
            telling["vrijgesteld"] += 1
        elif not afgeleid(x):
            telling["vierde" if node in KLASSE else "ongedekt"].append(positieklasse(x))
        elif zusje(x["type"].get("eenheid"), KLASSE[node]) is None:
            # in the table, but no convertible SISTER exists (a unit with no canonical/factor —
            # a unit system declares no conversion for it). A non-convertible lie would measure
            # refusal routing, not a declaration reading, so it is counted and not twisted.
            telling["zonderzusje"] += 1
    if afgeleid(x) and not vrijgesteld:
        nieuw, hoe = verdraai(x.get("type"), KLASSE[node])
        if hoe:
            x["type"] = nieuw
            telling["treffers"].append(hoe)
    for k, v in list(x.items()):
        if k == "type":        # a DECLARATION never sits under an expression's own type
            continue
        loop(v, telling, sla_eigen_over=(v is duur))


def stukken(o):
    if isinstance(o, RuweNum):
        yield o.raw
    elif o is None:
        yield "null"
    elif o is True:
        yield "true"
    elif o is False:
        yield "false"
    elif isinstance(o, str):
        yield json.dumps(o, ensure_ascii=False)
    elif isinstance(o, dict):
        yield "{"
        eerste = True
        for k, v in o.items():
            if not eerste:
                yield ","
            eerste = False
            yield json.dumps(k, ensure_ascii=False)
            yield ":"
            for c in stukken(v):
                yield c
        yield "}"
    elif isinstance(o, list):
        yield "["
        eerste = True
        for v in o:
            if not eerste:
                yield ","
            eerste = False
            for c in stukken(v):
                yield c
        yield "]"
    else:
        raise TypeError(repr(o))


bron, doel, naam = sys.argv[1], sys.argv[2], sys.argv[3]
with open(bron, encoding="utf-8") as fh:
    doc = json.load(fh, parse_int=RuweNum, parse_float=RuweNum)
telling = {"treffers": [], "ongedekt": [], "vierde": [], "eenheid": 0, "vrijgesteld": 0,
           "zonderzusje": 0}
loop(doc, telling)
map_ = os.path.dirname(doel)
if map_:
    os.makedirs(map_, exist_ok=True)
with open(doel, "w", encoding="utf-8") as fh:
    for c in stukken(doc):
        fh.write(c)
# line 1: the four counters the shell sums; then one TAGGED line per uncovered occurrence (the
# clamp), the tag separating ch. 2.10's fourth class from a node kind the table does not carry.
print("%d %d %d %d" % (len(telling["treffers"]), telling["eenheid"],
                       telling["vrijgesteld"], telling["zonderzusje"]))
for klasse in telling["vierde"]:
    print("vierde %s %s" % (naam, klasse))
for klasse in telling["ongedekt"]:
    print("onbekend %s %s" % (naam, klasse))
PY

# Is <golden> x <executor> a pinned survivor? Read LINE by line, never word by word: a
# reason carries spaces, so splitting $SURVIVORS on whitespace would shred every entry the
# moment the list stops being empty.
vermeld() {
    treffer=$(printf '%s\n' "$SURVIVORS" | while IFS= read -r s; do
        [ -n "$s" ] || continue
        [ "${s%%|*}" = "$1" ] || continue
        rest=${s#*|}
        [ "${rest%%|*}" = "$2" ] && echo hit
    done)
    [ -n "$treffer" ]
}

# The same read for the DRL baseline list, which is keyed on the golden alone.
basisvrij() {
    treffer=$(printf '%s\n' "$DRL_BASIS_UIT" | while IFS= read -r s; do
        [ -n "$s" ] || continue
        [ "${s%%|*}" = "$1" ] && echo hit
    done)
    [ -n "$treffer" ]
}

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — the harness that quietly probes half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# Both counts are this harness's discriminating power, not a file count: the twist total is what
# the twist TABLE meets in the corpus, so a narrowed table would shrink it with every corpus file
# still in place, and no other gate would see it. The DRL floor extends the "-eq 0" bottom below,
# whose own comment records the measured mutant that dropped the leg from 9 played goldens to 0:
# that mutant would today be caught at 8, not only at 0.
EXPECTED_MIN_TWISTS=74      # 2026-09-10: measured 74 annotations twisted over 15 goldens
EXPECTED_MIN_DRL_PLAYED=13  # 2026-09-10: measured 13 goldens PLAYED by the DRL-text branch

status=0
n_goldens=0
n_twists=0
n_gecontroleerd=0
n_survivors=0
n_drl=0
n_eenheid=0
n_vrijgesteld=0
n_zonderzusje=0
find corpus/golden -name ir.json 2>/dev/null | sort > "$LIST"
while IFS= read -r ir; do
    dir=$(dirname "$ir")
    naam=${dir#corpus/golden/}
    tw="$WORK/$dir/ir.json"
    uit=$(python3 "$TWISTER" "$ir" "$tw" "$naam") || { echo "FAIL: the twister could not read $ir" >&2; exit 2; }
    kop=$(printf '%s\n' "$uit" | head -1)
    # the clamp's accounting runs over EVERY golden, replayable or not: an unprobed node kind is a
    # gap wherever it sits.
    staart=$(printf '%s\n' "$uit" | tail -n +2)
    [ -z "$staart" ] || printf '%s\n' "$staart" >> "$ONGEDEKT"
    set -- $kop
    treffers=$1
    n_eenheid=$((n_eenheid + $2))
    n_vrijgesteld=$((n_vrijgesteld + $3))
    n_zonderzusje=$((n_zonderzusje + $4))
    [ "$treffers" -gt 0 ] || continue          # no twistable annotation: nothing to probe here
    find "$dir" -maxdepth 1 -name 'case*.json' 2>/dev/null | sort > "$CASES"
    [ -s "$CASES" ] || continue                # no case file: not replayable (gate 5 lists those)
    n_goldens=$((n_goldens + 1))
    n_twists=$((n_twists + treffers))

    # ref and clingo: exit 0 on the untouched ir.json is what gates 5 and 6 pin, so the twisted
    # run must exit 0 too. Any other outcome is the executor reading the repetition.
    while IFS= read -r case; do
        n_gecontroleerd=$((n_gecontroleerd + 2))
        if ! rapport=$("$RUNNER_REF" "$tw" "$case" 2>&1); then
            if vermeld "$naam" ref; then
                n_survivors=$((n_survivors + 1)); echo "$naam|ref" >> "$RAKERS"
            else
                printf 'FAIL: %s — ref answers differently on the lying copy:\n%s\n' "$case" "$rapport" >&2
                status=1
            fi
        fi
        if ! rapport=$("$RUNNER_CLINGO" "$tw" "$case" 2>&1); then
            if vermeld "$naam" clingo; then
                n_survivors=$((n_survivors + 1)); echo "$naam|clingo" >> "$RAKERS"
            else
                printf 'FAIL: %s — clingo answers differently on the lying copy:\n%s\n' "$case" "$rapport" >&2
                status=1
            fi
        fi
    done < "$CASES"

    # Drools: compare its own status line on the lying copy with the one it prints on the
    # untouched file. Comparing against the BASELINE rather than against PLAYED is deliberate —
    # a golden gate 12 auto-refuses per form, or carries in its value-divergence list, must stay
    # exactly as refusing/diverging under the twist, and this harness must not re-litigate that.
    set --
    while IFS= read -r case; do set -- "$@" "$case"; done < "$CASES"
    n_gecontroleerd=$((n_gecontroleerd + 1))
    basis=$(java -jar "$JAR" "$ir" "$@" 2>/dev/null | tail -1)
    verdraaid=$(java -jar "$JAR" "$tw" "$@" 2>/dev/null | tail -1)
    if [ "$basis" != "$verdraaid" ]; then
        if vermeld "$naam" drools; then
            n_survivors=$((n_survivors + 1)); echo "$naam|drools" >> "$RAKERS"
        else
            printf 'FAIL: %s — Drools answers differently on the lying copy:\n  untouched: %s\n  lying:     %s\n' \
                "$naam" "$basis" "$verdraaid" >&2
            status=1
        fi
    fi

    # The SECOND Drools transpiler branch, through the same twist: DrlTranspiler emits DRL TEXT, which the
    # runner's --drl entry compiles to a KieBase (the classic DRL build) and replays against the SAME pinned
    # expectations. Same baseline discipline as the exec leg — its OWN untwisted status line, never PLAYED —
    # and for the same reason, twice over: the text branch is NARROWER than the exec one (it has no ch. 7
    # prioritizedDefaults lowering, and it markers constructs the exec branch lowers, the ch. 4.10 Let among
    # them), and on a handful of goldens it also answers differently. Neither is this harness's question. What
    # IS its question is whether the answer it does give follows the declarations, and for that a refusal that
    # stays exactly as refusing is a perfectly good observation.
    n_gecontroleerd=$((n_gecontroleerd + 1))
    dbasis=$(java -jar "$JAR" --drl "$ir" "$@" 2>/dev/null | tail -1)
    dverdraaid=$(java -jar "$JAR" --drl "$tw" "$@" 2>/dev/null | tail -1)
    dkop=${dbasis%% *}
    if [ "$dkop" = PLAYED ]; then
        n_drl=$((n_drl + 1))
    elif [ "$dkop" != EXCLUDE ]; then
        # the baseline pin: nothing else holds this branch to the corpus's absolute values
        if basisvrij "$naam"; then
            echo "$naam" >> "$DRLRAKERS"
        else
            printf 'FAIL: %s — the DRL-text branch neither played nor refused the UNTOUCHED golden, so its\n      value is pinned by nothing:\n  %s\n' \
                "$naam" "$dbasis" >&2
            status=1
        fi
    fi
    if [ "$dbasis" != "$dverdraaid" ]; then
        if vermeld "$naam" drl; then
            n_survivors=$((n_survivors + 1)); echo "$naam|drl" >> "$RAKERS"
        else
            printf 'FAIL: %s — the DRL-text branch answers differently on the lying copy:\n  untouched: %s\n  lying:     %s\n' \
                "$naam" "$dbasis" "$dverdraaid" >&2
            status=1
        fi
    fi
done < "$LIST"

# The survivor list, BOTH halves (the check-noop pattern): an entry whose golden is gone is stale,
# and so is an entry that no longer diverges — it then admits an exception nothing needs any more,
# which is exactly how a list stops describing the system it pins.
weg=$(printf '%s\n' "$SURVIVORS" | while IFS= read -r s; do
    [ -n "$s" ] || continue
    g=${s%%|*}
    rest=${s#*|}
    ex=${rest%%|*}
    # (no `case` here: /bin/sh on macOS is bash 3.2, whose parser cannot read one inside $(...))
    if [ "$ex" != ref ] && [ "$ex" != clingo ] && [ "$ex" != drools ] && [ "$ex" != drl ]; then
        echo "  $g|$ex — unknown executor (expected ref, clingo, drools or drl)"
    elif [ ! -d "corpus/golden/$g" ]; then
        echo "  $g|$ex — no such golden"
    elif ! grep -qxF "$g|$ex" "$RAKERS" 2>/dev/null; then
        echo "  $g|$ex — no longer diverges under the twist; drop the entry"
    fi
done)
if [ -n "$weg" ]; then
    printf 'FAIL: stale survivor entry:\n%s\n' "$weg" >&2
    status=1
fi

# The DRL baseline list, the same both halves.
drlweg=$(printf '%s\n' "$DRL_BASIS_UIT" | while IFS= read -r s; do
    [ -n "$s" ] || continue
    g=${s%%|*}
    if [ ! -d "corpus/golden/$g" ]; then
        echo "  $g — no such golden"
    elif ! grep -qxF "$g" "$DRLRAKERS" 2>/dev/null; then
        echo "  $g — the DRL-text branch plays or refuses it again; drop the entry"
    fi
done)
if [ -n "$drlweg" ]; then
    printf 'FAIL: stale DRL-baseline entry:\n%s\n' "$drlweg" >&2
    status=1
fi

if [ "$n_twists" -eq 0 ]; then
    echo 'FAIL: no annotation was twisted — the harness probed nothing (an empty twist set is never green)' >&2
    exit 1
fi
if [ "$n_twists" -lt "$EXPECTED_MIN_TWISTS" ]; then
    echo "FAIL: $n_twists annotation(s) twisted, but EXPECTED_MIN_TWISTS pins at least $EXPECTED_MIN_TWISTS — the probe shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

# The same bottom for the DRL leg, and it is NOT implied by the baseline clamp above. That clamp demands
# PLAYED or EXCLUDE per golden, so a text branch that REFUSED EVERY golden would satisfy it for all of
# them, be indifferent by construction (both runs refuse identically), and leave the leg silently green
# while measuring nothing at all. Measured 2026-09-04 with exactly that mutant — a refusal at the front of
# CaseRunner's DRL branch: the leg fell from 9 played goldens to 0 and the gate still said PASS. NB the
# count is the STATUS HEAD, so a golden whose every case file carried expectedFault would replay nothing
# and still print "PLAYED 0", counting here — the corpus zone discipline gate 4 pins (golden carries
# expected, rood carries expectedFault) makes that shape unreachable today, and this bottom must not come
# to lean on it.
if [ "$n_drl" -eq 0 ]; then
    echo 'FAIL: the DRL-text branch PLAYED no golden — the leg measured nothing (an empty played set is never green)' >&2
    exit 1
fi
if [ "$n_drl" -lt "$EXPECTED_MIN_DRL_PLAYED" ]; then
    echo "FAIL: the DRL-text branch PLAYED $n_drl golden(s), but EXPECTED_MIN_DRL_PLAYED pins at least $EXPECTED_MIN_DRL_PLAYED — the leg shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

n_vierde=$(awk '$1 == "vierde" {n++} END {print n+0}' "$ONGEDEKT")
n_onbekend=$(awk '$1 == "onbekend" {n++} END {print n+0}' "$ONGEDEKT")
n_ongedekt=$((n_vierde + n_onbekend))
if [ "$n_vierde" -gt 0 ]; then
    echo 'FAIL: a unit-bearing annotation sits at a node kind whose result unit v0 does not derive —' >&2
    echo '      the §2.10 fourth-class witness has arrived. The node kind is one the twist table carries,' >&2
    echo '      but THIS operator/kind branch has no join rule (GEDEELD_DOOR, TOT_DE_MACHT,' >&2
    echo '      PERCENTAGE_VAN, an absolute value, a root, a date/time extractor), so a twist here would' >&2
    echo '      probe a rule that does not exist and report its absence as a consumer defect. What the' >&2
    echo '      witness must buy is a derivation rule (or a fence) — never a wider licence to read the' >&2
    echo '      wire. ref/clingo and the Drools backend answer such a position DIFFERENTLY today (ch.' >&2
    echo '      2.10), so the divergence is now observable and must be settled:' >&2
    awk '$1 == "vierde" {print "  " $2 " " $3}' "$ONGEDEKT" | sort | uniq -c >&2
    status=1
fi
if [ "$n_onbekend" -gt 0 ]; then
    echo 'FAIL: a unit-bearing annotation sits at a node kind the twist table does not carry, and that' >&2
    echo '      the exception set does not exempt — this harness probes nothing there, so no consumer is' >&2
    echo '      held to the declaration at that position. Give the kind a twist class (with its own' >&2
    echo '      factor exponent) or an exception with the reason its annotation IS the source:' >&2
    awk '$1 == "onbekend" {print "  " $2 " " $3}' "$ONGEDEKT" | sort | uniq -c >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    printf '  NOTE (ch. 5.10): the DRL-TEXT branch no longer diverges from ch. 5.10. The maintainer\n'
    printf '       decision of 2026-09-07 added at-most'"'"'s TRUE trigger, which makes the norm the exact dual\n'
    printf '       of at-least — and that dual is what the branch already computed. Remeasured over the full\n'
    printf '       read-set model (n = 1..7, 6646 combinations, both directions, all five quantifiers):\n'
    printf '       0 verdict divergences, 0 extra reads, 0 skipped reads, for the text branch and for clingo.\n'
    printf '       The former watchlist is retired; see the comment above.\n'
    printf 'check-reftype-derivation: PASS (%s unit annotations twisted over %s goldens, floor %s; %s executor replays reproduce the untouched outcome, %s of them PLAYED by the DRL-text branch, floor %s; %s pinned survivors)\n' \
        "$n_twists" "$n_goldens" "$EXPECTED_MIN_TWISTS" "$n_gecontroleerd" "$n_drl" \
        "$EXPECTED_MIN_DRL_PLAYED" "$n_survivors"
    printf '  corpus-wide accounting: %s unit-bearing annotations = %s twistable + %s exempt (the annotation IS the source) + %s without a convertible sister + %s at a §2.10 fourth-class branch (v0 derives no result unit) + %s outside the twist table\n' \
        "$n_eenheid" "$((n_eenheid - n_vrijgesteld - n_zonderzusje - n_ongedekt))" \
        "$n_vrijgesteld" "$n_zonderzusje" "$n_vierde" "$n_onbekend"
fi
exit "$status"
