# jutterswet-stukopbrengst

The **eenheid-identiteit** witness (arbiter *typeringen* v2.3.0 Tabel 11 rij 4 +
Tabel 13, ratified 2026-08-29 as besluit 16Y). It is the public golden that
pins the multiplicative unit-identity: the dimensionless "1" is the group
identity and **absorbs** under multiplication, so a unitless operand times a
unit-carrying operand carries that unit — `(none) maal Eenheid1 -> Eenheid1`,
with the result decimals the SUM of the operands' decimals.

## The typing here

`opbrengst := basisbedrag + stuktal × stukprijs`. The attribute name is
`stuktal`, NOT "aantal stukken" — the latter parses as the §13.4.17 count
function ("het aantal … van"); `stuktal` is an ordinary unitless integer
attribute, so the product is a genuine `stuktal (geen eenheid) × stukprijs (€)`.

`Typing.compute` now types that product as `Numeriek` with the EUR unit
(Tabel 11 rij 4), unconditionally — literal OR attribute left (the old
literal/attribute split retired). The outer `+` then adds two EUR values into
the EUR `opbrengst` target, unit-clean. There is **no `Onbekend` on the wire**:
before besluit 16Y this exact shape degraded to
`Onbekend("samengestelde eenheid")` on the product and `Onbekend("operand")` on
the sum — a typing INABILITY the Eenheid model could not name. That degradation
is gone; the product now names its unit.

## Hand-computed absolutes

| Vondst | basisbedrag | stuktal | stukprijs | opbrengst           |
|--------|-------------|---------|-----------|---------------------|
| V1     | 40 €        | 3       | 50 €      | 40 + 3·50 = **190 €** |
| V2     | 12 €        | 5       | 20 €      | 12 + 5·20 = **112 €** |

## The differential square

`ref` ≡ `clingo` over every expectation (gate 5 ≡ gate 6), anchored to these
hand-written absolutes. The Drools backend PLAYS it too (gate 12: `PLAYED`), so
the full square agrees — ref ≡ clingo ≡ Drools. NOOP-REFUTING: `opbrengst` is
computed and absent from the raw input, so a do-nothing run mismatches it
(gate 7).
