Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) at its **aggregation
position**: `EERSTE(kust van alle schuiten van de Berging)` carries a
node `type` naming the enumeration domain `Vaarwater` while its operand —
`kust` of `Schuit`, read faithfully as `Kust` — names `Kust`. The
element-passing folds hand a contributor through unchanged, so the node
type is a pure claim about a value the fold never touches: with one
contributor no comparison happens at all and the alien label `Noord`
arrives in `Berging.vaarwater`, whose truthful `Match` reader has no arm
for it.

    WF-089 FAIL /resolvedRegels/0/waarde/type: the EERSTE aggregation is
    typed with keuzetype domain 'Vaarwater', but operand 0 names domain
    'Kust'

Why it has to exist: the reference arms of WF-089 bind an
`AttribuutRef`/`DimensieCel`/`ParameterRef`/`BinderRef` to the
declaration it reads, and WF-023/WF-067 bind every value written into a
declaration — but the fold sat between them, crossed by nothing. WF-037
admits a one-argument value fold as soon as `elementObjecttype` is set;
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
`/resolvedRegels/0/waarde/scrutinee/type`.

Falsifiable: make the passthrough cross in the verifier's `Functie` arm
return `[]` and this document passes together with
`wf089-minimum-doorgeefluik`, while every other wf089 case — the
reference arms and `wf089-foldbron-enum` — stays red.
