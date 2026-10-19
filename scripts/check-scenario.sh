#!/usr/bin/env bash
# Gate 16 — the scenario gate: the scenariotoetsen extension of the RegelSpraak frontend
# (frontends/regelspraak/grammar/DIVERGENCES.md, "Scenariotoetsen"): a scenario
# is a case file written in RegelSpraak, and its lowering must reproduce the
# pinned corpus.
#
# For every golden that carries a scenario.rgs beside its source.rgs, the
# frontend's --cases mode reads the two as one test document and must
#   1. write exactly the golden's pinned case files — the same file names, none
#      missing and none extra — each byte-identical to the pinned file: the
#      handwritten expectations are the anchor, never the lowering;
#   2. write the golden's ir.json byte-identically: without a scope that is the
#      production route's IR document, with one the forms the scope carries,
#      and both must be the pinned document. The one scoped golden
#      (jutterswet-rubriektabel-rgs) names every form of its document, so this
#      gate reads a scope but sees none leave a form out: a scope that does
#      writes an ir.json other than the pinned one, which no golden pins. The
#      leaving out is witnessed by the frontend suite (ScenarioLoweringTest).
# Gate 4 stays the schema gate of the same case files and gates 5, 6 and 12
# their replay; this gate adds only that a RegelSpraak scenario document
# regenerates them.
#
# An empty scenario-golden set is a FAILURE, never silently green, and a
# missing JDK fails loudly.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-scenario: FAIL — $*" >&2; exit 1; }

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — a scenario document deleted or a golden that
# quietly regenerates fewer cases — while growth needs no bump. A deliberate removal lowers the floor
# in the same commit. Accepted weakness, recorded rather than hidden: a count that stays equal while
# its content shifted passes; the per-file comparison is what covers that direction.
EXPECTED_MIN_SCENARIO_GOLDENS=7   # 2026-09-27: measured 7 (corpus/golden/*/scenario.rgs; 6 on 2026-09-26, +1 the parameter set)
EXPECTED_MIN_SCENARIO_CASES=31    # 2026-09-27: measured 31 case files regenerated over those 7 (29 on 2026-09-26)

command -v java  >/dev/null 2>&1 || fail "java not found (the frontend needs JDK 21+)"
command -v javac >/dev/null 2>&1 || fail "javac not found (the frontend needs JDK 21+, not a bare JRE)"
command -v make  >/dev/null 2>&1 || fail "make not found"

FE=frontends/regelspraak
[ -d "$FE" ] || fail "$FE is missing"
build_out=$(make -C "$FE" compile 2>&1) || { echo "$build_out" | tail -25 >&2; fail "the frontend does not build"; }
JAR=$(ls "$FE"/lib/antlr-*-complete.jar 2>/dev/null | head -1)
[ -n "$JAR" ] || fail "no ANTLR jar under $FE/lib after the build"

goldens=0; cases=0; bad=0
while IFS= read -r scen; do
  d=$(dirname "$scen"); n=${d#corpus/golden/}
  [ -f "$d/source.rgs" ] || fail "$n carries scenario.rgs but no source.rgs"
  [ -f "$d/ir.json" ] || fail "$n carries scenario.rgs but no ir.json"
  goldens=$((goldens + 1))
  tmp=$(mktemp -d -t scenario-gate-XXXXXX)
  if ! out=$(java -cp "$JAR:$FE/out" dev.uchar.regelspraak.resolver.Resolver \
      --ir "$tmp/ir.json" --cases "$tmp/cases" "$d/source.rgs" "$d/scenario.rgs" 2>&1); then
    echo "LOWER FAIL: $n (the frontend refuses its own scenario document)"
    echo "$out" | sed 's/^/    /'
    bad=$((bad + 1)); rm -rf "$tmp"; continue
  fi
  made=$(cd "$tmp/cases" && ls case*.json 2>/dev/null | sort)
  pinned=$(cd "$d" && ls case*.json 2>/dev/null | sort)
  if [ "$made" != "$pinned" ]; then
    echo "SET DIFF: $n (lowered: $(echo $made); pinned: $(echo $pinned))"
    bad=$((bad + 1))
  fi
  for f in $made; do
    [ -f "$d/$f" ] || continue
    if cmp -s "$tmp/cases/$f" "$d/$f"; then
      cases=$((cases + 1))
    else
      echo "DIFF: $n/$f (the lowering diverges from the pinned case file)"
      diff "$d/$f" "$tmp/cases/$f" | head -10 | sed 's/^/    /'
      bad=$((bad + 1))
    fi
  done
  if ! cmp -s "$tmp/ir.json" "$d/ir.json"; then
    echo "DIFF: $n/ir.json (the scenario route's IR document diverges from the pinned one)"
    bad=$((bad + 1))
  fi
  rm -rf "$tmp"
done < <(find corpus/golden -name scenario.rgs | sort)

[ "$goldens" -gt 0 ] || fail "no scenario goldens found under corpus/golden (an empty set is a failure)"
[ "$bad" -eq 0 ] || fail "$bad divergence(s) over $goldens scenario golden(s)"
[ "$goldens" -ge "$EXPECTED_MIN_SCENARIO_GOLDENS" ] \
  || fail "$goldens scenario golden(s), but EXPECTED_MIN_SCENARIO_GOLDENS pins at least $EXPECTED_MIN_SCENARIO_GOLDENS — a scenario document went missing (a deliberate removal lowers the floor in the same commit)"
[ "$cases" -ge "$EXPECTED_MIN_SCENARIO_CASES" ] \
  || fail "$cases case file(s) regenerated, but EXPECTED_MIN_SCENARIO_CASES pins at least $EXPECTED_MIN_SCENARIO_CASES — the regenerated corpus shrank (a deliberate removal lowers the floor in the same commit)"
echo "check-scenario: PASS — $goldens scenario golden(s) (floor $EXPECTED_MIN_SCENARIO_GOLDENS) regenerate $cases case file(s) (floor $EXPECTED_MIN_SCENARIO_CASES) and their ir.json byte-identically"
