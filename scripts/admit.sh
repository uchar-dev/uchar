#!/bin/sh
# The PLAN §8 admission protocol, mechanized (corpus/README.md
# "Admission rules", rule 2). Usage:
#
#   scripts/admit.sh <candidate-dir> <name>
#
# Admits <candidate-dir> into corpus/golden/<name>/ only when ALL hold:
#
#   1. the candidate is self-contained: no machine-local paths, no
#      private-repo names, no TOKA material (the gate-1 patterns);
#   2. its ir.json exists and the verifier accepts it;
#   3. it carries at least one case*.json (an empty candidate is a
#      refusal, never silently admitted);
#   4. every case replays EXACTLY in ref AND in clingo — the
#      differential square as far as it exists in this repo. A
#      disagreement between the executors is a spec bug routed to
#      adversarial triage, NEVER a case to admit;
#   5. every case refutes the no-op executor (a case a do-nothing
#      executor satisfies tests nothing);
#   6. after the copy, the full gate set passes — a red gate rolls the
#      admission back and reports.
#
# Any refusal prints the reason and leaves the corpus untouched.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

if [ $# -ne 2 ]; then
    echo "usage: scripts/admit.sh <candidate-dir> <name>" >&2
    exit 2
fi
CAND=$1
NAME=$2
DEST="corpus/golden/$NAME"

fail() { echo "admit: REFUSED — $1" >&2; exit 1; }

[ -d "$CAND" ] || fail "candidate directory $CAND does not exist"
[ -f "$CAND/ir.json" ] || fail "$CAND carries no ir.json"
[ -e "$DEST" ] && fail "$DEST already exists; admission never overwrites"
case "$NAME" in
    */*) fail "name $NAME must be a bare directory name" ;;
esac

# Locate dune; fall back to the opam environment when it is not on PATH.
if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
command -v dune >/dev/null 2>&1 || { echo 'admit: need dune (OCaml)' >&2; exit 2; }
dune build spec/verifier ref backends/clingo 2>&1 || fail "build is red"

VERIFIER=spec/verifier/_build/default/bin/verifier.exe
[ -x "$VERIFIER" ] || VERIFIER=_build/default/spec/verifier/bin/verifier.exe
REF=_build/default/ref/bin/runner.exe
CLINGO=_build/default/backends/clingo/bin/clingo_runner.exe

# 1. self-containedness on the candidate (the gate-1 patterns).
PRIVATE='(/users/|/home/|~/|regelspraak-java|regelspraakruntime|calculemus)'
if grep -rnIiE "$PRIVATE" "$CAND"; then
    fail "machine-local path or private-repo reference in the candidate"
fi
if grep -rnIiE '(^|[^[:alpha:]])toka([^[:alpha:]]|$)' "$CAND"; then
    fail "TOKA material in the candidate (rights policy, corpus/README.md)"
fi

# 2. the verifier accepts the document.
"$VERIFIER" "$CAND/ir.json" || fail "the verifier rejects $CAND/ir.json"

# 3. at least one case.
CASES=$(find "$CAND" -maxdepth 1 -name 'case*.json' | sort)
[ -n "$CASES" ] || fail "$CAND carries no case*.json (an empty candidate never admits)"

# 4 + 5. the differential square and the no-op refutation, per case.
for case in $CASES; do
    if ! "$REF" "$CAND/ir.json" "$case"; then
        fail "ref does not replay $case — fix the candidate or triage the spec"
    fi
    if ! "$CLINGO" "$CAND/ir.json" "$case"; then
        fail "clingo does not replay $case: ref and clingo DISAGREE — a spec bug for adversarial triage, never a case to admit"
    fi
    if "$REF" --noop "$CAND/ir.json" "$case" >/dev/null 2>&1; then
        fail "$case is satisfied by a do-nothing executor and tests nothing"
    fi
done

# 6. copy, then the full gate set; a red gate rolls back.
mkdir -p "$DEST"
cp "$CAND/ir.json" "$DEST/"
[ -f "$CAND/bron.rgs" ] && cp "$CAND/bron.rgs" "$DEST/"
for case in $CASES; do cp "$case" "$DEST/"; done
for gate in scripts/check-*.sh; do
    if ! sh "$gate" >/dev/null 2>&1; then
        rm -rf "$DEST"
        fail "gate $gate turned red after the copy — admission rolled back"
    fi
done

n=$(echo "$CASES" | wc -l | tr -d ' ')
echo "admit: ADMITTED $DEST ($n case(s); verifier-clean, ref = clingo, no-op refuted, gates green)"
