#!/bin/sh
# Gate 7: the no-op guard (PLAN section 8: "an identity transpiler must
# fail"). The reference runner's --noop mode skips evaluation and checks
# every golden case's expectations against the raw input world. A corpus
# a do-nothing executor satisfies has stopped testing anything, so:
#
#   1. ref/ builds;
#   2. every corpus/golden/**/case*.json run with --noop must FAIL
#      (exit non-zero) — the case refutes a do-nothing executor;
#   3. the cases that legitimately survive a no-op run (they pin
#      defeasible standing: an input value deliberately left standing,
#      or a consistency that holds on the input) are pinned in the
#      explicit survivor list below. A survivor outside the list is a
#      failure (the corpus lost discriminating power — pin it here,
#      with its reason, only if the standing is the very thing the case
#      tests); a listed survivor that no longer survives, or no longer
#      exists, is a stale entry and also a failure.
#   4. an empty case set is a failure, never silently green.
#
# Survivors, each with the standing it pins:
#   - jutterswet-vondstwaardering/case5-input-blijft.json: a
#     non-deciding ladder leaves the input standing (ch. 7.1).
#   - jutterswet-waardering-v0vorm/case3-input-blijft.json: the v0
#     initialisation leaves a filled input standing (ch. 5.4).
#   - jutterswet-vondstnummer/case-consistent.json: a uniqueness that
#     holds on the input — "consistent" is true of an empty channel.
#   - jutterswet-nummerregister/case2.json: the ever-unique
#     NON-collision (same-instance redeclaration) — the absence of a
#     channel fact IS the assertion (RS ch. 5.9).
#   - jutterswet-strandregister/case2.json: the same non-collision on
#     the single form (shared tuple function, RS ch. 5.9).
#   - jutterswet-strandregister/case4.json: the scope narrowing — the
#     same ever-pair under two DIFFERENT parents stays consistent.
SURVIVORS='corpus/golden/anchor/jutterswet-vondstwaardering/case5-input-blijft.json
corpus/golden/anchor/jutterswet-waardering-v0vorm/case3-input-blijft.json
corpus/golden/jutterswet-vondstnummer/case-consistent.json
corpus/golden/anchor/jutterswet-nummerregister/case2.json
corpus/golden/anchor/jutterswet-strandregister/case2.json
corpus/golden/anchor/jutterswet-strandregister/case4.json'

set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
if ! command -v dune >/dev/null 2>&1; then
    echo 'FAIL: need dune (OCaml). Install via opam: opam install dune yojson zarith' >&2
    exit 2
fi

if ! dune build ref 2>&1; then
    echo 'check-noop: FAIL (build)' >&2
    exit 1
fi
BIN=_build/default/ref/bin/runner.exe

listed() {
    for s in $SURVIVORS; do
        [ "$s" = "$1" ] && return 0
    done
    return 1
}

status=0
n_cases=0
n_refuted=0
n_survivors=0
list=$(mktemp) || exit 2
trap 'rm -f "$list"' EXIT
find corpus/golden -name 'case*.json' 2>/dev/null | sort > "$list"
while IFS= read -r case; do
    n_cases=$((n_cases + 1))
    ir=$(dirname "$case")/ir.json
    "$BIN" --noop "$ir" "$case" >/dev/null 2>&1
    code=$?
    if [ "$code" -eq 0 ]; then
        if listed "$case"; then
            n_survivors=$((n_survivors + 1))
        else
            echo "FAIL: $case is satisfied by a do-nothing executor and is not a pinned survivor" >&2
            echo "  (if the standing IS what the case tests, pin it in check-noop.sh with a reason)" >&2
            status=1
        fi
    elif [ "$code" -ne 1 ]; then
        # exit 2 is a load error, not a refutation — a broken case must
        # never count as discriminating power
        echo "FAIL: $case errored under --noop (exit $code), refuting nothing" >&2
        status=1
    elif listed "$case"; then
        echo "FAIL: stale survivor entry — $case no longer survives a no-op run" >&2
        status=1
    else
        n_refuted=$((n_refuted + 1))
    fi
done < "$list"

for s in $SURVIVORS; do
    if [ ! -f "$s" ]; then
        echo "FAIL: stale survivor entry — $s does not exist" >&2
        status=1
    fi
done

if [ "$n_cases" -eq 0 ]; then
    echo 'FAIL: no case files under corpus/golden — nothing refutes a do-nothing executor' >&2
    exit 1
fi
if [ "$n_refuted" -eq 0 ]; then
    echo 'FAIL: no case refutes a do-nothing executor' >&2
    exit 1
fi

if [ "$status" -eq 0 ]; then
    printf 'check-noop: PASS (%s cases refute a do-nothing executor; %s pinned survivors pin defeasible standing)\n' \
        "$n_refuted" "$n_survivors"
fi
exit "$status"
