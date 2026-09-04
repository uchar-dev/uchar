#!/bin/sh
# The reference-type derivation harness (RUNTIME-SEMANTICS ch. 2.7, "the consumer clause").
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
# NOT twisted, each because the annotation IS the source there (ch. 2.7's exceptions):
#   - a Literal: its type is the lexeme's own carrier ("3,5 EUR"), a copy of nothing. All three
#     executors read it, and must.
#   - the DURATION operand of a date +/- duration Binair: ref (eval.ml) and the ASP translator
#     both route the calendar unit off that operand's static type (WF-035). Twisting it would
#     measure that routing, not a declaration reading.
#   - a TIJDSDUUR / ABSOLUTE_TIJDSDUUR Functie's own type: its unit is the OPERATOR's parameter
#     ("de tijdsduur van A tot B in jaren", WF-038), not a derived result type.
#   - every node kind with no unit-bearing annotation in the corpus (Fold, Match, Construct and
#     the rest of the v1 nodes): nothing to graft. NB the ASP translator does read Fold's and
#     Match's own type — for its integer SCALE (value_scale, S19) — so those two would need the
#     same reason-carrying treatment the moment a unit-bearing witness appears.
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
trap 'rm -rf "$TWISTER" "$WORK" "$LIST" "$CASES"' EXIT

cat > "$TWISTER" <<'PY'
import json, os, sys
from decimal import Decimal

# the position classes and their restatement-factor exponents (see the script header)
KLASSE = {"AttribuutRef": 1, "ParameterRef": 2, "DimensieCel": 3, "BinderRef": 4,
          "Binair": 5, "Let": 6, "Functie": 7, "Afgerond": 8, "Begrensd": 9}
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


def loop(x, treffers, sla_eigen_over=False):
    if isinstance(x, list):
        for v in x:
            loop(v, treffers)
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
    if node in KLASSE and not sla_eigen_over \
            and not (node == "Functie" and x.get("kind") in GEEN_FUNCTIE):
        nieuw, hoe = verdraai(x.get("type"), KLASSE[node])
        if hoe:
            x["type"] = nieuw
            treffers.append(hoe)
    for k, v in list(x.items()):
        if k == "type":        # a DECLARATION never sits under an expression's own type
            continue
        loop(v, treffers, sla_eigen_over=(v is duur))


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


bron, doel = sys.argv[1], sys.argv[2]
with open(bron, encoding="utf-8") as fh:
    doc = json.load(fh, parse_int=RuweNum, parse_float=RuweNum)
treffers = []
loop(doc, treffers)
map_ = os.path.dirname(doel)
if map_:
    os.makedirs(map_, exist_ok=True)
with open(doel, "w", encoding="utf-8") as fh:
    for c in stukken(doc):
        fh.write(c)
print(len(treffers))
PY

vermeld() {
    for s in $SURVIVORS; do
        [ "${s%%|*}" = "$1" ] && [ "$(echo "$s" | cut -d'|' -f2)" = "$2" ] && return 0
    done
    return 1
}

status=0
n_goldens=0
n_twists=0
n_gecontroleerd=0
n_survivors=0
find corpus/golden -name ir.json 2>/dev/null | sort > "$LIST"
while IFS= read -r ir; do
    dir=$(dirname "$ir")
    naam=${dir#corpus/golden/}
    tw="$WORK/$dir/ir.json"
    treffers=$(python3 "$TWISTER" "$ir" "$tw") || { echo "FAIL: the twister could not read $ir" >&2; exit 2; }
    [ "$treffers" -gt 0 ] || continue          # no unit-bearing annotation: nothing to probe here
    find "$dir" -maxdepth 1 -name 'case*.json' 2>/dev/null | sort > "$CASES"
    [ -s "$CASES" ] || continue                # no case file: not replayable (gate 5 lists those)
    n_goldens=$((n_goldens + 1))
    n_twists=$((n_twists + treffers))

    # ref and clingo: exit 0 on the untouched ir.json is what gates 5 and 6 pin, so the twisted
    # run must exit 0 too. Any other outcome is the executor reading the repetition.
    while IFS= read -r case; do
        n_gecontroleerd=$((n_gecontroleerd + 2))
        if ! uit=$("$RUNNER_REF" "$tw" "$case" 2>&1); then
            if vermeld "$naam" ref; then n_survivors=$((n_survivors + 1)); else
                printf 'FAIL: %s — ref answers differently on the lying copy:\n%s\n' "$case" "$uit" >&2
                status=1
            fi
        fi
        if ! uit=$("$RUNNER_CLINGO" "$tw" "$case" 2>&1); then
            if vermeld "$naam" clingo; then n_survivors=$((n_survivors + 1)); else
                printf 'FAIL: %s — clingo answers differently on the lying copy:\n%s\n' "$case" "$uit" >&2
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
        if vermeld "$naam" drools; then n_survivors=$((n_survivors + 1)); else
            printf 'FAIL: %s — Drools answers differently on the lying copy:\n  untouched: %s\n  lying:     %s\n' \
                "$naam" "$basis" "$verdraaid" >&2
            status=1
        fi
    fi
done < "$LIST"

for s in $SURVIVORS; do
    d="corpus/golden/${s%%|*}"
    if [ ! -d "$d" ]; then
        echo "FAIL: stale survivor entry — $d does not exist" >&2
        status=1
    fi
done

if [ "$n_twists" -eq 0 ]; then
    echo 'FAIL: no annotation was twisted — the harness probed nothing (an empty twist set is never green)' >&2
    exit 1
fi

if [ "$status" -eq 0 ]; then
    printf 'check-reftype-derivation: PASS (%s unit annotations twisted over %s goldens; %s executor replays reproduce the untouched outcome; %s pinned survivors)\n' \
        "$n_twists" "$n_goldens" "$n_gecontroleerd" "$n_survivors"
fi
exit "$status"
