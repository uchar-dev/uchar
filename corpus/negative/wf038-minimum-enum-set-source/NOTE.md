Violates WF-038 (`spec/IR-SPEC.md` ch. 4.6), the ordering-fold operand
clause, in the **set-source** form: `minimum(de kust van alle schuiten
van de Berging)` folds over `kust` of `Schuit`, declared as the
enumeration `Kust`. An ordering fold keeps the least contributor, and
`Enumeration` is equality-only by the ch. 2.4 orderability paragraph —
the fold asks for an order the element type does not have.

    WF-038 FAIL /rules/rules/0/versions/0/value/arguments/0: operand of
    the minimum ordering fold is enumeration, must be a number or a date

Why it has to exist: the set-source form spells its contributors as one
argument — the set source of a fold that carries a range — whose type is
the element attribute's datatype, so the
whole defect sits in a single operand and nothing else in the document
is a lie — no drifted domain, no unresolved name. WF-089 makes such a
fold's domain honest; it never asks whether the kind is admissible, and
before this clause the document was verifier-clean while the three
executors disagreed about what it even is. Measured, one document, three
answers: `ref` faults at evaluation
(`EVALUATION FAULT: unordered comparison: Noord vs Zuid`) but only from
the *second* contributor on — with a single moored schuit it silently
succeeds and writes that one label; the ASP backend refuses ahead of
translation (`TRANSLATION FAULT: a text/enum/boolean operand here is an
own-subject attribute read or a literal`), a carrier fence rather than a
verdict; and the Drools backend **throws**, on one contributor as
readily as on two
(`ERROR ConsequenceException: … java.lang.IllegalStateException: kan
accumulate-resultaat niet naar BigDecimal omzetten: class
java.lang.String`) — the accumulating min/max lowering assumes a number.
A verifier-clean document that crashes an engine is exactly what the
document layer is for. The faithful twin over an orderable element type
is a standing golden (`corpus/golden/jutterswet-vondstpiek`, the same
`minimum`/`maximum` set fold over a numeric attribute), so the shape is
legitimate; only the element type is not.

Falsifiable, arm-exact: make the per-operand orderability arm of the
WF-038 ordering-fold clause return `[]` and this case turns clean
together with `wf038-last-enum-articulated`,
`wf038-first-sum-type-articulated` and `wf038-maximum-sum-type-set-source`, while
`wf038-first-mixed-family` — whose defect is the cross-operand
family arm — stays red.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
