# atmost-no-vacuous-true

The red witness of **boundary 1** of `RUNTIME-SEMANTICS.md` ch. 5.10:
`at-most`'s complementary direction is **not** a stop trigger.

## What it pins

```
Het teken van een Bakenwacht moet gesteld worden op 1
indien hij aan ten hoogste twee van de volgende voorwaarden voldoet:
- het aandeel is groter dan 0        (de deling)
- zijn ploegomvang is gelijk aan 0.
```

The threshold (2) covers every member (2), so the composite can no longer
become false: its outcome is already fixed **true** before a single
member is read. Boundary 1 says that is nonetheless not a stop — the
members are read, and member 1 divides `vangstwaarde` by a
`ploegomvang` of 0.

So the pinned behaviour is a **fault**, in ref and in clingo alike. That
is what makes the boundary machine-checkable in the one direction a green
golden cannot express: a green case can only pin that a fault is absent,
and boundary 1 is precisely a place where the fault must be **present**.

An executor that adds the vacuous-true trigger answers `teken = 1`
without reading anything and this case stops failing — measured on an
arm-exact ref mutant that adds exactly that row:

```
met de trigger : EXPECTED FAULT DID NOT OCCUR: evaluation succeeded   (exit 1)
zonder         : oracle: expected fault observed (division by numeric zero)
```

## Why it is red and not merely a marker

The fault is the ordinary `division by numeric zero` of
`corpus/rood/division-by-numeric-zero` — the ch. 1.4 point 4 v0 mirror,
which the tracked v1 delta will one day route to the §9.5 channel. This
case adds nothing to that question: it fixes only *that the divisor is
reached at all*. When the v1 delta lands, this case moves with its
sibling — the pinned fragment changes, the reason for the case does not.
