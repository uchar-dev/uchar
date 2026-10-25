#!/bin/sh
# Gate 5: the oracle — the reference interpreter (ref/) replays every
# corpus case (PLAN section 8: "an interpreter replay of each corpus case
# must pass").
#
# Four checks:
#   1. ref/ builds (the OCaml workspace, scripts/lib/ocaml-build.sh, which
#      also builds the ir_v0 loader it links against);
#   2. ref/'s OWN test suite (ref/test/test_ref.ml) runs green with a
#      non-zero, floored example count — see below;
#   3. every corpus/golden/**/case*.json replays exactly: the runner loads
#      the sibling ir.json and the case file, evaluates to the fixpoint,
#      and compares every expectation exactly (exit 0); any mismatch or
#      load error turns the gate red with the runner's report;
#   4. goldens that have no case file yet are listed explicitly — they are
#      not replayed and must never look green. An empty case set is a
#      failure, never silently green.
#
# With explicit arguments (ir.json case.json) it replays just that pair.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

if [ ! -d ref ]; then
    echo 'FAIL: ref/ not found' >&2
    exit 2
fi

# Locate dune; fall back to the opam environment when it is not on PATH.
if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
if ! command -v dune >/dev/null 2>&1; then
    echo 'FAIL: need dune (OCaml). Install via opam: opam install dune yojson zarith' >&2
    exit 2
fi

# check 1: build (the OCaml workspace: ref/ plus the ir_v0 library).
. scripts/lib/ocaml-build.sh
if ! ensure_ocaml; then
    echo 'check-oracle: FAIL (build)' >&2
    exit 1
fi
BIN=_build/default/ref/bin/runner.exe

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — the suite that quietly runs half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
EXPECTED_MIN_EXAMPLES=489   # 2026-09-10: measured 489 (ref/test/test_ref.ml worked examples)
EXPECTED_MIN_CASES=427      # 2026-09-10: measured 427 (corpus/golden/**/case*.json)

if [ "$#" -eq 2 ]; then
    exec "$BIN" "$1" "$2"
fi

# check 2: ref/'s own test suite. Until 2026-09-10 NO gate ran it, while it carries the worked
# examples RUNTIME-SEMANTICS.md owes each of its clauses (CLAUDE.md, Spec discipline) — spec
# artifacts whose executor was unguarded. It is run here, in the gate that already owns ref/ and
# already builds it, exactly the way gate 6 runs the clingo backend's own suite: run it, demand a
# count line, floor the count. The workspace build above already produces the exe.
# It sits BELOW the two-argument branch on purpose: that form replays one pair for a person
# debugging it, and making that pay for the whole suite is a cost with no gate value.
SUITE_BIN=_build/default/ref/test/test_ref.exe
[ -x "$SUITE_BIN" ] || { echo "FAIL: ref suite binary missing at $SUITE_BIN after the build" >&2; exit 1; }
if ! SUITE_OUT=$("$SUITE_BIN" 2>&1); then
    echo "$SUITE_OUT" >&2
    echo 'check-oracle: FAIL (ref test suite)' >&2
    exit 1
fi
SUITE_N=$(printf '%s' "$SUITE_OUT" | sed -n 's/^test_ref: all \([0-9][0-9]*\) worked examples pass$/\1/p' | tail -1)
if [ -z "$SUITE_N" ]; then
    echo "$SUITE_OUT" >&2
    echo 'FAIL: the ref suite printed no "all N worked examples pass" line — it did not run (a silent green is not green)' >&2
    exit 1
fi
if [ "$SUITE_N" -lt "$EXPECTED_MIN_EXAMPLES" ]; then
    echo "FAIL: the ref suite ran $SUITE_N worked examples, but EXPECTED_MIN_EXAMPLES pins at least $EXPECTED_MIN_EXAMPLES — the suite shrank (a deliberately dropped example lowers the floor in the same commit)" >&2
    exit 1
fi

status=0
list=$(mktemp) || exit 2
trap 'rm -f "$list"' EXIT

# check 3: every case replays exactly.
n_cases=0
find corpus/golden -name 'case*.json' 2>/dev/null | sort > "$list"
while IFS= read -r case; do
    n_cases=$((n_cases + 1))
    ir=$(dirname "$case")/ir.json
    if [ ! -f "$ir" ]; then
        echo "FAIL: $case has no sibling ir.json" >&2
        status=1
        continue
    fi
    if ! out=$("$BIN" "$ir" "$case" 2>&1); then
        printf 'FAIL: %s does not replay:\n%s\n' "$case" "$out" >&2
        status=1
    fi
done < "$list"

if [ "$n_cases" -eq 0 ]; then
    echo 'FAIL: no case files under corpus/golden — the oracle has nothing to replay' >&2
    exit 1
fi
# The floor on the green zone's case files. Gate 4 sees the same files plus the red zone's, but its
# total is the sum of two zones, so a golden case traded for a red one keeps it equal — the floor
# per zone (here, and gate 8 for corpus/red) is strictly stronger and is where they live.
if [ "$n_cases" -lt "$EXPECTED_MIN_CASES" ]; then
    echo "FAIL: $n_cases golden case(s) replayed, but EXPECTED_MIN_CASES pins at least $EXPECTED_MIN_CASES — the green corpus shrank (a deliberate removal lowers the floor in the same commit)" >&2
    exit 1
fi

# check 4: goldens without any case file are listed, never silently green.
uncovered=''
for d in corpus/golden/*/; do
    [ -f "${d}ir.json" ] || continue
    if ! ls "${d}"case*.json >/dev/null 2>&1; then
        uncovered="$uncovered ${d%/}"
    fi
done

if [ "$status" -eq 0 ]; then
    printf 'check-oracle: PASS (ref suite %s worked examples, floor %s; %s cases replayed exactly, floor %s' \
        "$SUITE_N" "$EXPECTED_MIN_EXAMPLES" "$n_cases" "$EXPECTED_MIN_CASES"
    if [ -n "$uncovered" ]; then
        printf '; NOT replayed, no case file yet:%s' "$uncovered"
    fi
    printf ')\n'
fi
exit "$status"
