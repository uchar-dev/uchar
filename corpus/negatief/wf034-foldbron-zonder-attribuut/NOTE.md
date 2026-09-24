Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **fold-source**
`AttributeReference` that names no attribute.

    WF-034 FAIL /rules/resolvedRegels/1/versions/0/value/arguments/0: attribute
    reference on 'Vondst' without an attribute name: it resolves against no
    declaration

Why it exists separately from `wf034-attribuutref-zonder-attribuut`, which
carries the same empty field in a plain value position: the fold source
**never reaches `walk_expr`** — the aggregation branch of the `Functie` case
replaces the generic walk for that argument — and `aggregation_source_check`
resolves only on `attribuut = Some a`. So the generic clause does not fence
it, and this is not a guess: with the value-position arm in place and this one
absent, a sweep emptying all 751 `attribuut` positions of the goldens one at a
time still left **43** verifier-clean survivors, every one of them a
fold-/aggregation argument. With this arm too, **0**.

That is the same lesson the object-type half taught one card earlier
(`wf034-foldbron-zonder-objecttype`): each half of the resolving pair needs a
clause on BOTH paths, because the fold source is the one reference position
the generic walk does not visit.

Until L2 this document carried the measured **dangerous** shape: the source
read `Vondst.gewicht`, declared in `kg`, and was typed in `t` (same canonical
unit, factor 1000 — the convertible class no compatibility cross reaches).
With the name filled that lie was refused arm-exact (`WF-091 FAIL …
fold-source attribute reference 'gewicht' is typed 't' …`); with the name
emptied it was verifier-clean, because `resolved_attr_datatype` yields `None`
and the whole identity family deferred. Since L2 the source carries no type
of its own (B13), so the unit lie cannot be written any more; what remains
is the empty name.

Measured before the closure, on this document against
`corpus/golden/jutterswet-vondsttelling/case.json`:

    ref    : EVALUATION FAULT: missing fold attribute in IR node
    clingo : TRANSLATION FAULT: missing fold attribute
    Drools : ERROR ConstraintEvaluationException: Error evaluating constraint

All three refuse — loudly, but each in its own words and only at run time, on
a document three engines had to be handed before anything said what was wrong
with it. And "loud" is the accident of this shape, not a property of the
channel: the object-type half of the same pair, in the same fold-source slot,
was played by all three.

*Measured dracht:* zero outside this witness and its value-position sibling —
152 golden + 22 rood + 30 parity documents and the 238 negatief cases that
predate this card carry no `AttribuutRef` with `attribuut: null` anywhere.
Over-rejection is therefore impossible by measurement.

A hand-broken variant of `corpus/golden/jutterswet-vondsttelling/ir.json` (one
field emptied, one unit abbreviation and factor changed — a three-line diff);
the shape is schema-valid — `AttribuutRef.attribuut` is `["string","null"]` on
the wire — which is precisely why the verifier, not the schema, is the
fence.

Falsifiable, clause-exact: remove the `attribuut = None` arm from the
aggregation branch of `walk_expr`'s `Functie` case and this case alone loses
its WF-034 citation, every other `wf034-*` case staying red — gate 3 turns
red on it, since the directory declares a rule the report then no longer
names.

*Honesty correction (2026-09-04), against the paragraph above; it held
until L2.* It did not turn CLEAN, and "the whole identity family defers" was
no longer true: since WF-091 gained its DERIVED-type half
(`spec/IR-SPEC.md` ch. 4.1, composite positions) the tonne-for-kilogramme lie
was caught a second time at the fold ROOT, which crossed the aggregation's
own type against the type derived from operand 0 (the source's own `t`) and
needed no resolved declaration to do it. Measured with the arm removed, until
L2:

    WF-091 FAIL /rules/resolvedRegels/1/waarde/type: unit mismatch: the SOM
    aggregation is typed 'kg' of system 'Gewicht' (factor 1 to 'kg'), but the
    type derived from its first operand is 't' of system 'Gewicht' (factor
    1000 to 'kg')

What still deferred on `resolved_attr_datatype` yielding `None` was the
family's REFERENCE half — WF-089/WF-090 and WF-091's reference arms — which
is what that paragraph should be read as saying.

*Since L2* operand 0 has no type of its own: its type is its declaration's,
and a source that names no attribute has none, so the fold root derives
nothing to cross. Measured in the L2 fix round (2026-09-24), with the
`attribuut = None` arm disabled in a copy of `spec/verifier/lib/checks.ml`:
this document answers `verifier: PASS`, and over golden, rood, negatief and
the parity zone (718 documents) no other report changes. The arm is the only
fence on this shape again, as the "Falsifiable" paragraph above first said.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the fold
source lost its own `type` (B13), and with it the WF-091 line its unit lie
drew beside this one; the source still names no attribute. The rule still
fires on the same arm, and `expect.txt` pins it (U1).

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. The report quoted from
before L2 carries the pointer of that wire (`waarde`, since L4
`versions/0/value`). `expect.txt` pins the arm.
