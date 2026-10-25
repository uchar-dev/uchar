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
command -v clingo >/dev/null 2>&1 || { echo 'admit: need clingo on PATH (the differential square needs both executors)' >&2; exit 2; }
# the OCaml workspace as the gates take it: built when a source is newer than its last green build
. scripts/lib/ocaml-build.sh
ensure_ocaml || fail "the OCaml build is red (the ocaml-build line above says why)"

VERIFIER=_build/default/spec/verifier/bin/verifier.exe
REF=_build/default/ref/bin/runner.exe
CLINGO=_build/default/backends/clingo/bin/clingo_runner.exe

# 1. self-containedness on the candidate (the gate-1 patterns). grep exits 0 on a hit, 1 on none
#    and 2 when it could not read a file; only 1 lets the candidate through, so a file the check
#    could not read refuses instead of passing as "no hit".
selfcheck() {  # <pattern> <reason for a hit>
    grep -rnIiE "$1" "$CAND"
    rc=$?
    case $rc in
        0) fail "$2" ;;
        1) ;;
        *) fail "the self-containedness check could not read the whole candidate (grep exit $rc)" ;;
    esac
}
PRIVATE='(/users/|/home/|~/|regelspraak-java|regelspraakruntime|calculemus)'
selfcheck "$PRIVATE" "machine-local path or private-repo reference in the candidate"
selfcheck '(^|[^[:alpha:]])toka([^[:alpha:]]|$)' "TOKA material in the candidate (corpus provenance, corpus/README.md)"

# 2. the verifier accepts the document.
"$VERIFIER" "$CAND/ir.json" || fail "the verifier rejects $CAND/ir.json"

# 3. at least one case, all at the top level (a nested case would be
#    silently skipped — refuse instead of swallowing), no whitespace in
#    names (the shell loop would split them).
CASES=$(find "$CAND" -maxdepth 1 -name 'case*.json' | sort)
[ -n "$CASES" ] || fail "$CAND carries no case*.json (an empty candidate never admits)"
NESTED=$(find "$CAND" -mindepth 2 -name 'case*.json')
[ -z "$NESTED" ] || fail "case files below the top level would be skipped: $NESTED"
case "$CASES" in *' '*) fail "case file names must carry no whitespace" ;; esac

# 4 + 5. the differential square and the no-op refutation, per case.
# The no-op verdict distinguishes exit codes like gate 7 does: exit 0
# means a do-nothing executor satisfies the case (refused); exit 2 is a
# load/config error, which refutes nothing (refused); only a clean
# evaluation-level failure (exit 1) is a genuine refutation.
for case in $CASES; do
    if ! "$REF" "$CAND/ir.json" "$case"; then
        fail "ref does not replay $case — fix the candidate or triage the spec"
    fi
    if ! "$CLINGO" "$CAND/ir.json" "$case"; then
        fail "clingo does not replay $case: ref and clingo DISAGREE — a spec bug for adversarial triage, never a case to admit"
    fi
    "$REF" --noop "$CAND/ir.json" "$case" >/dev/null 2>&1
    noop_rc=$?
    [ "$noop_rc" -eq 0 ] && fail "$case is satisfied by a do-nothing executor and tests nothing"
    [ "$noop_rc" -ge 2 ] && fail "$case errors under --noop (exit $noop_rc), which refutes nothing"
done

# 6. copy, then the full gate set; a red gate rolls back. NB: the
#    post-copy self-containedness gate reads tracked files only (git
#    grep); for the just-copied untracked files the pre-copy grep above
#    is the guard, and CI re-checks after commit.
mkdir -p "$DEST" || fail "cannot create $DEST"
# a failed copy removes the half-made directory, or the refusal would leave
# the corpus touched
cp "$CAND/ir.json" "$DEST/" || { rm -rf "$DEST"; fail "copy failed"; }
# the optional parts of a golden (corpus/README.md, Layout): a frontend source
# and the NOTE with the hand computation
for f in source.rgs source.catala_en source.flogic scenario.rgs NOTE.md; do
    if [ -f "$CAND/$f" ]; then cp "$CAND/$f" "$DEST/" || { rm -rf "$DEST"; fail "copy failed"; }; fi
done
for case in $CASES; do cp "$case" "$DEST/" || { rm -rf "$DEST"; fail "copy failed"; }; done
# the full set runs as everywhere else, through make gates: the build first, then the gates side by side
if ! make gates >/dev/null 2>&1; then
    rm -rf "$DEST"
    fail "make gates turned red after the copy — admission rolled back (the newest out/gates/<run>/summary.txt names the gate)"
fi

n=$(echo "$CASES" | wc -l | tr -d ' ')
echo "admit: ADMITTED $DEST ($n case(s); verifier-clean, ref = clingo, no-op refuted, gates green)"
