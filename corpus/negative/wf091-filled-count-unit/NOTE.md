# wf091-filled-count-unit

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, "Derived-type identity on
composite positions") at the **twelfth** derived arm, the unit facet: an
`filledCount` annotated `kg` where the operation derives no unit at all.

    WF-091 FAIL /rules/rules/0/versions/0/value/type: unit mismatch:
    the filledCount aggregation is typed 'kg' of system 'Gewicht' (factor 1
    to 'kg'), but the type a filled count derives is no unit

**This case exists because the hole was real, and it was found by a review
rather than by a gate.** `RUNTIME-SEMANTICS.md` ch. 3.4c and `IR-SPEC.md`
ch. 4.3 both say a filled count answers a whole, unitless cardinality
whatever the folded attribute carries — a MUST with, until this case, no
check and no failing witness. The identity engine matches on the fold KIND,
so the `count` arm did not cover the new kind and nothing crossed its node
type.

Measured on this very document before the arm landed, and it is worse than
a clean document that should not be: the lie is PLAYED. With the count's
target declared `kg` too — so WF-023's assignment cross agrees and has
nothing to say — the verifier reported **PASS**, and `ref` then replayed a
case over it with `oracle: PASS`, writing a unitless 1 into a kg-declared
attribute. The *identical* lie one kind over, under `count`, was
`WF-091 FAIL`. One aggregation's node type was unchecked while every other
one's was.

The document. The well-formed filled count of the sibling cases, with ONE
facet twisted and the rest left exactly as the operation derives them:
whole, zero decimals, non-negative — and `kg`. The target carries the same
annotation, deliberately, so that WF-023 agrees and the unit facet of
WF-091 is the only thing that can speak. The verifier reports one fault.

**Why a unit and not decimals.** `wf091-count-decimals` already pins the
decimals facet at the sibling arm, and the unit is the facet that travels:
a wrong decimal count is a precision lie a reader may notice, while a wrong
unit is the one an executor acts on — it is exactly what the annotation-
indifference harness of gate 13 exists for, and a cardinality is the one
result type no twist can reach, since it has no convertible sister.

Rood-eerst, measured on this document. Without the arm (verifier md5
`d970a571d0ecbc88de4bea5b14da9fc7` — byte-identical to the binary that
shipped `filledCount`, so the red-first state and the mutant are the
same measurement): `verifier: PASS`. With the arm (md5
`cb504f858c625baf82378a77d00362bf`): the single rejection above.

Falsifiable, arm-exact, twice. (1) Delete the `Aantal_gevulde` case from
`composite_expectation` in `spec/verifier/lib/checks.ml` — one arm, the
sibling of the `Aantal` one. Swept over golden + red + negative + parity
(696 documents) exactly this document moves, and it turns clean again; no
golden, red case, other negative or parity document changes a verdict or a
report line, and the rebuilt binary's md5 equals the pre-arm one. (2)
Neuter the engine's UNIT facet: eight derived unit cases turn clean, this
one among them (seven before it existed) — the count honesty note (1) of
the WF-091 index row carries.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the composite's annotation, a
derived position, keeps its (V5 (a)), so the cross now runs against the type
the operands' declarations derive. The rule still fires on the same arm, and
`expect.txt` pins it.
