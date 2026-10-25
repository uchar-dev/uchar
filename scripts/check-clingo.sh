#!/bin/sh
# Gate 6: the differential — clingo replays every corpus case exactly
# (PLAN section 6, Phase 3: "differential testing becomes a square;
# every divergence is by definition a spec bug"). The backend translates
# ir.json + case.json to ASP-Core-2, clingo computes the stable model,
# and the model is checked against the same absolute expectations gate 5
# pins for ref/ — so a green gate 5 plus a green gate 6 is
# ref ≡ Clingo over the corpus, anchored to hand-written values, never
# parity alone.
#
# Checks:
#   1. backends/clingo builds (the OCaml workspace, scripts/lib/ocaml-build.sh);
#   2. clingo is available;
#   3. every corpus/golden/**/case*.json replays exactly through the
#      translate → clingo → check pipeline; an empty case set is a
#      failure, never silently green.
#
# The replays run on one process per two cores (scripts/lib/parallel.sh;
# the measurement is below). The loop that judges them reads each case's
# output and exit status in the order of the case list, so the report is
# the one a replay case by case would give.
#
# The gate takes no arguments. One pair plays through the runner itself,
# as backends/clingo/README.md shows:
#   _build/default/backends/clingo/bin/clingo_runner.exe <ir.json> <case.json>
set -u
[ "$#" -eq 0 ] || { echo "usage: scripts/check-clingo.sh (no arguments)" >&2; exit 2; }
cd "$(git rev-parse --show-toplevel)" || exit 2

if [ ! -d backends/clingo ]; then
    echo 'FAIL: backends/clingo not found' >&2
    exit 2
fi

if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
if ! command -v dune >/dev/null 2>&1; then
    echo 'FAIL: need dune (OCaml). Install via opam: opam install dune yojson zarith' >&2
    exit 2
fi
if ! command -v clingo >/dev/null 2>&1; then
    echo 'FAIL: need clingo on PATH (e.g. pip install clingo)' >&2
    exit 2
fi

. scripts/lib/ocaml-build.sh
if ! ensure_ocaml; then
    echo 'check-clingo: FAIL (build)' >&2
    exit 1
fi

# --- monotone floor (maintainer decision 2026-09-09) ---------------------------------------------
# A lower bound, never an equality: it catches SHRINK — the suite that quietly runs half — while
# growth needs no bump, so an ordinary corpus card touches no protected script. Raised deliberately,
# at an occasion. Accepted weakness, recorded rather than hidden: a count that stays equal while its
# content shifted passes; the name-carrying lists are what cover that direction. A deliberate
# removal lowers the floor in the same commit.
# The suite is floored because nothing else counts it — it is not corpus files, so no other gate
# would notice it halving. NO floor on the replayed-case count: that is byte-for-byte the file set
# gate 5 already floors (both scan corpus/golden for case*.json), and one quantity wants one floor.
EXPECTED_MIN_SUITE_CHECKS=83   # 2026-09-10: measured 83 (backends/clingo/test/test_clingo.ml)

# The backend's own unit suite carries the fault-mode proof obligations of the
# normalisation pass (no finest unit faults; 1/24 stays exact) that no corpus witness
# can exercise — an un-run suite is a silent green, so the gate runs it and
# demands a non-zero, floored check count (the gate-9 zero-checks discipline).
SUITE_BIN=_build/default/backends/clingo/test/test_clingo.exe
SUITE_OUT=$("$SUITE_BIN" 2>&1)
if [ $? -ne 0 ]; then
    echo "$SUITE_OUT" >&2
    echo 'check-clingo: FAIL (backend unit suite)' >&2
    exit 1
fi
SUITE_CHECKS=$(printf '%s' "$SUITE_OUT" | grep -Eo '[0-9]+ checks' | grep -Eo '[0-9]+' | head -1)
if [ -z "$SUITE_CHECKS" ] || [ "$SUITE_CHECKS" -eq 0 ]; then
    echo "$SUITE_OUT" >&2
    echo 'check-clingo: FAIL (backend unit suite ran zero checks — a silent green is not green)' >&2
    exit 1
fi
if [ "$SUITE_CHECKS" -lt "$EXPECTED_MIN_SUITE_CHECKS" ]; then
    echo "FAIL: the backend unit suite ran $SUITE_CHECKS checks, but EXPECTED_MIN_SUITE_CHECKS pins at least $EXPECTED_MIN_SUITE_CHECKS — the suite shrank (a deliberately dropped check lowers the floor in the same commit)" >&2
    exit 1
fi
BIN=_build/default/backends/clingo/bin/clingo_runner.exe

# One process per two cores. clingo_runner and the clingo it starts are single-threaded, but more than
# eight processes do not help: measured 2026-10-25 on 16 cores, the 899 cases took 152 s one at a time,
# 51 s on 4 processes and 36 s on 8 and on 16, where the longest case alone takes 35 s
# (jutterswet-opvolgerversies/case3-tweede-versie.json). In `make gates` more processes would only take
# cores from the JUnit suite of gate 12, the longest gate of the set.
. scripts/lib/parallel.sh
if ! procs=$(parallel_jobs 2); then
    echo 'check-clingo: FAIL (no number of processes)' >&2
    exit 1
fi

status=0
list=$(mktemp) || exit 2
tasks=$(mktemp -d) || exit 2
trap 'rm -f "$list"; rm -rf "$tasks"' EXIT

find corpus/golden -name 'case*.json' 2>/dev/null | sort > "$list"
# collect: a task per case with a sibling ir.json, keyed by the case's line in the list
n_cases=0
while IFS= read -r case; do
    n_cases=$((n_cases + 1))
    ir=$(dirname "$case")/ir.json
    if [ -f "$ir" ]; then
        parallel_task "$tasks" "$n_cases" out "$BIN" "$ir" "$case"
    fi
done < "$list"
echo "check-clingo: replays on $procs processes (one per two cores)"
if ! parallel_run "$tasks" "$procs"; then
    echo 'check-clingo: FAIL (parallel_run failed over the replays; the parallel line above says why)' >&2
    exit 1
fi

# judge, in the order of the list
n_cases=0
while IFS= read -r case; do
    n_cases=$((n_cases + 1))
    ir=$(dirname "$case")/ir.json
    if [ ! -f "$ir" ]; then
        echo "FAIL: $case has no sibling ir.json" >&2
        status=1
        continue
    fi
    out=$(cat "$tasks/$n_cases.out")
    if [ "$(cat "$tasks/$n_cases.rc")" != 0 ]; then
        printf 'FAIL: %s does not replay through clingo:\n%s\n' "$case" "$out" >&2
        status=1
    fi
done < "$list"

if [ "$n_cases" -eq 0 ]; then
    echo 'FAIL: no case files under corpus/golden — the differential has nothing to replay' >&2
    exit 1
fi

if [ "$status" -eq 0 ]; then
    echo "check-clingo: PASS (backend suite $SUITE_CHECKS checks, floor $EXPECTED_MIN_SUITE_CHECKS; $n_cases cases replay exactly through clingo — ref = clingo over the corpus)"
fi
exit "$status"
