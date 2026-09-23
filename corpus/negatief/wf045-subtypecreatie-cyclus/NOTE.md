# wf045-subtypecreatie-cyclus

Violates **WF-045** (`spec/IR-SPEC.md` ch. 4.6, `RUNTIME-SEMANTICS.md`
ch. 6.2): the object creation iterates the `Strandrecht` population and
creates a `Wrakrecht`, which IS a `Strandrecht` — so every pass hands the
next pass more subjects and the creation feeds itself.

    WF-045 FAIL /rules/objectCreaties/0: dependency cycle through no recursive
    creation: boek een wrakrecht bij elk strandrecht -> boek een wrakrecht
    bij elk strandrecht

The cycle runs through the SUPERTYPE's population node, which is why it needs
the chain-closed keying to be seen at all. Measured before it (2026-09-12) on
this very document: verifier PASS, and `ref` on a single-instance case
`EVALUATION FAULT: no fixpoint within 100 passes — divergence is a fault
(ch. 5.7)`. Loud, but at the wrong layer and under a report that names
neither the creation nor the type: the fixpoint counter is the last line of
defence, and the document was refusable statically.

The literal-name twin of this case was ALREADY refused before the repair — a
creation with `subject` and `nieuwType` both `Strandrecht` produces exactly
the report above — so what the case pins is that a subtype is not an escape
hatch from a rule the same document in one type already met.

**The boundary, measured on this document's three variants** (2026-09-12,
all three verifier-PASS after the repair, as they must be — the rule refuses
cycles, not subtype creation):

- `subject: Wrakrecht`, `nieuwType: Strandrecht` — the OTHER direction.
  Creating a `Strandrecht` does not grow the `Wrakrecht` population, so there
  is no edge and no cycle. This is why the closure runs upward on writes only
  and never downward on reads: closing reads would refuse this document.
- `subject: Kustgemeente`, `nieuwType: Wrakrecht` — a subtype created from a
  subject outside its chain. Two population writes (`Wrakrecht` and
  `Strandrecht`), one unrelated read, no cycle.
- the same creation inside a `regelGroep` marked `recursief` with a guard —
  removed from the graph by WF-045's own recursive-creation exemption, as
  every guarded recursive creation is.

Falsifiable in one edit: drop `Wrakrecht`'s `supertype` and the two types
become unrelated, the population write no longer reaches the subject's node,
and the document is clean.

Minimal own-authored document (Jutterswet vocabulary), on the type pair of
`corpus/golden/anchor/jutterswet-strandrechtketen`.
