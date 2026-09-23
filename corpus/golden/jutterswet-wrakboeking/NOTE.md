# jutterswet-wrakboeking — a creation that mints its own SUPERTYPE

The positive witness of the ch. 5.1 rule that **an object creation's member
write closes UPWARD only**, and the only document in the corpus that can
falsify it. Every other arm of the chain-closed dependency graph has a golden
that stays clean under it; this one is the arm's.

**The shape.** `Wrakrecht is een Strandrecht`. One rule computes the
subtype's own attribute from the inherited one:

> De bergingsvergoeding van een Wrakrecht is zijn grondslag.

and one creation mints a `Strandrecht` — the SUPERTYPE — for every
`Wrakrecht`, filling the inherited attribute from what that rule wrote:

> Boek een Strandrecht bij elk Wrakrecht, met als grondslag zijn
> bergingsvergoeding.

One direction is a real edge: the creation reads `bergingsvergoeding`, which
the rule writes, so the rule orders before the creation. The other direction
is the one the graph must NOT draw. The creation writes `grondslag` on a
freshly minted instance whose concrete type is exactly `Strandrecht`;
`Wrakrecht` also carries `grondslag` (it inherits it), but no `Wrakrecht` is
ever minted here — **creation is not polymorphic** (`IR-SPEC.md` ch. 2.2) —
so the rule's read of `Wrakrecht.grondslag` depends on nothing this creation
does, and the two forms are ordered, not circular.

**Falsifiable by one word in the verifier.** Let the creation's member write
close over the ⊑-comparable types in BOTH directions — the reading a rule
target, an allocation target and a defaults target all correctly take,
because those forms iterate an extension instead of minting one instance —
and this document turns into

    WF-045 FAIL /rules/resolvedRegels/0: dependency cycle through no recursive
    creation: bereken de bergingsvergoeding van een wrakrecht -> boek een
    strandrecht bij elk wrakrecht -> bereken de bergingsvergoeding van een
    wrakrecht

with the reference interpreter, which shares that graph, refusing to evaluate
while the ASP backend answers the pinned numbers — one document, a split
differential square, and no cycle anywhere in the model. Measured
2026-09-12, both readings, on this file.

**Replayed by all three executors** (gates 5, 6 and 12) on two subjects with
different grounds. The case pins the derived attribute rather than anything
about the created objects: `uchar-case-v0` has no way to address an instance
that did not exist in the input, and what this golden exists to pin is that
the document is ADMITTED and computes, not what the new `Strandrecht` holds.

The creation names its back-link explicitly (`verbindendeRol` on
`wrakboeking`) rather than leaving it to the ch. 5.5 derivation. That is not
decoration: with `Wrakrecht ⊑ Strandrecht` the pair is connected twice over
once the chain closes, and the derivation is what WF-048 refuses to guess at.
It is also what lets the Drools backend play the golden instead of refusing
it for an underivable role.

Sibling documents: `anchor/jutterswet-strandrechtketen` and
`jutterswet-wrakrecht` pin the two-spelling member write between RULES (both
directions, correctly); `jutterswet-zustervondst` pins that two incomparable
subtypes stay apart; `corpus/negatief/wf045-subtypecreatie-cyclus` and
`corpus/negatief/wf045-erfattribuut-spelling-cyclus` are the two cycles the
closure does catch.
