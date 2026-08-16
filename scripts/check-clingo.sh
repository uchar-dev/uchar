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
#   1. backends/clingo builds (dune, workspace root);
#   2. clingo is available;
#   3. every corpus/golden/**/case*.json replays exactly through the
#      translate → clingo → check pipeline; an empty case set is a
#      failure, never silently green.
#
# With explicit arguments (ir.json case.json) it replays just that pair.
set -u
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

if ! dune build backends 2>&1; then
    echo 'check-clingo: FAIL (build)' >&2
    exit 1
fi
BIN=_build/default/backends/clingo/bin/clingo_runner.exe

if [ "$#" -eq 2 ]; then
    exec "$BIN" "$1" "$2"
fi

status=0
list=$(mktemp) || exit 2
trap 'rm -f "$list"' EXIT

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
        printf 'FAIL: %s does not replay through clingo:\n%s\n' "$case" "$out" >&2
        status=1
    fi
done < "$list"

if [ "$n_cases" -eq 0 ]; then
    echo 'FAIL: no case files under corpus/golden — the differential has nothing to replay' >&2
    exit 1
fi

if [ "$status" -eq 0 ]; then
    echo "check-clingo: PASS ($n_cases cases replay exactly through clingo — ref = clingo over the corpus)"
fi
exit "$status"
