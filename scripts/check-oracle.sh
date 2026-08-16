#!/bin/sh
# Gate 5: the oracle — the reference interpreter (ref/) replays every
# corpus case (PLAN section 8: "an interpreter replay of each corpus case
# must pass").
#
# Three checks:
#   1. ref/ builds (dune build at the workspace root, which also rebuilds
#      the ir_v0 loader it links against);
#   2. every corpus/golden/**/case*.json replays exactly: the runner loads
#      the sibling ir.json and the case file, evaluates to the fixpoint,
#      and compares every expectation exactly (exit 0); any mismatch or
#      load error turns the gate red with the runner's report;
#   3. goldens that have no case file yet are listed explicitly — they are
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

# check 1: build (workspace root: ref/ plus the ir_v0 library).
if ! dune build ref 2>&1; then
    echo 'check-oracle: FAIL (build)' >&2
    exit 1
fi
BIN=_build/default/ref/bin/runner.exe

if [ "$#" -eq 2 ]; then
    exec "$BIN" "$1" "$2"
fi

status=0
list=$(mktemp) || exit 2
trap 'rm -f "$list"' EXIT

# check 2: every case replays exactly.
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

# check 3: goldens without any case file are listed, never silently green.
uncovered=''
for d in corpus/golden/*/; do
    [ -f "${d}ir.json" ] || continue
    if ! ls "${d}"case*.json >/dev/null 2>&1; then
        uncovered="$uncovered ${d%/}"
    fi
done

if [ "$status" -eq 0 ]; then
    printf 'check-oracle: PASS (%s cases replayed exactly' "$n_cases"
    if [ -n "$uncovered" ]; then
        printf '; NOT replayed, no case file yet:%s' "$uncovered"
    fi
    printf ')\n'
fi
exit "$status"
