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
#      without cases is reported explicitly, never silently green. A case
#      directory that carries an expect.txt (card l2-referentielaag, U1) must
#      also be rejected on the ARM that file names: its one line is a literal
#      fragment of the expected verifier message, without a JSON pointer and
#      without a rule number, and it must occur verbatim (grep -F) in the
#      MESSAGE of a report line of the declared rule — the text after
#      "WF-### FAIL <pointer>: " on a line that begins with the declared
#      rule — so neither the line prefix nor a message of another rule can
#      satisfy it; an empty or blank expect.txt, or one of more than one
#      line, fails.
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

# --- monotone floor (maintainer decision 2026-09-09) ---------------------------------------------
# A lower bound, never an equality: it catches SHRINK — the negatief corpus that quietly halves —
# while growth needs no bump, so an ordinary corpus card touches no protected script. Raised
# deliberately, at an occasion. Accepted weakness, recorded rather than hidden: a count that stays
# equal while its content shifted passes; the name-carrying lists are what cover that direction. A
# deliberate removal lowers the floor in the same commit.
# The negatief cases are floored HERE because this is the only gate that sees them at all: a
# vanished wf###-case takes a MUST's failing witness with it (CLAUDE.md, Spec discipline) and no
# other check would notice. NO floor on n_golden: that is byte-for-byte the file set gate 2 already
# floors (both scan corpus/golden + corpus/rood for ir.json), and one quantity wants one floor.
EXPECTED_MIN_NEGATIEF=350   # 2026-09-10: measured 350 (corpus/negatief/**/ir.json)
# The arm fragments (U1) are floored for the same reason and in the same regime: a vanished
# expect.txt takes the arm pin of a rewritten witness with it, and its directory keeps passing on
# the rule number alone. Measured at the L2 landing: every rewritten or replaced witness of the
# reference layer carries one.
EXPECTED_MIN_EXPECT=84      # 2026-09-24: measured 84 (corpus/negatief/*/expect.txt)

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

# check 3: negatief cases are rejected for the declared rule, and on the
# declared arm where the case names one (expect.txt).
n_neg=0
n_expect=0
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
    arm="$(dirname "$f")/expect.txt"
    if [ -f "$arm" ]; then
        n_expect=$((n_expect + 1))
        frag=$(cat "$arm")
        if [ -z "$(printf '%s' "$frag" | tr -d '[:space:]')" ]; then
            printf 'FAIL: %s is empty or blank: it pins no arm\n' "$arm" >&2
            status=1
        elif [ "$(awk 'END { print NR }' "$arm")" -ne 1 ]; then
            printf 'FAIL: %s is not one line: an arm fragment is one literal line of the verifier message\n' "$arm" >&2
            status=1
        elif printf '%s\n' "$frag" | grep -qE '/(model|rules|diagnostics)/'; then
            printf 'FAIL: %s carries a JSON pointer: the fragment is the message, which a path shift must not break\n' "$arm" >&2
            status=1
        elif printf '%s\n' "$frag" | grep -qE 'WF-[0-9]{3}'; then
            printf 'FAIL: %s carries a rule number: the rule is the directory name, the fragment is the message of its arm\n' "$arm" >&2
            status=1
        elif ! printf '%s\n' "$out" | sed -n "s/^$expect FAIL [^ ]*: //p" | grep -qF -- "$frag"; then
            # Only the messages of the declared rule's lines count: sought in the whole report,
            # a fragment such as "FAIL", the rule number, or a line of another rule's message
            # would pass without pinning an arm of this rule (measured at the L2 lens).
            printf 'FAIL: negatief case %s rejected for %s, but not on the arm %s names (sought in the messages of the %s lines):\n  expected fragment: %s\n%s\n' \
                "$f" "$expect" "$arm" "$expect" "$frag" "$out" >&2
            status=1
        fi
    fi
done < "$list"

# The floor is evaluated BEFORE the unpopulated-corpus exit, or that exit would answer the very
# case the floor exists for: with the zones moved aside both counts are 0 and an "all zero -> exit
# 0" branch placed first never lets the floor run. Measured 2026-09-10, before this order: both
# zones aside gave PASS at 0. The floor also precedes the no-negatief check, so the zero case is
# reported with BOTH numbers rather than by the weaker non-empty message.
if [ "$n_neg" -lt "$EXPECTED_MIN_NEGATIEF" ]; then
    echo "FAIL: $n_neg negatief case(s) rejected, but EXPECTED_MIN_NEGATIEF pins at least $EXPECTED_MIN_NEGATIEF — the negatief corpus shrank (a deliberate removal lowers the floor in the same commit)" >&2
    status=1
elif [ "$n_golden" -eq 0 ] && [ "$n_neg" -eq 0 ]; then
    # Only reachable while no floor is pinned yet (floor 0) — the genuinely empty corpus.
    echo 'check-verifier: build OK; 0 corpus files (corpus not yet populated)'
    exit 0
elif [ "$n_neg" -eq 0 ]; then
    echo 'FAIL: no negatief cases — every implemented WF check needs a failing case (spec discipline)' >&2
    status=1
fi
if [ "$n_expect" -lt "$EXPECTED_MIN_EXPECT" ]; then
    echo "FAIL: $n_expect arm fragment(s) (expect.txt) checked, but EXPECTED_MIN_EXPECT pins at least $EXPECTED_MIN_EXPECT — arm pins vanished (a deliberate removal lowers the floor in the same commit)" >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    echo "check-verifier: PASS ($n_golden golden clean; $n_neg negatief rejected for their declared rule, floor $EXPECTED_MIN_NEGATIEF; $n_expect on their declared arm (expect.txt), floor $EXPECTED_MIN_EXPECT)"
fi
exit "$status"
