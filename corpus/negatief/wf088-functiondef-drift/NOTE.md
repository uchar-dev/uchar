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
this document passes for WF-088 while every other negatief case stays
red — the gate-3 run on that mutant is green without this case and red
with it, which is what makes the recursion arm falsifiable rather than
merely present.

One incidental violation rides along, and it is structural rather than
sloppy: `WF-060 /model/domains/1/datatype` fires because an enumeration is
not an admissible sum-type payload. That fence is exactly why the
recursion is described as belt-and-braces in the verifier — WF-060,
WF-065 and WF-071 fence enumerations out of every nested position, so a
document that exercises the recursion is necessarily ill-formed for the
fence as well. The two rules are not interchangeable: WF-060 speaks about
the declaration's payload *kind*, WF-088 about a use site's value *list*,
and only the second one is checked at a `FunctionDef`. Gate 3 requires the
declared rule to be cited and allows the extra (`scripts/check-verifier.sh`
check 3).

A second, smaller flank this document records without pinning: WF-088
deliberately does not walk `domeinen`, so the same drift written into the
sum *declaration*'s payload copy would go unreported. That is the
declaration-side half of the use-site≡declaration question — measured at
the card `20260901-use-site-declaratie-identiteit`: for the enumeration
it cannot arise, because WF-060 (sum payload), WF-071 (struct field) and
WF-065 (list element) refuse a nested `Enumeration` in every declaration
position before it could drift; the use-site half is WF-089 (ch. 2.4).

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`). Until L2 a
third line came from the expression layer: the body is a `BinderRef`, whose
own `type` repeated the drifted copy. A binder read carries no type since L2
(B13) — its binder's declaration, here the parameter, is its type — so that
line left with the annotation, and the two declaration lines carry the
witness. The WF-060 line beside them is unchanged.
