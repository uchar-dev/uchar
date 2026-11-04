Violates WF-088 (`spec/IR-SPEC.md` ch. 2.4) three times over, at the one
family of positions the rule reaches only through its **nested** walk: a
`FunctionDef` whose sum-typed parameter, result and body carry a payload
`Enumeration` whose inline value list drifts from the domain it names.

The declaring enumeration `bergingsstatus` has two values (`Geborgen`,
`Vermist`); the `bergingsmelding` sum type's `Gemeld` payload repeats
them faithfully in `domains`, and the three use sites repeat a third
value `Verspoeld` that no declaration has. The verifier reports:

    WF-088 FAIL /model/functionDefs/0/parameters/0
    WF-088 FAIL /model/functionDefs/0/result

the model layer's `FunctionDef` arm (parameter and result), both reached
through `enum_use_site`'s recursion into **sum-type payloads**.

Why this case has to exist. Nothing else on the wire catches a drifted
enumeration in this position: WF-061 pins a `SumType` use site against
its declaration only at attribute and parameter datatypes, WF-067 checks
only that a `FunctionDef` *result* names a declared sum type, and WF-063
never looks here at all. Delete the `Sum_type` arm of `enum_use_site` and
this document passes for WF-088 while every other negative case stays
red — the gate-3 run on that mutant is green without this case and red
with it, which is what makes the recursion arm falsifiable rather than
merely present.

The `bergingsmelding` declaration itself repeats the enumeration faithfully,
and WF-088 walks that copy as well: a payload of a sum-type declaration is a
use site of the domain it names (`wf088-sum-payload-drift` is the witness of
that position). So the document carries exactly the two lines above.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`). Until L2 a
third line came from the expression layer: the body is a `BinderRef`, whose
own `type` repeated the drifted copy. A binder read carries no type since L2
(B13) — its binder's declaration, here the parameter, is its type — so that
line left with the annotation, and the two declaration lines carry the
witness.
