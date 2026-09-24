# jutterswet-wrakrecht-flogic — the same inheritance case, authored in F-logic

The **second source twin** of the hand-written anchor
`corpus/golden/anchor/jutterswet-strandrechtketen`, beside the RegelSpraak
twin `corpus/golden/jutterswet-wrakrecht`: the same three object types, the
same chain, the same fact type and the same four cases with the same
absolute values — but with an `ir.json` the F-logic frontend
(`frontends/flogic`) emitted from a `bron.flogic`. The anchor pins that all
three executors agree on inheritance; the two source twins pin that two
unrelated source languages reach exactly that IR. The four `case*.json`
files are byte-identical to the anchor's and to the RegelSpraak twin's on
purpose: **one set of expectations, replayed against a hand-written
document and two emitted ones.**

## Source (`bron.flogic`)

The slice is a subset of Flora-2 2.2 syntax; `frontends/flogic/README.md`
carries the full table. The four things this document needs from it:

- `Wrakrecht::Strandrecht.` — the single direct supertype;
- `Strandrecht[|grondslag => numeriek(niet_negatief, 2)|].` — the
  inheritable signature. The bars matter: the unbarred frame is the class
  object's own and refuses;
- `strandrechtregistratie : feittype.` with two role signatures — the
  relation reified as a class, the cardinality braces saying which end is
  to-many;
- Horn rules, where an unconditional rule is a target's base and a
  conditional one an exception above it. That shape is the whole priority
  device, because F-logic's own ordering operators (defeasible reasoning)
  are refused.

## The frontend diff, category by category

The method is the one Phase 5d used for the `-catala` pair: put the two
emitted documents side by side and account for **every** difference.
Against `corpus/golden/jutterswet-wrakrecht/ir.json` there are **39
differing lines**, counted on the twin's side of the diff (re-measured at L2,
2026-09-24), in three categories — 19 + 12 + 8 — and no fourth. Until the v1
wire there were 52: the 8 `onderwerpBron` lines left the wire at L2
(`spec/proposals/V1-WIREMIGRATIE.md` B11), and the 5 lines of category (iv)
left with the `regels` index (B3).

**(i) Source provenance — `bron` (19 lines).**
Every such string differs, because each document quotes its own source: `"zijn grondslag"`
against `"?S.grondslag"`, `"het Strandrecht betwist is"` against
`"?S : betwist"`, `"de som van … of 0,00 als die er niet zijn"` against
`"sum{?V | ?V = ?K.'geregistreerde strandrechten'.uitkering}"`. No
executor reads the field. One sub-case is the
**absence** of a citation rather than a different one: on each base's
vacuously true justification the twin quotes `"altijd"` where this document
writes `null`, because an unconditional Horn rule states no justification
to quote. The node itself — `ALLE` over no parts, the shape WF-050 asks
for — is identical in both.

**(ii) Literal spelling and the literal's own type annotation (12 lines:
4 `waarde`, 4 `geheel`, 4 `decimalen`).** `"5,00"` against `"5.00"`, and with it the annotation `geheel/decimalen` on the
literal node (`true/0` against `false/2`). The Dutch decimal comma is
RegelSpraak's lexeme; F-logic writes the point, and the frontend derives
the literal's scale from the lexeme it was given. This difference already
exists between the hand-written anchor and the RegelSpraak twin — the
anchor also spells `5.00` with scale 2 — and all four executors pin the
same absolute values across it. That is the effective-type law of
`RUNTIME-SEMANTICS.md` §2.10 doing its job: consumers derive from
declarations, never from the wire annotation, which is what gate 13
enforces mechanically.

