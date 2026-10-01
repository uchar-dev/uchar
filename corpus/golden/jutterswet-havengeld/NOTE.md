# jutterswet-havengeld — the percentage of a navigated read, without the '%'

Exercises "het percentage P van Y" where P is a Percentage parameter written
without the percentage sign and Y is no own attribute of the rule subject
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "The percentage forms"):
Y navigated over a role, Y between parentheses, and Y a variable of a
"Daarbij geldt" part. Each lowers to the `percentageOf` node the '%' form
already emits, with P a `ParameterReference`. Own Jutterswet-flavoured casus,
own amounts, rights-free.

## Casus (own text)

> **Art. 3i (havengeld, kadegeld en toeslag).** 1. Voor een vondst is
> havengeld verschuldigd: het percentage havengeld van het taxatiebedrag van
> de taxatie van de vondst, naar beneden afgerond op twee decimalen. 2. Voor
> een vondst is kadegeld verschuldigd: het percentage kadegeld van de som van
> het taxatiebedrag en het herstelbedrag van de taxatie, naar beneden
> afgerond op twee decimalen. 3. Het meerbedrag is het taxatiebedrag
> verminderd met de drempel; is het meerbedrag groter dan nul, dan is een
> toeslag verschuldigd van het percentage toeslag van het meerbedrag, naar
> beneden afgerond op twee decimalen. 4. De havenmeester stelt de drie
> percentages en de drempel vast.

## Source (`bron.rgs`)

- **havengeld** (lid 1): "het percentage havengeld van het taxatiebedrag van
  de taxatie van de Vondst" — Y read one step from the Vondst over the role
  `taxatie`. The grammar reads the whole phrase as one attribute read, whose
  name runs over every inner 'van'; the resolver re-reads it as the
  percentage function because its first name part is a Percentage parameter,
  and reads Y from every remaining name part and the subject.
- **kadegeld** (lid 2): "het percentage kadegeld van (… plus …)" — Y between
  parentheses, a sum of two navigated reads; the parenthesised form is
  `primaireExpressie`'s last alternative.
- **toeslag** (lid 3): "het percentage toeslag van het meerbedrag" with
  "Daarbij geldt: het meerbedrag is het taxatiebedrag van de taxatie van de
  Vondst verminderd met de drempel" — Y a variable, a `BinderRef` under the
  `Let` the variable part lowers to; the condition reads the variable too.

The amounts carry no unit. With a unit (€) on the amounts, the frontend, ref
and both Drools branches carry the same three forms (measured 2026-10-02),
and clingo refuses a percentage of an amount with a unit loudly ("a
unit-carrying multiplicative operand is not translated"), so a golden with a
unit would not replay in the differential square. The amounts stay below 200:
clingo grounds in machine integers and refuses a larger percentage product
loudly ("arithmetic range exceeded"; measured on 1234,57 without a unit).

## Hand computation

Parameterset `havengelden`: percentage havengeld 12,5 %, percentage kadegeld
4 %, percentage toeslag 20 %, drempel 100,00. `percentageOf` is `P/100 × Y`,
exactly; an empty operand counts as 0 (RUNTIME-SEMANTICS ch. 1.2), as it
does for `plus`; `verminderd met` propagates an empty left operand; a read
over an absent link is empty (ch. 5.8).

| case | taxatiebedrag | herstelbedrag | havengeld | kadegeld | meerbedrag | toeslag |
|---|---|---|---|---|---|---|
| case1-boven-de-drempel | 120,00 | 30,00 | 12,5 % × 120,00 = **15,00** | 4 % × 150,00 = **6,00** | 20,00 | 20 % × 20,00 = **4,00** |
| case2-onder-de-drempel-met-afronding | 75,30 | 12,46 | 9,4125 → **9,41** | 4 % × 87,76 = 3,5104 → **3,51** | −24,70, not > 0 | **leeg** |
| case3-op-de-drempel-zonder-herstelbedrag | 100,00 | — | **12,50** | 4 % × (100,00 + leeg) = **4,00** | 0,00, not > 0 | **leeg** |
| case4-zonder-taxatie | (no taxatie) | — | 12,5 % × leeg = **0,00** | 4 % × (leeg + leeg) = **0,00** | leeg | **leeg** |

## Executors

Measured on 2026-10-02: ref replays the four cases exactly (3 expectations
each), clingo the same, the Drools exec-model branch plays them (`PLAYED 4`),
and so does the DRL-text branch (`PLAYED 4`). Every case refutes the
do-nothing executor, and twelve counter-cases, each one expectation moved by
0,01 or an empty expectation turned into 1, fail in ref, clingo and the exec
branch. The IR from `bron.rgs` alone is byte-identical to the one written
beside the cases from `bron.rgs` with `scenario.rgs`. Before the frontend
read these forms (uchar `921fcc64`), each of the three rules was refused: the
navigated read with an ERROR that no member "percentage havengeld van het
taxatiebedrag" exists on Taxatie, the parenthesised operand with a syntax
error, and the variable with an ERROR that "het meerbedrag" names no role
from Vondst.
