# jutterswet-seizoenvondst — the dimension-cell unit witness, verifier-only

The **`DimensieCel` unit witness** of the reference-type identity rule
(`IR-SPEC.md` ch. 4.1, WF-091) and of its domain siblings' cell arm
(WF-089/WF-090, ch. 2.4). Until this golden the cell-read arm had **no
positive witness anywhere**: the corpus carried exactly ONE
`DimensionCellRead` (in the witness cabinet `jutterswet-vormenkabinet`)
and it was not unit-bearing, so a verifier that OVER-rejected at that arm
left every gate green.

It is the **second verifier-only golden** (see the "witness cabinet"
section of `corpus/golden/README.md`): it carries no `case*.json`, so
gate 5 lists it as not replayed. That is a true report, and the section
below is the measurement that makes it true.

## What it pins

```
Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
    1. voorjaar
    2. najaar

het vondstgewicht Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg
    gedimensioneerd met seizoen;

Het voorjaarsgewicht van een Jutter moet berekend worden als
    zijn vondstgewicht in voorjaar.
```

The frontend lowers `zijn vondstgewicht in voorjaar` to a `DimensieCel`
whose OWN `type` is a copy of the dimensioned attribute's declared
datatype — unit sub-object included (`kg` of system `Gewicht`, factor 1
to `kg`) — over an inner `AttribuutRef` that carries the same copy. The
cell read's own type is what WF-091 crosses at this arm; the inner
reference is crossed on its own in the same walk.

**Two-sided, measured.** The document is verifier-clean, and a mutant
that fires at the `DimensieCel` arm on every unit-bearing use site turns
this document — and, before it landed, no document at all — red. The
refusal half, on a mutated copy of this same `ir.json` where the cell's
type is retyped `t` (canonical `kg`, factor 1000) while the declaration
and the inner reference stay `kg`:

```
WF-091 FAIL /rules/resolvedRegels/0/waarde/type: unit mismatch: dimension-cell
  read of 'vondstgewicht' is typed 't' of system 'Gewicht' (factor 1000
  to 'kg'), but the declaration it reads from is 'kg' of system 'Gewicht'
  (factor 1 to 'kg')
```

(The permanent refusal form is the negatief
`corpus/negatief/wf091-dimensiecel-eenheid`; what this golden adds is the
non-over-rejection a negatief can never pin.)

## Why no replay case — measured, per executor

A cell read is not executable by the v0 corpus toolchain, and the case
format has no way to supply one cell of a dimensioned attribute (the
`uchar-case-v0` value variants are number / date / dateTime / boolean /
text / enumeration / constructor / list / struct — there is no per-label
map). Each executor was run on this exact shape with a case supplying a
scalar `vondstgewicht`, and each declined, in its own words:

| executor | verdict |
|---|---|
| verifier (gate 3) | `verifier: PASS` — the well-formedness arm is the point of this document |
| `ref` (gate 5) | `EVALUATION FAULT: expression kind not implemented in this card` |
| `clingo` (gate 6) | `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)` |
| Drools (gate 12) | `EXCLUDE dimensioned attribute input 'vondstgewicht' (§3.6, a Map bean member) is not carried by the v0 backend` |

So a case here could pin nothing and would turn gates 5 and 6 red: it is
exactly the manufactured "make the report go away" case the witness
cabinet must not carry. When the cell read becomes executable — a case
format that carries cells, plus the `ref`/ASP arms — this witness moves
into an executing document and this NOTE goes with it.

Gate 10 still applies in full: `bron.rgs` re-emits byte-identically to
the checked-in `ir.json`, so the source-level shape is pinned too.
