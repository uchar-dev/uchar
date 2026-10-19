Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) at its **aggregation
position**: `first(kust van alle schuiten van de Berging)` carries a
node `type` naming the enumeration domain `Vaarwater` while its operand —
`kust` of `Schuit`, read faithfully as `Kust` — names `Kust`. The
element-passing folds hand a contributor through unchanged, so the node
type is a pure claim about a value the fold never touches: with one
contributor no comparison happens at all and the alien label `Noord`
arrives in `Berging.vaarwater`, whose truthful `Match` reader has no arm
for it.

    WF-089 FAIL /rules/rules/0/versions/0/value/type: the first
    aggregation is typed with keuzetype domain 'Vaarwater', but operand 0 names
    domain 'Kust'

Why it has to exist: the reference arms of WF-089 bind an
`AttributeReference`/`DimensionCellRead`/`ParameterReference`/`BinderRef` to the
declaration it reads, and WF-023/WF-067 bind every value written into a
declaration — but the fold sat between them, crossed by nothing. WF-037
admits a one-argument value fold as soon as `elementType` is set;
no rule looked at the node type. Before the rule the document was
verifier-clean (`verifier: PASS`) while ref faulted at evaluation
(`match has no arm for enumeration value "Noord"`, in a message that
promised the fault was unreachable) and clingo refused the translation
ahead of it (`a text/enum/boolean operand here is an own-subject
attribute read or a literal`) — a carrier fence, not a verdict. The
faithful twin (everything `Vaarwater`) replays `oracle: PASS`, so the
shape itself is legitimate; only the claim is false. The sibling shape
in which the fold IS the `Match` scrutinee — no attribute write at all,
so WF-023 never participates — fires the same arm at
`/rules/rules/0/versions/0/value/scrutinee/type`.

Falsifiable: make the passthrough cross in the verifier's `Functie` arm
return `[]` and the WF-089 line disappears here, on
`wf089-minimum-pass-through` and on `wf090-pass-through` — three reports
change and no other — while every other wf089 case, the reference arms
and `wf089-foldbron-enum`, is untouched. Re-measured after the WF-038
ordering-fold clause landed (2026-09-05): the three no longer turn
*clean* under that mutant, because a `first` over an enumeration
contributor violates that clause on the very same operand. The
falsification signal is the rule this document is filed under, which the
gate reads: under the mutant it is rejected for WF-038 and no longer for
the WF-089 its directory declares.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the aggregation's annotation, a
derived position, keeps its (V5 (a)); operand 0 is now typed by the
attribute it reads. The rule still fires on the same arm, and `expect.txt`
pins it.
