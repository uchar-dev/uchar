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
#   2. ref, clingo and the Drools backend replay the golden's ORIGINAL case files against the
#      LYING copy;
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
#      A node kind outside both is a position this harness cannot probe, so nothing holds a
#      consumer to the declaration there — the gate fails with the kind and the golden named.
#      That clamp is what keeps a NEW wire form from entering the corpus with its consumer side
#      silently unmeasured; measured 2026-09-04 it costs nothing (zero such kinds).
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
# Four twist classes are, for the DROOLS leg specifically, a no-op by construction: the Java IR
# records for ParameterRef, DimensieCel, Afgerond and Begrensd DERIVE their type() (from the
# parameter's declaration, from the inner reference, from the basis) and hold no wire annotation
# at all, so the graft cannot reach that leg — it still probes ref and clingo, which do read the
# wire there. The Drools side of those four rests on the EffectieveEenheidWitnessTest witnesses,
# which build the nodes directly. Said out loud so a green line is not read as coverage it has
# not got.
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
#   composite positions — Binair [1e5], Let [1e6], Functie [1e7], Afgerond [1e8], Begrensd
#   [1e9] (the ch. 4.10 Let/BinderRef nodes reach this backend as carriers, and their wire
#   annotation is what the carrier holds).
#
# NOT twisted, each because the annotation IS the source there (ch. 2.10's exceptions), and each
# therefore EXEMPT from the clamp of step 6:
#   - a Literal: its type is the lexeme's own carrier ("3,5 EUR"), a copy of nothing. All three
#     executors read it, and must.
#   - the DURATION operand of a date +/- duration Binair: ref (eval.ml) and the ASP translator
#     both route the calendar unit off that operand's static type (WF-035). Twisting it would
#     measure that routing, not a declaration reading.
#   - a TIJDSDUUR / ABSOLUTE_TIJDSDUUR Functie's own type: its unit is the OPERATOR's parameter
#     ("de tijdsduur van A tot B in jaren", WF-038), not a derived result type.
# A node kind that is neither twisted nor exempt is NOT silently skipped any more (step 6): every
# v1 node the corpus does not yet carry with a unit (Fold, Match, Construct, ...) trips the clamp
# on its first unit-bearing witness, and must then be given a twist class or an exception with its
# reason. NB the ASP translator already reads Fold's and Match's own type for its integer SCALE
# (value_scale, S19) — that is the reading the clamp will surface first.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

# --- the survivor list (reason-carrying; fails when stale) -----------------------------------
# Entries are "<golden-dir>|<executor>|<reason>". EMPTY today: measured 2026-09-04, all three
# executors are indifferent over the whole twistable set. An entry here is an admission that one
# executor's answer depends on a repetition rather than a declaration, and must say why that is
# the right reading — not merely that it is the current one.
SURVIVORS=''

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
trap 'rm -rf "$TWISTER" "$WORK" "$LIST" "$CASES" "$ONGEDEKT" "$RAKERS"' EXIT

cat > "$TWISTER" <<'PY'
import json, os, sys
from decimal import Decimal

# the position classes and their restatement-factor exponents (see the script header)
KLASSE = {"AttribuutRef": 1, "ParameterRef": 2, "DimensieCel": 3, "BinderRef": 4,
          "Binair": 5, "Let": 6, "Functie": 7, "Afgerond": 8, "Begrensd": 9}
# The reason-carrying exception set of the step-6 clamp: node kinds OUTSIDE the twist table whose
# annotation IS the source, so leaving them unprobed is a statement and not a gap.
UITZONDERING = {"Literal": "the lexeme's own carrier (\"3,5 EUR\") is a copy of no declaration"}
# WF-038: a tijdsduur's own unit is the operator's parameter, not a derived result type
GEEN_FUNCTIE = {"TIJDSDUUR", "ABSOLUTE_TIJDSDUUR"}
DATUM = ("DatumInDagen", "DatumTijd")
ACHTERVOEGSEL = "-verdraaid"


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
        # step 6, the clamp: account for EVERY unit-bearing annotation, so a kind that is neither
        # twisted nor exempt cannot pass unnoticed.
        telling["eenheid"] += 1
        if vrijgesteld:
            telling["vrijgesteld"] += 1
        elif node not in KLASSE:
            telling["ongedekt"].append(node)
        elif zusje(x["type"].get("eenheid"), KLASSE[node]) is None:
            # in the table, but no convertible SISTER exists (a unit with no canonical/factor —
            # a unit system declares no conversion for it). A non-convertible lie would measure
            # refusal routing, not a declaration reading, so it is counted and not twisted.
            telling["zonderzusje"] += 1
    if node in KLASSE and not vrijgesteld:
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
telling = {"treffers": [], "ongedekt": [], "eenheid": 0, "vrijgesteld": 0, "zonderzusje": 0}
loop(doc, telling)
map_ = os.path.dirname(doel)
if map_:
    os.makedirs(map_, exist_ok=True)
with open(doel, "w", encoding="utf-8") as fh:
    for c in stukken(doc):
        fh.write(c)
# line 1: the four counters the shell sums; then one line per UNCOVERED occurrence (the clamp)
print("%d %d %d %d" % (len(telling["treffers"]), telling["eenheid"],
                       telling["vrijgesteld"], telling["zonderzusje"]))
for node in telling["ongedekt"]:
    print("%s %s" % (naam, node))
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

status=0
n_goldens=0
n_twists=0
n_gecontroleerd=0
n_survivors=0
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
    if [ "$ex" != ref ] && [ "$ex" != clingo ] && [ "$ex" != drools ]; then
        echo "  $g|$ex — unknown executor (expected ref, clingo or drools)"
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

if [ "$n_twists" -eq 0 ]; then
    echo 'FAIL: no annotation was twisted — the harness probed nothing (an empty twist set is never green)' >&2
    exit 1
fi

n_ongedekt=$(awk 'END {print NR}' "$ONGEDEKT")
if [ "$n_ongedekt" -gt 0 ]; then
    echo 'FAIL: a unit-bearing annotation sits at a node kind the twist table does not carry, and that' >&2
    echo '      the exception set does not exempt — this harness probes nothing there, so no consumer is' >&2
    echo '      held to the declaration at that position. Give the kind a twist class (with its own' >&2
    echo '      factor exponent) or an exception with the reason its annotation IS the source:' >&2
    sort "$ONGEDEKT" | uniq -c >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    printf 'check-reftype-derivation: PASS (%s unit annotations twisted over %s goldens; %s executor replays reproduce the untouched outcome; %s pinned survivors)\n' \
        "$n_twists" "$n_goldens" "$n_gecontroleerd" "$n_survivors"
    printf '  corpus-wide accounting: %s unit-bearing annotations = %s twistable + %s exempt (the annotation IS the source) + %s without a convertible sister; %s outside the twist table\n' \
        "$n_eenheid" "$((n_eenheid - n_vrijgesteld - n_zonderzusje - n_ongedekt))" \
        "$n_vrijgesteld" "$n_zonderzusje" "$n_ongedekt"
fi
exit "$status"
