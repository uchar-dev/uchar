# jutterswet-seizoenvondst — the dimension-cell witness

Until L2 the **`DimensieCel` witness** of the domain rules' cell arm
(WF-089/WF-090, ch. 2.4) and of the reference-type identity rule
(`IR-SPEC.md` ch. 4.1, WF-091) at that arm. Before this golden the cell-read
arm had **no positive witness anywhere**: the corpus carried exactly ONE
`DimensionCellRead` (in the witness cabinet `jutterswet-vormenkabinet`) and
it was not unit-bearing, so a verifier that OVER-rejected at that arm left
every gate green. Since L2 a cell read carries no type of its own (B13), and
the reference arms of all three rules left the wire with it (the index rows
WF-089, WF-090 and WF-091 in `IR-SPEC.md`). What the golden pins since then:
the verifier passes a well-formed `DimensionCellRead` over a unit-bearing
dimensioned attribute, the cell's own shape checks stay live (WF-034,
`corpus/negative/wf034-dimension-cell-*`), and gate 10 re-emits it byte for
byte.

It carries a case, and `ref` and the ASP backend replay it (gates 5 and 6):
the cell read executes (`RUNTIME-SEMANTICS.md` ch. 5.8c).

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

The frontend lowers `zijn vondstgewicht in voorjaar` to a `DimensionCellRead`
over an inner `AttributeReference`. Since L2 neither node carries a type of its
own (`spec/proposals/V1-WIREMIGRATIE.md` B13): the cell read's type is the
dimensioned attribute's declared datatype (`kg` of system `Gewicht`, factor 1 to
`kg`), and every consumer reads it from that declaration (`RUNTIME-SEMANTICS.md`
§2.10). Until L2 both nodes carried a copy of that datatype, unit sub-object
included; WF-091 crossed the cell read's copy at this arm and the inner
reference's in the same walk.

**Two-sided, measured until L2.** The document was verifier-clean, and a
mutant that fired at the `DimensieCel` arm on every unit-bearing use site
turned this document — and, before it landed, no document at all — red.
The refusal half, on a mutated copy of this same `ir.json` where the cell's
type was retyped `t` (canonical `kg`, factor 1000) while the declaration
and the inner reference stayed `kg`:

```
WF-091 FAIL /rules/resolvedRegels/0/waarde/type: unit mismatch: dimension-cell
  read of 'vondstgewicht' is typed 't' of system 'Gewicht' (factor 1000
  to 'kg'), but the declaration it reads from is 'kg' of system 'Gewicht'
  (factor 1 to 'kg')
```

Since L2 that mutant has no wire form: a `type` on a `DimensionCellRead` is an
unknown field and is refused at load (the L2 load table in
`backends/clingo/test/test_clingo.ml`), and the negative
`wf091-dimensiecel-eenheid` left the corpus with that field
(`corpus/negative/README.md`). The other half, that the verifier does not
over-reject at that arm, has no arm left to witness: no unit or domain rule
crosses a cell read since L2. What stays is that the verifier passes the
cell read.

## The case — measured, per executor

`case1-voorjaarscel.json`, the reading example of `RUNTIME-SEMANTICS.md`
ch. 5.8c: J1 with the cells `voorjaar` 12.50 and `najaar` 4.00 gives
`voorjaarsgewicht` 12.50; J2 with the cell `najaar` 4.00 alone gives
`voorjaarsgewicht` leeg. The case pins no input cell, since an expectation on
one holds on the input as well and the no-op guard (gate 7) would let it pass.

| executor | outcome (2026-11-02) |
|---|---|
| `ref` (gate 5) | `oracle: PASS (3 expectations)` |
| `clingo` (gate 6) | `clingo: PASS (3 expectations)` |
| Drools (gate 12) | `PLAYED 1`, both branches |

Gate 10 still applies in full: `source.rgs` re-emits byte-identically to
the checked-in `ir.json`, so the source-level shape is pinned too.