**(iii) Declared surface names F-logic has no grammar for (8 lines:
3 object-type `plural`, 1 `animate`, the role's `naam` and `meervoud`, 2
`bronRol.rol`).** The three object types carry no `plural`, the to-many
role's `meervoud` is `null`, and `animate` is `false` on the Kustgemeente. F-logic declares neither grammatical number
nor animacy, so the frontend leaves them unset rather than inventing them.
The role is consequently named by one label, `geregistreerde
strandrechten`, where the RegelSpraak twin carries the singular/plural pair
— and the shared case files key on exactly that string, which the twin
matches on `Rol.meervoud` and this document on `Rol.naam` (the reference
interpreter matches a role by either). `bronRol.rol` follows the same
label. The shape "to-many with no declared plural" is not new here:
`corpus/golden/jutterswet-kaveltelling` already carries it and plays in
every executor.

**(iv) The order of the `regels` index (5 lines until the v1 wire).** The
index left the wire with `spec/proposals/V1-WIREMIGRATIE.md` B3, so this
category is empty now. It held seven entries with the same seven names and
the same `soort` in both, but the RegelSpraak twin listed all `Regel` heads
before all `Uitzondering` heads, while the F-logic frontend listed rules in
source order — where each exception follows the base it qualifies.

**No semantic difference.** Everything else is byte-identical: the three
object types with the same attributes, characteristics, datatypes and
`supertype`; the fact type with the same name and the same role fillers;
the same three `ResolvedRegel`s; the same two `PrioritizedDefaults` groups
with the same members in the same order (exception above base); the same
conditions; the same SOM and AANTAL with the same `elementObjecttype` and
the same `bronRol.feittype`. A semantic difference here would be a defect
in the lowering, not a finding about the languages.

## External executor: Flora-2 on the inheritance skeleton

A **note, never a gate and never a fourth leg of the differential.** The
comparison is limited to the inheritance skeleton by construction: F-logic
carries no units, no declared decimal scales, no timelines and no
allocation, so only the ordering of definitions is comparable, never the
amounts. The skeleton fixes `grondslag` at 10, which turns the two bases
into the class-level constants Flora-2 can hold:

```
Strandrecht[|uitkering -> 10|].
Wrakrecht::Strandrecht.
Wrakrecht[|uitkering -> 20|].
?S[uitkering -> 99] :- ?S : Strandrecht, ?S : betwist.
?W[uitkering -> 55] :- ?W : Wrakrecht,   ?W : diep.
```

Measured on the Flora-2/ErgoLite Reasoner 2.2 beta of 2022-08-05 (rev.
1203) over XSB 5.0, installed outside this repository:

| cell | instance | Flora-2 answers | uchar answers |
|---|---|---|---|
| 1 | Strandrecht | **10** | **10** |
| 2 | Strandrecht, betwist | **99** | **99** |
| 3 | Wrakrecht | **20** | **20** |
| 4 | Wrakrecht, betwist | **99** | **99** |
| 5 | Wrakrecht, diep | **55** | **55** |
| 6 | Wrakrecht, betwist and diep | **55 and 99**, two answers, no choice made | **55**, one answer |

Membership is transitive in Flora-2 as well (a Wrakrecht answers to both
class queries), which is the extensional reading the two aggregations
depend on.

Five of the six cells agree exactly, cells 3 and 4 among them — those two
are the penguin test, and they are the pair a flat lowering cannot produce
at once. On cell 6 Flora-2 returns both values silently: it has no conflict
channel and enforces no cardinality, so nothing there chooses. uchar's
single answer **55 lies inside that answer set**, and it is the value
Flora-2's own first principle — definitions in subclasses override
definitions in superclasses — selects wherever it selects at all. What
divides the two systems on this cell is determinism, not direction: uchar
answers once, by an order its own documents state, where Flora-2
accumulates.

Deliberately **not** part of this comparison: the variant that models the
exception as a second, incomparable *class* instead of a rule. There
Flora-2 blocks the value entirely — two incomparable parents, so
inheritance yields false — while uchar answers, because its priority comes
from stack position and not from specificity on a class lattice. That is a
real structural divergence, but it is not a cell of this use case: uchar
admits no second parent at all (WF-106), so the shape has no F-logic source
this frontend would accept. It stays where it was measured, in the design
document's own comparison.
