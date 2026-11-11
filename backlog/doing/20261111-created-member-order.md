# The order of the members of a to-many role, created members included

Goal: the order in which a fold reads the members of a to-many role is normed for every member, so that whether a
fault is reached (RS ch. 5.11) no longer depends on an executor's own order. Maintainer decision 2026-11-11, after a
Fable advice (`werk/review-fable/created-member-order/notes.md` beside the repository, with its probes and the
measurement switch in ref).

The rule: the links the case wrote, in written order; then every other link — materialised, or laid by a creation
or a fact creation — in instance order: case instances ascending by label, then created objects by the index of
their creation and then by their subject, recursively.

Measured (the advice): ref today appends a created link (`World.add_link`), so its order is that of execution
(stratum, generation, creation index, subject in case-file order); two probes show it moves with the order of the
instances in the case file and with the stratum of a creation. With the rule switched on in ref: 0 different
outcomes or codes over 1,241 case files, 0 different lines over 36,513 trace lines, 2,686 worked examples pass; of
57 reads with two or more created links 53 are in instance order already (the other 4 in
`anchor/jutterswet-gemeenteberging`); reversing the created part also changes nothing, so no corpus case observes
this order today.

Steps:
1. RS ch. 5.7a: the rule, replacing the "recorded residue"; ch. 3.9a ("contributors' own order"), 5.5, 5.11 (row and
   witness) and 5.12 follow; a worked example with absolute expectations. IR-SPEC does not change.
2. ref follows the rule; the boundary written/created lies in the world (the loader), because clingo's translator
   reads the fold order from the case links (since card `clingo-own-semantics`). ref-test for the worked example.
3. Witnesses: a red case where the rule makes a fault reached (ref EF-014 or EF-019; clingo in the divergence list
   with CLINGO-035; Drools an EXCLUDE pin DROOLS-259 or NOFAULT, measured), and a golden where the rule stops before
   the fault (gate 6 and 12 channel lines as measured). Gate 17's tripwire on `unordered` stays.
4. CONSTRUCTS: the rows that name the unnormed order (the main session lands them).

Acceptance: the worked example in ref/test; the red case and the golden pinned; `make gates` green; no other corpus
outcome changes (measure with the oracle of ref's trace).

May touch: `ref/`, corpus witnesses outside `corpus/golden/anchor`, this card. Protected (the main session lands
them): `spec/RUNTIME-SEMANTICS.md`, `scripts/`, `spec/CONSTRUCTS.md`.

Out of scope: clingo and Drools carrying the order (they keep refusing the form with CLINGO-035 and DROOLS-259, now a
limit of the executor and no gap in the spec).
