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
#   - jutterswet-waardestelling/case2-flank-input-blijft.json: the
#     false-condition flank on a standing target — no write, the input
#     stands (ch. 7.8).
#   - jutterswet-waardestelling/case3-leeg-valt-door.json: the leeg
#     fall-through on a standing target — a computed leeg never
#     displaces the input value (ch. 7.8; the pinned v0 delta).
#   - jutterswet-waardering-v0vorm/case6-leeg-doorval-input.json: the
#     same fall-through in the ch. 7.7 desugaring domain (fire-but-leeg
#     beside an initialisation, input standing) — the corner that was a
#     prose pin until ref carried the ladder reading natively.
#   - jutterswet-hardheid/case3-stilte.json: the discretionary route
#     stays silent without a decision (six finds, no strandvoogd act) —
#     the silence IS the legislator's intent (the hardship pattern).
#   - jutterswet-penningrecht-catala/case3-stilte.json: the Catala
#     render of the same articles — silence without a ground is the
#     meaning, exactly as the hand-written hardheid anchor pins it
#   - jutterswet-penningrecht-catala/case4-hardheidsgrens.json: a
#     hardship ground does not waive the halved threshold — four
#     finds stay short (the anchor's case4 reason, same corner)
#   - jutterswet-penningrecht/case3-stilte.json: the RegelSpraak render
#     of the same articles (the toolchain's two flat kenmerktoekenning
#     rungs, not a ladder) — silence without a ground is the meaning,
#     exactly as the hardheid anchor and the Catala render pin it.
#   - jutterswet-penningrecht/case4-hardheidsgrens.json: the same
#     halved-threshold corner on the RegelSpraak side — a hardship
#     ground leaves four finds short, nothing is attributed.
#   - jutterswet-hardheid/case4-hardheidsgrens.json: a decision does
#     not waive the halved threshold — four finds stay short, nothing
#     is attributed; the absence is the assertion.
#   - jutterswet-borgdeel/case5-input-blijft.json: the temporal
#     standing (ch. 7.8, timeline arm) — a non-deciding pro-rata fold
#     (empty result) leaves the input timeline standing whole.
#   - jutterswet-dagentelling/case3-leeg.json: the ch. 3.8 day count
#     over no condition periods writes nothing — empty stays empty
#     (never written) IS the assertion.
#   - jutterswet-dagentelling/case4-input-blijft.json: the temporal
#     standing for the day count — a non-deciding count leaves the
#     input timeline standing whole (ch. 7.8, timeline arm).
#   - jutterswet-beschikkingsom/case4-leeg.json: the leeg-scrutinee
#     corner (ch. 9.2) — with beschikking absent the match derives
#     nothing, so uitkering stays empty and both = and ≠ against
#     Aangehouden are false (Tabel 38); the expectation IS that absence,
#     which a do-nothing executor satisfies.
#   - jutterswet-staffelvergoeding/case3-afwezig.json: the leeg-list
#     propagation of ch. 10.3 — with the staffelbedragen parameter absent
#     (read leeg), the Fold propagates leeg and vergoeding stays empty; the
#     expectation IS that absence. The sibling case2-leeg-lijst (supplied
#     []) DOES decide — it writes 0.00 — and is deliberately not a survivor.
#   - jutterswet-aanspraaktelling/case3-afwezig.json: the same
#     leeg-propagation of ch. 10.4 — with the aanspraken list absent, the
#     MapList/Fold and the FilterList/ListLength both propagate leeg, so
#     erkende som and aantal erkend stay empty. Its case2-leeg-lijst
#     (supplied []) writes 0.00/0 and is not a survivor.
#   - jutterswet-vondstdossier/case3-afwezig.json: the record no-hole
#     absence (ch. 11) — with both struct inputs (dossier, dossiers) absent,
#     premie's GEVULD(dossier)-guarded rule does not fire, waardekopie's bare
#     FieldAccess on the leeg dossier propagates leeg, totaalwaarde's fold
#     propagates leeg over the absent list, samenvatting's StructLiteral
#     propagates leeg (its telling field |dossiers| evaluates leeg — the
#     build-side no-hole witness), and dubbel/afwijkend are both false
#     against the leeg struct operand (Tabel 38 — the both-directions
#     corner). The absence IS the assertion; case1/case2 (both lists
#     supplied) and case4 (dossier present, list absent) all decide and are
#     not survivors.
#   - jutterswet-nestuitzondering/case8-niets.json: the nested-group empty
#     fall-through (ch. 7.9) — with no ground characteristic present, rung 0's
#     sub-group is empty and its own grondX consequence does not fire, the base
#     group-rung's sub-group is empty too, so nothing decides and heffing stays
#     empty. The absence IS the assertion; the sibling cases all decide (a
#     value or a conflict fact) and are not survivors.
#   - jutterswet-nestkenmerk/case1-y-wint.json: the characteristic variant of
#     the spurious-conflict test (ch. 7.9) — the sub-decision do-not-attribute
#     (false, from ny) displaces rung 0's own true, so vrijgesteld stays absent.
#     The observable outcome (absent + consistent) coincides with a do-nothing
#     run, yet the case pins the nested reading against a FLATTENING executor,
#     which would manufacture a conflict here (case2-conflict and case3-terugval
#     both decide observably and are not survivors).
#   - synth3-standtarief/case4-leeg.json: a leeg scrutinee propagates through
#     Match (ch. 9.2) — with stand absent tarief stays empty; the absence IS the
#     assertion (case1/2/3 all decide and are not survivors).
#   - synth3-vangstsom/case4-afwezig.json: the leeg-list propagation of ch. 10.3
#     — the vangsten list absent, the Fold propagates leeg, totaal stays empty;
#     the supplied [] sibling (case3) writes 0.00 and is not a survivor.
#   - synth3-metingtal/case4-afwezig.json: the same leeg-list propagation through
#     MapList + Fold (bovental and somverhoogd both empty); case3 (supplied [])
#     decides 0/0.00 and is not a survivor.
#   - synth3-keuringtal/case4-afwezig.json: the leeg-list propagation through
#     FilterList + ListLength (all three counts empty); case3 (supplied []) writes
#     0/0/0 and is not a survivor.
#   - synth3-vondststaat/case3-afwezig.json: the record no-hole absence (ch. 11)
#     — both scalar inputs absent, the StructLiteral build propagates leeg, the
#     FieldAccess reads leeg, and the struct-equality characteristic is false
#     against a leeg operand; the absence IS the assertion.
#   - synth3-drempelheffing/case4-leeg-grondslag.json: the strict Let leeg-barrier
#     (ch. 10.6) — `Let g = grondslag` reads leeg, so the whole heffing chain is
#     leeg regardless of the other strict bindings; the absence IS the assertion.
#   - synth3-besluitschakel/case4-afwezig.json: a score absent leaves both
#     disjoint Construct writers unjustified, so uitkomst is undecided and the
#     downstream Match propagates leeg to bedrag; the absence IS the assertion.
#   - synth3-letbeslag/case3-leeg-regime.json: a leeg Match scrutinee propagates
#     (ch. 9.2) even while the strict Let binding stays gevuld — regime absent,
#     beslag empty; the value cases decide and are not survivors.
#   - synth3-letbeslag/case4-leeg-grondslag.json: the strict Let leeg-barrier
#     again (ch. 10.6), here composed under a Match body — grondslag absent makes
#     `Let g` leeg, so beslag is leeg before the Match ever dispatches.
#   - jutterswet-samenloop/case6-geen-grond.json: the no-ground flank of the
#     samenloop competition ladder (ch. 7.1) — with neither aanspraak ground
#     present no rung decides and vergoeding stays empty, whatever amounts the
#     input carries; the absence IS the assertion. The five sibling cases all
#     decide (the hoogste under samenloop, or the sole ground's bedrag) and
#     are not survivors.
#   - jutterswet-paasvondst/case5-leeg.json: the ch. 1.2d date-row leeg
#     corner — a dateless Vondst makes every extractor leeg, and the
#     both-leeg date gelijk-aan is onwaar (v0's Tabel-38 guard, NOT the
#     numeric two-empties-equal cell), so nothing is written and no
#     kenmerk attaches; the absence IS the assertion. The four sibling
#     cases all decide and are not survivors.
SURVIVORS='corpus/golden/anchor/jutterswet-vondstwaardering/case5-input-blijft.json
corpus/golden/anchor/jutterswet-waardering-v0vorm/case3-input-blijft.json
corpus/golden/jutterswet-vondstnummer/case-consistent.json
corpus/golden/anchor/jutterswet-nummerregister/case2.json
corpus/golden/anchor/jutterswet-strandregister/case2.json
corpus/golden/anchor/jutterswet-strandregister/case4.json
corpus/golden/anchor/jutterswet-waardestelling/case2-flank-input-blijft.json
corpus/golden/anchor/jutterswet-waardestelling/case3-leeg-valt-door.json
corpus/golden/anchor/jutterswet-waardering-v0vorm/case6-leeg-doorval-input.json
corpus/golden/anchor/jutterswet-hardheid/case3-stilte.json
corpus/golden/anchor/jutterswet-hardheid/case4-hardheidsgrens.json
corpus/golden/jutterswet-penningrecht-catala/case3-stilte.json
corpus/golden/jutterswet-penningrecht-catala/case4-hardheidsgrens.json
corpus/golden/jutterswet-penningrecht/case3-stilte.json
corpus/golden/jutterswet-penningrecht/case4-hardheidsgrens.json
corpus/golden/anchor/jutterswet-borgdeel/case5-input-blijft.json
corpus/golden/anchor/jutterswet-dagentelling/case3-leeg.json
corpus/golden/anchor/jutterswet-dagentelling/case4-input-blijft.json
corpus/golden/anchor/jutterswet-beschikkingsom/case4-leeg.json
corpus/golden/anchor/jutterswet-staffelvergoeding/case3-afwezig.json
corpus/golden/anchor/jutterswet-aanspraaktelling/case3-afwezig.json
corpus/golden/anchor/jutterswet-vondstdossier/case3-afwezig.json
corpus/golden/anchor/jutterswet-nestuitzondering/case8-niets.json
corpus/golden/anchor/jutterswet-nestkenmerk/case1-y-wint.json
corpus/golden/synth3-standtarief/case4-leeg.json
corpus/golden/synth3-vangstsom/case4-afwezig.json
corpus/golden/synth3-metingtal/case4-afwezig.json
corpus/golden/synth3-keuringtal/case4-afwezig.json
corpus/golden/synth3-vondststaat/case3-afwezig.json
corpus/golden/synth3-drempelheffing/case4-leeg-grondslag.json
corpus/golden/synth3-besluitschakel/case4-afwezig.json
corpus/golden/synth3-letbeslag/case3-leeg-regime.json
corpus/golden/synth3-letbeslag/case4-leeg-grondslag.json
corpus/golden/jutterswet-samenloop/case6-geen-grond.json
corpus/golden/jutterswet-paasvondst/case5-leeg.json'

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
