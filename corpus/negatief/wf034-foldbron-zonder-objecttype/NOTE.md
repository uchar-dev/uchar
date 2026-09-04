Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **fold-source**
`AttributeReference` that names no object type. `objecttype` is the endpoint
the attribute's value lives on (ch. 4.1a), so a source without one resolves
against no declaration at all.

    WF-034 FAIL /resolvedRegels/1/waarde/argumenten/0: attribute reference
    'gewicht' without an object type: it resolves against no declaration

Why it has to exist *separately* from the sibling case
(`wf034-attribuutref-zonder-objecttype`, the same empty field in a plain
value position): the fold source **never reaches `walk_expr`** — the
aggregation branch replaces the generic walk for it — and
`aggregation_source_check` matches only on `objecttype = Some otn`. The
absent endpoint therefore fell through **both** fences at once: the generic
arm that reports it, and the fold-source check that would have resolved the
member. Silent with it are the fold source's own rules — the member/role
resolution (WF-034), the self-denoting-role check (WF-083), the
characteristic-in-a-value-position check (WF-080) — plus the whole identity
family, because `resolved_attr_datatype` yields `None` and WF-089/WF-090/
WF-091 defer on an unresolvable declaration.

This document carries the measured **dangerous** shape, not a neutral one:
the source reads `Vondst.gewicht`, declared in `kg`, and is typed in `t`
(same canonical unit, factor 1000 — the convertible class no compatibility
cross reaches). With `objecttype` filled that lie is refused arm-exact:

    WF-091 FAIL /resolvedRegels/1/waarde/argumenten/0/type: unit mismatch:
    fold-source attribute reference 'gewicht' is typed 't' of system
    'Gewicht' (factor 1000 to 'kg'), but the declaration it reads from is
    'kg' of system 'Gewicht' (factor 1 to 'kg')

With the field emptied it was verifier-clean — and **every executor plays
it**. Measured on this document against
`corpus/golden/jutterswet-vondsttelling/case.json` before the closure:
`ref` PASS (7 expectations), clingo PASS (7 expectations), Drools PLAYED.
That is strictly worse than the sibling channel, where the two OCaml
executors at least fault loudly and only Drools plays: here the tonne-typed
sum answers in kilogrammes in all three, and no rule and no executor fences
it. Corpus dracht of the shape is zero: 152 golden + 22 rood + 30 parity
documents plus the 236 negatief cases that existed before this one were
scanned for an `AttribuutRef` with `objecttype: null` in any position, and
the only occurrence anywhere is the sibling negatief — in a value position,
not a fold source. So the widening cannot over-reject. Confirmed the other
way round after the fact: nulling all 718 `objecttype` fields of the goldens
one at a time leaves 43 verifier-clean survivors before this clause and 0
after, and gate 3 stays at 174 golden clean.

A hand-broken variant of `corpus/golden/jutterswet-vondsttelling/ir.json`
(one field emptied, one unit abbreviation and factor changed); the shape is
schema-valid — `AttribuutRef.objecttype` is `["string","null"]` on the v0
wire, like nearly every reference field — which is precisely why the
verifier, not the schema, is the fence.

Falsifiable, clause-exact: remove the `objecttype = None` arm from the
aggregation branch of `walk_expr`'s `Functie` case and this case alone turns
clean, every other `wf034-*` case staying red.
