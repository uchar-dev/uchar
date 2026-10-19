# jutterswet-vuurtoren-flogic — the same counting quantifiers, authored in F-logic

The **second source twin** of `corpus/golden/jutterswet-vuurtoren`: the same
two object types, the same relation, the same five rules and the same five
cases with the same absolute values — but with an `ir.json` the F-logic
frontend (`frontends/flogic`) emitted from a `source.flogic`. The twin's
`ir.json` is hand-written, because no RegelSpraak source reaches this node
(the reason is in that golden's own NOTE); this one is emitted, so the
quantifier fold has a SOURCE LANGUAGE for the first time. The five
`case*.json` files are byte-identical to the twin's on purpose: **one set of
expectations, replayed against a hand-written document and an emitted one.**

Until this golden the fold's independence rested on its three executors
alone. It now also rests on two unrelated source languages reaching the same
node — the guard `corpus/golden/jutterswet-wrakrecht-flogic` gives the
subtype kernel, for the quantifier.

## Source (`source.flogic`)

The slice is a subset of Flora-2 2.2 syntax; `frontends/flogic/README.md`
carries the full table. What this document needs from it:

- the **filtered comprehension** `count{?V | ?V = ?T.lampen, ?V : gekeurd,
  ?V.sterkte > 5}`. Flora-2's aggregate body is a comma-separated
  conjunction of goals; the slice reads the first as the role hop, the LAST
  as the matrix, and everything between as the die/dat subselection. That
  position rule is the whole distinction, because F-logic has no relative
  clause to hang a filter on;
- the **count compared with its threshold**, which is what makes it a
  quantification: `>= 2` is `atLeast 2`, `<= 1` `atMost 1`, `= 1`
  `exactly 1`. No negation form appears anywhere in this document, and none is
  needed — which is why the slice's `\naf` / `\neg` refusals did not have to
  move for the quantifier to arrive;
- the **isa head** `?T : helder :- …`, Flora-2's own spelling for a class
  membership conclusion. A fold is a CONDITION, and until this card the slice
  could only write attribute values, so there was nothing for a fold to be the
  condition OF. A characteristic target stays a standing norm with its condition
  as the rule version's `condition`, and that is NOT because a characteristic
  cannot be over-determined — on the wire it can, since a defaults group carries
  a `characteristic` target and a default carries a `decision`, so attribute and
  do-not-attribute are two decisions a stack does order. It is because this
  SLICE has no do-not-attribute form (the negation forms are refused), so within
  F-logic two definitions of one characteristic cannot conflict; and because it
  is the shape the RegelSpraak frontend gives the same sentence — a conditional
  characteristic assignment lowers to a `Rule` there too, not to a group;
- `torenindeling : feittype.` with two role signatures — the relation
  reified as a class, the cardinality braces saying which end is to-many.

The bare `count{?V | ?V = ?T.lampen}` of `lampental` carries no goal beside
the hop, so it states no matrix and stays the plain instance fold: a
quantifier over no condition would violate WF-108 (d).

## The frontend diff, category by category

The method is the one the `-catala` and `-flogic` pairs used before: put the
two documents side by side and account for **every** difference. Against
`corpus/golden/jutterswet-vuurtoren/ir.json` there are **70 differing
lines**, counted on both sides of the diff (re-measured at L5, 2026-09-25;
67 at L2 and at L4), in three categories — 52 + 15 + 3 — and no fourth.
Until the v1 wire there were 89: the 20 `onderwerpBron` lines left the wire
at L2 (`spec/proposals/V1-WIREMIGRATIE.md` B11), and this document omits the
two object types' `plural` where it wrote `meervoud: null` (2 lines). At L5
the source record (delta B26) made each citation this document does not
write three lines on the twin's side instead of one differing line on each,
and the role's absent `plural` one line instead of two.

**(i) Source provenance — `source` (52 lines).**
Every such string differs, because each document quotes its own source:
`"de sterkte van ten minste 2 van de lampen van de Vuurtoren groter is dan
5"` against `"count{?V | ?V = ?T.lampen, ?V.sterkte > 5} >= 2"`, `"Een
Vuurtoren is helder"` against `"?T : helder"`, `"gekeurd zijn"` against
`"?V : gekeurd"`. No executor reads the field. One
sub-case is the **absence** of a citation rather than a different one (12
of those 52 lines): on each characteristic assignment's implied value the
twin quotes `"waar"` in a `source` where this document writes none, because
an isa head writes no lexeme for the value it gives. The `Literal` node
itself — the value `{"boolean": true}` at type `Boolean` — is identical in
both.

The member's own variable reaches the wire as `?V` whatever letter the
author chose, so one comprehension has one spelling; that is the same
normalisation the slice's aggregate rendering already did before the goals
existed.

**(ii) Declared surface names F-logic has no grammar for (15 lines: 2
object-type `plural`, the role's `plural` ×1 and `name` ×2, 10 `role` in the
range bindings).** The two object types carry no `plural`, and neither does
the to-many role. F-logic declares no grammatical number, so the
frontend leaves it unset rather than inventing it. The role is consequently
named by one label, `lampen`, where the twin carries the singular/plural pair
`lamp`/`lampen` — and the shared case files key on exactly that string, which
the twin matches on `Role.plural` and this document on `Role.name` (the
reference interpreter matches a role by either). The five range bindings — the
`sourceRole` of the four quantifier folds and of the instance fold — follow
the same label in their `role`. `animate` is `false` in both, so it is not a
difference here.

**(iii) The trailing `prioritizedDefaults` (3 lines).** This frontend's
serialiser always writes the array; the hand-written twin, which has no
defaults group, omits the key. Both are schema-valid and the loader reads
them identically.

**No semantic difference.** Everything else is byte-identical: the two object
types with the same attributes, characteristics and datatypes and the same
`kind` on every `Characteristic` (`kenmerkSoorten` until L1, B5); the fact
type with the same name and the same role fillers; the same five `Rule` forms
with the same names, the same `initialisation` and `presentation` and in the
same order; the same four `QuantifierFold` nodes with the same `quantifier`, the
same `threshold` where a counting quantifier has one and none where it would
contradict the quantifier (WF-108 (e)), the same `elementType`, the same
filter on `keurig` and none on the other three, the same matrix; and the same
`count` with the same `elementType` and `sourceRole.factType`. A semantic
difference here would be a defect in the lowering, not a finding about the
languages.

## The cells

Unchanged from the twin, and hand-computed there before any executor ran.
Bound 5, comparison strict; `count` is the number of lampen whose `sterkte`
exceeds 5, and `keurig` counts over the `gekeurd` survivors only.

| case | toren | lampen (sterkte, keuring) | lampental | count | helder (≥2) | zuinig (≤1) | enkelvoudig (=1) | keurig (≥2, gekeurd) |
|---|---|---|---|---|---|---|---|---|
| 1 | T1 | 8 gekeurd, 6 gekeurd | 2 | 2 | **ja** | nee | nee | **ja** (2) |
| 2 | T1 | 8 gekeurd, 3 gekeurd | 2 | 1 | nee | **ja** | **ja** | nee (1) |
| 3 | T1 | geen lampen | 0 | 0 | nee | **ja** | nee | nee (0) |
| 4 | T1 | 8 gekeurd, 9 NIET gekeurd, 3 gekeurd | 3 | 2 | **ja** | nee | nee | nee (1) |
| 4 | T2 | 9 gekeurd | 1 | 1 | nee | **ja** | **ja** | nee (1) |
| 5 | T1 | leeg gekeurd, 3 gekeurd | 2 | 0 | nee | **ja** | nee | nee (0) |

`asOfDate` is null in every case: nothing in this document is
time-dependent, which is what makes the matrix non-temporal — and the slice
has no timelines at all, so the whole-period matrix of the fold's other
witnesses stays out of F-logic's reach by construction.
