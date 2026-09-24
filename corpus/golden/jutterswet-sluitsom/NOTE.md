# jutterswet-sluitsom — the listed value fold whose operand NAVIGATES

Own-authored Jutterswet material (strandjutten domain). The witness this
golden exists for: a **listed value fold** (`LAATSTE` over two listed
operands, IR-SPEC §5.8.3) whose **second operand is an ordinary navigated
read** — `het ijkloon van de bewarende jutter`, one hop over the fact type
`bewaring` from the `Vondst` subject to the `Jutter` who keeps it.

```
Regel sluitsom van een vondst
    De sluitsom van een Vondst is de laatste van haar meldloon
    en het ijkloon van de bewarende jutter.
```

## Why it is a golden and not a curiosity

WF-083 (ch. 4.3) says a fold **source** is anchored and never navigates.
That is a statement about the ONE-argument instance fold, whose single
argument is the source of the folded set. With two or more arguments the
node is the listed value fold and its operands are **ordinary reads**: all
three executors evaluate them one by one against the rule subject, through
the very same expression evaluator that navigates everywhere else. This
document pins that reading with absolute hand-computed values, so the
scope of WF-083 is not a claim in prose but a case that fails the moment
the scope is dropped.

Measured on this document, both spellings of the verifier (re-measured at L4,
2026-09-24; the scope is the `arity = 1` of `source_position` in
`spec/verifier/lib/checks.ml`):

```
scope removed (the whole "a fold source never navigates" family unscoped):
    WF-053 FAIL /rules/resolvedRegels/0/versions/0/value/arguments/1: navigation
                bindings on a fold source are ill-formed (the fold's relation
                is its range, sourceRole or sourcePath)
    WF-083 FAIL /rules/resolvedRegels/0/versions/0/value/arguments/1:
                fold-source objecttype 'Jutter' is not the fold's anchor
                'Vondst' (a fold without a range reads the subject's own
                attribute) …
scope in place:  verifier PASS
```

Until L2 the second line was WF-083's `navigatieRol` arm (`navigatieRol
'bewarende jutter' on a fold source that is not the v0 instance-fold source
…`). That field left the wire at L2 (`spec/proposals/V1-WIREMIGRATIE.md`
B11), and WF-083's anchor arm now reports the same operand.

while ref, the ASP backend and BOTH Drools transpiler branches play it and
agree on every pinned value. A rule that refuses what three executors
agree on is refusing a legal document.

## Why the source is hand-written

The RegelSpraak frontend cannot emit this shape. Two independent reasons,
both measured against `frontends/regelspraak` on this branch:

1. `de laatste van …` is restricted there to date expressions
   (`laatste van vereist datumexpressies, kreeg Numeriek`), while the
   numeric `EERSTE`/`LAATSTE` pair is admitted IR — `jutterswet-peildatum`
   pins it;
2. on a form it does accept, the emitter writes a listed fold without an
   `elementType` and leaves the pronoun operand (`haar meldloon`) unresolved
   beside a navigated sibling. The VESTIGIAL `elementType` a listed fold may
   carry — the tag that
   makes the fold-source arm run at all, and therefore the whole point of
   this witness — is exactly what that emission drops.

So the document is hand-written from declarations in the same idiom as
`jutterswet-tempovouw` (fact type `bewaring`: `bewarende jutter` — Jutter,
`vondst`/`vondsten` — Vondst), and it carries the navigation the way the
frontend spells it where it can. Since L2 that is the symbolic
`navigatieBindingen` hop alone: the written `navigatieRol` left the wire
(`spec/proposals/V1-WIREMIGRATIE.md` B11), and the binding is the only route
a reader takes. Until L2 the document carried both, so the bound route was
exercised and not merely the derivable one.

## Hand-derivation of the expected values

`LAATSTE` over listed operands returns the **maximum** of the operands
that carry a value; an empty operand is skipped, and all-empty derives
nothing (RUNTIME-SEMANTICS ch. 1.2e — the same law
`jutterswet-peildatum` pins on `hoogloon`).

### case1-genavigeerde-operand — both directions of the comparison

`J1.ijkloon = 30`; V1 and V2 both fill the role `bewarende jutter` with
J1, so both read 30 through the hop.

```
V1: meldloon 12, navigated ijkloon 30  ->  max(12, 30) = 30   (the HOP wins)
V2: meldloon 41, navigated ijkloon 30  ->  max(41, 30) = 41   (the OWN read wins)
```

Load-bearing in both directions: a consumer that dropped the navigated
operand would answer 12 for V1, and a consumer that dropped the own
operand would answer 30 for V2. One case cannot be satisfied by ignoring
either operand.

### case2-leeg-en-onverbonden — the empty corners of the hop

```
V3: meldloon 7, NO bewarende jutter  -> the hop yields leeg, skipped
                                     -> max(7) = 7
V4: no meldloon, NO bewarende jutter -> both operands leeg -> sluitsom leeg
V5: no meldloon, linked to J1        -> max(30) = 30
```

V3 pins that an unreachable hop contributes nothing rather than faulting;
V4 pins the all-empty corner (`attributeEmpty`, not 0 — `LAATSTE` has no
empty-source fallback); V5 pins the mirror of V3, where only the hop
carries a value.

## no-op refutation (gate 7)

Every case writes `sluitsom` on instances where the raw input does not
carry it, so a do-nothing executor fails both cases. No survivor entry is
needed.

## Annotation indifference (gate 13)

One unit-bearing `type` annotation, inside the twist table: the
`Aggregation`/`LAATSTE` result. Until L2 there were four — the three
`AttribuutRef` positions (the rule's target, the own operand, the navigated
operand) carried one each, and since L2 a reference carries no `type` (B13).
Measured then on the twisted copy — ref, the ASP backend, the Drools
exec-model branch and the Drools DRL-text branch all reproduced the untwisted
outcome exactly, so the golden is carried by the harness and needs no
survivor entry.
