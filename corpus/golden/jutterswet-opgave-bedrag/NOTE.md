# jutterswet-opgave-bedrag — the opgave marker on an amount with a unit

Exercises the RegelSpraak dialect's **opgave marker**
(`frontends/regelspraak/grammar/DIVERGENCES.md` §7) on an attribute of
datatype Numeriek with one unit, an amount in €: `(mag opgegeven worden)` as
the last element of the declaration, lowered by the frontend to the hardship
pattern of `spec/proposals/PHASE4-DEFAULTS.md` §4.1 on structure the wire
already carries — an input attribute for the opgave, the authored computation
moved to an attribute of its own, one defaults group with the opgave above the
computation and the authored initialisation below it, and a characteristic plus
a consistency that record a deviating opgave on the §9.5 channel. The marked
amount is computed by an addition in the same unit and initialised with an
amount; a rule reads the generated characteristic and a computation reads the
marked amount. Own Jutterswet-flavoured casus, own amounts, rights-free.

## Casus (own text)

> **Art. 3k (waarde van een vondst).** 1. De waarde van een vondst is de
> taxatie vermeerderd met de bergingskosten, als de taxatie bekend is. 2. Is
> de waarde niet anders vastgesteld, dan bedraagt zij 25,00 €. 3. De vinder kan
> de waarde opgeven; een opgegeven waarde gaat voor de berekende. 4. Wijkt de
> opgegeven waarde af van de berekende, dan stelt de strandvoogd de vondst ter
> controle. 5. De verzekerde waarde van een vondst is de waarde vermeerderd met
> 2,50 €.

## Source (`source.rgs`)

- `waarde` of a Vondst, a Numeriek with the unit € (declared as `EUR` with the
  symbol €), carries the marker (lid 3).
- **waarde** (lid 1): `waarde` = `taxatie` plus `bergingskosten`, indien de
  taxatie gevuld is — an addition in one unit, no product. `plus` counts an
  empty operand as 0 (RUNTIME-SEMANTICS ch. 1.2), so without the condition the
  computation would decide 0,00 € for a Vondst without facts and the
  initialisation below it would never be reached.
- **waarde bij onbekende taxatie** (lid 2): `waarde` geïnitialiseerd op
  25,00 €.
- **controle bij afwijkende waarde** (lid 4): a Vondst is `ter controle`
  indien hij `afwijkende opgave waarde` heeft — a rule reading a characteristic
  the marker generates.
- **verzekerde waarde** (lid 5): `verzekerde waarde` = `waarde` plus 2,50 € —
  a computation reading the marked amount, so it reads the resolved value.

## Lowering (the frontend's)

As for a number without a unit (§7, "Lowering"): `opgegeven waarde` and
`berekende waarde` right after `waarde`, each of `waarde`'s datatype, the unit
€ included; the rule `waarde` keeps its name and statute annotation and targets
`berekende waarde`; the group `opgave boven berekening waarde` on `waarde`
holds, from the top, the opgave, the computation and the initialisation default
with the amount 25,00 € (whole, unit `EUR`); the possessive characteristic
`afwijkende opgave waarde`, attributed by the rule `afwijking opgave waarde`
where opgave and computation are both filled and differ, compared in the one
unit; the consistency `controle opgave waarde` asserting it absent. The opgave
and the computation share the unit of `waarde`: a computation or a case input
in another unit of the same system is refused by the typing of an assignment
("toekenning: assignment across incompatible units"), as without the marker.

## Hand computation

| Case | Input | waarde | berekende waarde | afwijkende opgave | ter controle | verzekerde waarde | consistency |
|---|---|---|---|---|---|---|---|
| case1-geen-opgave | taxatie 100,00 €, kosten 10,00 € | **110,00 €** | 110,00 € | absent | absent | **112,50 €** | consistent |
| case2-opgave-gelijk | as case1, opgave 110,00 € | **110,00 €** | 110,00 € | absent | absent | **112,50 €** | consistent |
| case3-opgave-wijkt-af | as case1, opgave 95,00 € | **95,00 €** (opgave) | 110,00 € | present | present | **97,50 €** | inconsistent: `controle opgave waarde` × V |
| case4-alleen-opgave | opgave 50,00 € | **50,00 €** | leeg | absent | absent | **52,50 €** | consistent |
| case5-niets | — | **25,00 €** (initialisation) | leeg | absent | — | **27,50 €** | consistent |
| case6-doel-zelf-in-invoer | taxatie 100,00 €, kosten 10,00 €, waarde 80,00 € | **110,00 €** (the computation displaces the input, Reading A) | 110,00 € | absent | — | **112,50 €** | consistent |
| case7-doel-zelf-zonder-feiten | waarde 80,00 € | **80,00 €** (the input stays: nothing above it decides) | leeg | absent | — | **82,50 €** | consistent |

## Executors

Measured on 2026-10-02: ref replays the seven cases exactly (6, 6, 6, 6, 5, 5
and 5 expectations), clingo the same, and the Drools exec-model branch plays
them (`PLAYED 7`); the DRL-text branch refuses the document on its defaults
sentence. Every case refutes the do-nothing executor, and 32 counter-cases,
each one expectation moved (an amount by 0,01, a characteristic turned, an
empty expectation turned into a value), fail in ref, clingo and the exec
branch. The IR from `source.rgs` alone is byte-identical to the one written
beside the cases. Before the frontend admitted the marker on a number with a
unit (uchar `921fcc64`), the declaration was refused with the ERROR "…
'(mag opgegeven worden)' staat alleen op een Numeriek attribuut zonder eenheid
of een Datum in dagen, en 'waarde' is Numeriek met eenheid € — …"; the
lowering written by hand in € (without the initialisation, which the dialect
does not let an author write beside an `Afwijking van` group) played in ref,
clingo and the exec branch then.
