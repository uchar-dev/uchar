# jutterswet-vondsttarief

The **frontend bare-%-literal** witness. This golden's `source.rgs` uses a
BARE percentage literal (`21%`, no `van`) in two value positions the grammar
admits, so uchar's own frontend (`getalWaarde : NUMBER PERCENT_SIGN`,
RegelSpraak.g4:973; resolver `ExpressionBuilder.java:200`) emits a
**`Literal` typed as a percentage** (a `Number` with the unit `%`) — the
round-trip gate 10 never exercised before (the two existing %-goldens use `%`
only as the left operand of `percentageOf`, which the frontend types as a
plain `Number`).

Chosen grammar positions (both admitted, verified by a quick emit test):

1. **berekening RHS** (`gelijkstelling : attribuutVanOnderwerp
   WORDT_BEREKEND_ALS expressie`): `Het bergingstarief van een Vondst moet
   berekend worden als 21%.` →
   `Literal{value: {"number": "21"}, type: Number with unit %}`.
2. **comparison RHS** (`vergelijkingsTekenGesplitst : GELIJK IS AAN`):
   `… indien het vondsttarief van de Vondst gelijk is aan 21%.` →
   the comparison's `right` is
   `Literal{value: {"number": "21"}, type: Number with unit %}`.

The frontend types a `Percentage (geheel getal)` attribute and a bare `21%`
literal with the `%` unit (abbreviation `%`, canonical `%`, factor 1/1); both
sides of the comparison and the assignment carry the SAME unit, so every
unit restatement is the identity.

## Percentage semantics used (spec, NOT copied from an executor)

`spec/RUNTIME-SEMANTICS.md` ch. 2.2 (Operators, l.288) defines the `/100`
scaling as a property of the **`percentage van` operator** only (`l/100 × r`,
exactly). No operator applies here: `21%` stands alone in a value position,
so nothing scales it. A **bare** percentage value therefore carries no
implicit `/100` — its magnitude is the per-hundred number itself. So `21%`
standing alone denotes the value **21** (in `%` units), NOT `0,21`.

This value is derived from the spec, not read off an executor. ref (gate 5)
and clingo (gate 6) independently reproduce **21**, which is confirmation of
the hand computation, never its source.

- **berekening** `bergingstarief := 21%`: value **21**. Assignment gate
  (ch. 2.4): literal precision 0 dec = target precision 0 dec (geheel) →
  no rounding, representable → writes **21**. Independent of any input.
- **comparison** `vondsttarief = 21%` (ch. 5.6, inequality/equality Tabel 44,
  both operands gevuld, identical `%` unit): true iff `vondsttarief` = 21.

## Cases (`case.json`)

| instance | vondsttarief (input) | bergingstarief (computed) | standaardbelast |
|----------|----------------------|---------------------------|-----------------|
| V1       | 21                   | 21                        | **present** (21 = 21) |
| V2       | 9                    | 21                        | **absent**  (9 ≠ 21)  |

No consistency rule fires → `consistent`.

## noop refutation (gate 7)

`bergingstarief` and `standaardbelast` are absent from the input world, so
the `--noop` run (expectations against the raw input, no evaluation) fails
on V1/V2 `bergingstarief = 21` and on V1 `standaardbelast present` — the case
refutes a do-nothing executor. Not a survivor.

## Differential square + gate 12 (Drools) auto-exclusion

ref (gate 5) ≡ clingo (gate 6) on the pinned values above.

**Drools (gate 12) auto-excludes this golden** — a channel-1 per-form
transpiler SKIP, printed loudly by the CLI, NOT a value-divergence and so
NOT listed in `scripts/check-drools.sh` (that script is untouched). The CLI
prints exactly ONE line: the transpiler short-circuits on the first regel it
cannot lower (the berekening, processed first), so that message alone is the
exclusion reason:

```
EXCLUDE transpiler SKIP: regel: bergingstarief van een vondst — regel (waarde buiten scope): bergingstarief van een vondst
```

The frontend types a `Percentage (geheel getal)` attribute and a bare `21%`
literal with the `%` unit (see `ir.json`). The berekening's scalar-value
lowering (`ExecModelTranspiler.java:553`, `waarde buiten scope`) declines the
`%`-unit operand and returns the skip above; transpilation stops
there. The comparison rule's Phase-B condition lowering (`:306`, `voorwaarde
buiten Phase-B scope`) would independently decline the same `%`-unit operand,
but it is never reached or printed in this golden — it is why the golden could
not play even if the berekening were carried, not a message the CLI emits here.

EVIDENCE that the divergence is exactly the percentage (`%`-unit) form and
nothing else: the byte-identical shape with the operands retyped as numbers
without `%` PLAYS+PASSES through the same Drools jar — `jutterswet-strandgoed`
(a `>` comparison of numbers without a unit) plays, and a minimal `… gelijk
is aan 21` (bare number equality, `%` stripped) also plays `PLAYED 1`. The bare
percentage literal is intrinsically typed as a percentage (RegelSpraak.g4:973 →
`ExpressionBuilder.java:200`), so no bare-`%` witness can both carry the
`%`-lexeme AND be lowered by the v0 Drools exec-model — the auto-exclusion is
unavoidable for this construct, and self-documenting.
