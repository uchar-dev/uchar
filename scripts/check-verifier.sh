#!/bin/sh
# Gate 3: the IR verifier (spec/verifier/, OCaml).
#
# Three checks:
#   1. the verifier builds (dune build in spec/verifier/);
#   2. every corpus/golden/**/ir.json is verifier-clean (exit 0);
#   3. every corpus/negatief/<case>/ir.json is rejected (non-zero exit) AND
#      the verifier reports the WF rule the case declares in its directory
#      name (convention: wf###-<slug>, e.g. wf001-duplicate-parameter).
#      Additional incidental violations are allowed; a case that passes, or
#      fails only for other rules, turns the gate red. A negatief corpus
#      without cases is reported explicitly, never silently green.
#
# With explicit file arguments it runs the verifier against those files and
# prints the reports (handy for a dump outside the repo).
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

VERIFIER_DIR='spec/verifier'
if [ ! -d "$VERIFIER_DIR" ]; then
    echo "FAIL: verifier not found at $VERIFIER_DIR" >&2
    exit 2
fi

# Locate dune; fall back to the opam environment when it is not on PATH.
if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
if ! command -v dune >/dev/null 2>&1; then
    echo 'FAIL: need dune (OCaml). Install via opam: opam install dune yojson' >&2
    exit 2
fi

# check 1: build.
if ! dune build --root "$VERIFIER_DIR" 2>&1; then
    echo 'check-verifier: FAIL (build)' >&2
    exit 1
fi
BIN="$VERIFIER_DIR/_build/default/bin/verifier.exe"

if [ "$#" -gt 0 ]; then
    status=0
    for f in "$@"; do
        echo "== $f"
        "$BIN" "$f" || status=1
    done
    exit "$status"
fi

status=0

# check 2: golden dumps — and the red corpus's documents, whose faults
# are behavioural, never well-formedness — are verifier-clean.
list=$(mktemp) || exit 2
trap 'rm -f "$list"' EXIT

n_golden=0
find corpus/golden corpus/rood -name ir.json 2>/dev/null | sort > "$list"
while IFS= read -r f; do
    n_golden=$((n_golden + 1))
    if ! out=$("$BIN" "$f" 2>&1); then
        printf 'FAIL: document %s is not verifier-clean:\n%s\n' "$f" "$out" >&2
        status=1
    fi
done < "$list"

# check 3: negatief cases are rejected for the declared rule.
n_neg=0
find corpus/negatief -name ir.json 2>/dev/null | sort > "$list"
while IFS= read -r f; do
    n_neg=$((n_neg + 1))
    dir=$(basename "$(dirname "$f")")
    id=$(printf '%s' "$dir" | sed -n 's/^wf0*\([0-9][0-9]*\)-.*/\1/p')
    if [ -z "$id" ]; then
        echo "FAIL: negatief case directory '$dir' does not follow wf###-<slug>" >&2
        status=1
        continue
    fi
    expect=$(printf 'WF-%03d' "$id")
    if out=$("$BIN" "$f" 2>&1); then
        printf 'FAIL: negatief case %s passed the verifier (must violate %s):\n%s\n' \
            "$f" "$expect" "$out" >&2
        status=1
    elif ! printf '%s\n' "$out" | grep -q "$expect"; then
        printf 'FAIL: negatief case %s rejected, but not for %s:\n%s\n' \
            "$f" "$expect" "$out" >&2
        status=1
    fi
done < "$list"

if [ "$n_golden" -eq 0 ] && [ "$n_neg" -eq 0 ]; then
    echo 'check-verifier: build OK; 0 corpus files (corpus not yet populated)'
    exit 0
fi
if [ "$n_neg" -eq 0 ]; then
    echo 'FAIL: no negatief cases — every implemented WF check needs a failing case (spec discipline)' >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    echo "check-verifier: PASS ($n_golden golden clean; $n_neg negatief rejected for their declared rule)"
fi
exit "$status"
