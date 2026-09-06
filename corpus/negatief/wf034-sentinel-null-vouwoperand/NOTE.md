# wf034-sentinel-null-vouwoperand

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.3, the sentinel fence): a listed fold
operand carries **no `type` at all**. This is the UNSPELLED TWIN of the
explicit `{"node":"Onbekend"}` sentinel — the same defect written by omission
instead of by name — and the expression walk's own type prologue has always
refused the pair together (`unknown-type sentinel inside a structured node`
for the spelled one, `value node without a static type` for the twin). The
fold-argument branch REPLACES that prologue, and when the seventh wave
repeated the fence there it repeated only the spelled half; this document is
the position that half left standing.

    WF-034 FAIL /resolvedRegels/0/waarde/argumenten/1/type: value node without
    a static type

The document. `corpus/golden/jutterswet-peildatum` verbatim, with ONE byte-level
change: the second operand of its first fold — an `EERSTE` over two listed
date reads, with an `elementObjecttype`, so the arguments run through the
fold-argument branch — carries `"type": null` instead of its `DatumInDagen`
annotation. Nothing else moves; the other three folds keep both their
annotations. One fault, at that operand, and no other rule.

Rood-eerst, measured on this document. Pre-clause verifier
(md5 `c95650b1ab993c8a8b370e5b0617b0e6`, the seventh wave as first landed):
`verifier: PASS`. With the twin arm (md5 `2237a9524ea35c7663dad97a8635f91f`):
the rejection above.

Executors, on this document with the source golden's own first case (five
pinned expectations):

    ref         : PASS (5 expectations) — it PLAYS, answering peildatum
                  2026-05-01, the value the golden pins
    ASP backend : TRANSLATION FAULT: mixed date/non-date operands under an
                  eerste/laatste fold are not translated (ref faults the
                  unordered comparison)
    Drools      : PLAYED — it fires and answers 2026-05-01 (confirmed against
                  a deliberately wrong pin)

This is the class read from the other side: not one-answers-two-refuse but
**two answer and AGREE while the third refuses**, on a document the verifier
passed. `ref` and Drools both reach past the missing annotation to the
DECLARATION (the effective-type law, `RUNTIME-SEMANTICS.md` ch. 2.10) and
land on the same date; the ASP backend reads the wire annotation, sees a
non-date operand beside a date one, and refuses to translate. The verdicts
disagree because the wire form is underspecified, which is precisely what
well-formedness is for.

**Where the twin comes from, measured.** Not from a producer in this repo:
`IrEmitter.datatype` writes JSON `null` only when the model hands it a null
datatype, and the reference builder substitutes the sentinel first
(`if (dt == null) dt = onbekend();`), so the null never reaches the wire. The
measurement is direct — 73 of the repo's 78 `.rgs` sources emit (the other 5
are fragments that do not resolve standing alone), and **not one emitted
document contains `"type": null` anywhere**, 0 occurrences. So this half of
the fence closes a WIRE form open to any conforming emitter, where the spelled
half closes a live producer route as well. Both are refused alike, because a
consumer cannot tell them apart and the two OCaml executors already treat them
as one.

**What stays admissible, and why that is not an accident.** The exemption is
one predicate for both spellings, so the SINGLE argument of an aggregation
keeps its licence with no type just as it keeps it with the sentinel — and
that is measured, not assumed: nulling the source type of a single-argument
fold leaves `corpus/golden/jutterswet-vondstpiek` (9 expectations),
`jutterswet-buitsom` (5) and `jutterswet-drempelvangst` (5) verifier-clean and
replaying unchanged through **all three** executors. At a fold source the wire
annotation was never what a consumer read; at an ordinary read it is the only
thing some of them read.

Falsifiable, arm-exact: delete the `| None when not source_exempt ->` arm at
the head of the fold-argument branch's reference arm
(`spec/verifier/lib/checks.ml`). Swept over golden + rood + negatief + parity
(540 documents) exactly this one case turns clean and nothing else moves.
Deleting the whole clause instead frees six documents — this one, the three
`wf034-sentinel-gelest-*`/`-geleste-*` cases and the two
`wf034-sentinel-builtin-*` cases — and removes one line from the parity zone's
report without changing its verdict.
