# jutterswet-opgave-datum — the opgave marker on a date

Exercises the RegelSpraak dialect's **opgave marker**
(`frontends/regelspraak/grammar/DIVERGENCES.md` §7) on an attribute of datatype
`Datum in dagen`: `(mag opgegeven worden)` as the last element of the
declaration, lowered by the frontend to the hardship pattern of
`spec/proposals/PHASE4-DEFAULTS.md` §4.1 on structure the wire already
carries — an input attribute for the opgave, the authored computation moved to
an attribute of its own, one defaults group with the opgave above the
computation and the authored initialisation below it, and a characteristic plus
a consistency that record a deviating opgave on the §9.5 channel. The marked
date is computed from another date and initialised with a date literal; a rule
reads the generated characteristic and a computation reads the marked date.
Own Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 3f (dag van aanmelding).** 1. De dag van aanmelding van een vondst is
> de derde dag na de vinddag. 2. Is de vinddag niet vastgesteld, dan geldt als
> dag van aanmelding 1 maart 2026, de eerste dag van het jutseizoen. 3. De
> jutter kan bij de aanmelding de dag van aanmelding opgeven; een opgegeven dag
> gaat voor de berekende. 4. Wijkt de opgegeven dag af van de berekende, dan
> stelt de strandvoogd de vondst ter controle. 5. De vondst kan worden
> afgehaald tot en met de dertigste dag na de dag van aanmelding.

## Source (`bron.rgs`)

- `meldingsdatum` of a Vondst, a `Datum in dagen`, carries the marker (lid 3).
- **melding na de vinddag** (lid 1): `meldingsdatum` = `vinddag` plus 3 dagen.
- **melding bij onbekende vinddag** (lid 2): `meldingsdatum` geïnitialiseerd
  op 01-03-2026.
- **controle bij afwijkende meldingsdatum** (lid 4): a Vondst is `ter
  controle` indien hij `afwijkende opgave meldingsdatum` heeft — a rule reading
  a characteristic the marker generates.
- **afhaaltermijn** (lid 5): `uiterste afhaaldatum` = `meldingsdatum` plus 30
  dagen — a computation reading the marked date, so it reads the resolved value.

Lid 1 carries no condition, unlike the weight rule of
`jutterswet-opgave-boven-berekening`, which needs "indien zijn gewicht gevuld
is" because `maal` counts an empty operand as 0. An empty date propagates
through `plus` (`RUNTIME-SEMANTICS.md` ch. 1.2: "leeg + 5 dagen = leeg"), so
without a vinddag the computation decides nothing and the initialisation below
it is reached. Measured on 2026-09-28, before this golden was written, on lid 1
alone without the marker and a Vondst without a vinddag, expecting
`meldingsdatum` empty: ref `oracle: PASS (2 expectations)`, clingo `clingo:
PASS (2 expectations)`, the Drools exec branch `PLAYED 1`. case4, case5 and
case7 below pin the same under the marker.

## Lowering (the frontend's)

| Piece | Wire |
|---|---|
| 1 | attribute `opgegeven meldingsdatum` on Vondst, `Date` with precision `day` like `meldingsdatum`, right after it; no writer, the case supplies it |
| 2 | attribute `berekende meldingsdatum`, the same datatype, right after `opgegeven meldingsdatum`; the rule `melding na de vinddag` keeps its name and statute annotation and now targets `berekende meldingsdatum` |
| 3 | `PrioritizedDefaults` `opgave boven berekening meldingsdatum` on `meldingsdatum`, validity always, from the top: the opgave default (justification the empty `all`, source `altijd`; consequence `opgegeven meldingsdatum`; source `opgegeven meldingsdatum`), the computation default (the same with `berekende meldingsdatum`), and the initialisation default (`initialisation: true`, consequence the date literal 2026-03-01, source the statute annotation `art. 3f lid 2`) — the initialisation stays on `meldingsdatum` and leaves `rules` |
| 4 | possessive characteristic `afwijkende opgave meldingsdatum`, attributed by the rule `afwijking opgave meldingsdatum` under an `all` of: `opgegeven meldingsdatum` filled, `berekende meldingsdatum` filled, `opgegeven meldingsdatum` notEqual `berekende meldingsdatum` — on two dates as on two numbers, and spelled as the frontend emits the same rule written by hand |
| 5 | consistency `controle opgave meldingsdatum`, assertion the characteristic check of `afwijkende opgave meldingsdatum`, negated; its source is the marked declaration and its line |

The generated characteristic follows `ter controle`; the generated rule, group
and consistency follow the authored ones in their lists.

## Case derivations (hand-computed, absolute)

Per case the highest default that decides wins: the opgave, else the
computation, else the initialisation, whose justification is "`meldingsdatum` is
empty in the input facts" (ch. 7.3). A default whose consequence is empty falls
through (ch. 7.1). When nothing decides, `meldingsdatum` keeps what the input
holds (ch. 7.8, Reading A). The characteristic needs both dates filled and
unequal; the consistency deposits one fact `controle opgave meldingsdatum` × V
exactly when the characteristic holds. `uiterste afhaaldatum` reads the
resolved `meldingsdatum`.

The date sums, counted in days: 10-03-2026 plus 3 = 13-03-2026; 13-03-2026
plus 30 = 12-04-2026 (18 days to 31 March, 12 into April); 12-03-2026 plus 30 =
11-04-2026; 01-03-2026 plus 30 = 31-03-2026; 20-03-2026 plus 30 = 19-04-2026
(11 days to 31 March, 19 into April).

| Case | Input | opgave | computation | initialisation | meldingsdatum | berekende meldingsdatum | afwijkende opgave / ter controle | uiterste afhaaldatum | channel |
|---|---|---|---|---|---|---|---|---|---|
| case1-geen-opgave | vinddag 10-03-2026 | empty | 13-03-2026 | not reached | **13-03-2026** | 13-03-2026 | absent / absent | **12-04-2026** | consistent |
| case2-opgave-gelijk | vinddag 10-03-2026, opgegeven 13-03-2026 | 13-03-2026 | 13-03-2026 | not reached | **13-03-2026** | 13-03-2026 | absent (equal) / absent | **12-04-2026** | consistent |
| case3-opgave-wijkt-af | vinddag 10-03-2026, opgegeven 12-03-2026 | 12-03-2026 | 13-03-2026 | not reached | **12-03-2026** | 13-03-2026 | **present / present** | **11-04-2026** | `controle opgave meldingsdatum` × V |
| case4-opgave-zonder-feiten | opgegeven 12-03-2026 | 12-03-2026 | empty (the date propagates) | not reached | **12-03-2026** | empty | absent (no computation) / absent | **11-04-2026** | consistent |
| case5-niets | — | empty | empty | 01-03-2026 (`meldingsdatum` empty in the input) | **01-03-2026** | empty | absent | **31-03-2026** | consistent |
| case6-doel-zelf-in-invoer | vinddag 10-03-2026, meldingsdatum 20-03-2026 | empty | 13-03-2026 | not reached | **13-03-2026** (the group decides; the input is displaced) | 13-03-2026 | absent | **12-04-2026** | consistent |
| case7-doel-zelf-zonder-feiten | meldingsdatum 20-03-2026 | empty | empty | justification fails: `meldingsdatum` is filled in the input | **20-03-2026** (nothing decides; the input stands) | empty | absent | **19-04-2026** | consistent |

case1 to case4 also pin `ter controle`: present in case3 alone, where the
opgave deviates. The pin on `uiterste afhaaldatum` shows the reader reading the
resolved date — the opgave in case3 and case4, the initialisation in case5, the
input in case7 — and it makes every case refute the do-nothing executor: no
case supplies `uiterste afhaaldatum`. For case7 that pin is the only one the raw
input does not already satisfy.

## Scenario document (`scenario.rgs`)

The seven cases are written as seven scenarios, one instance `V` each, and the
expectations above are their expectation bullets. The frontend's `--cases`
mode lowers `bron.rgs` with `scenario.rgs` to the seven case files and to the
`ir.json` of the production route, and gate 16 regenerates them byte for byte.
The scenario layer carries every expectation here: a date value, an empty
attribute, a characteristic present and absent, consistent, and the
inconsistency with its rule and label (`de gegevens zijn inconsistent volgens
"controle opgave meldingsdatum" voor V`).

## Executors

ref and clingo replay all seven cases exactly; the Drools exec branch plays all
seven (`PLAYED 7`) with the same values, the date literal of the
initialisation default among them; the DRL-text branch refuses the document on
its defaults sentence (`EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text
branch has no defaults lowering`), as it refuses every defaults document (class
(A) of `DRL_CHANNEL` in `scripts/check-drools.sh`). Fifteen flipped
counter-cases, each one pinned expectation of a case turned to its opposite and
kept outside the repository, fail in all three: case1 `meldingsdatum`
10-03-2026 and `uiterste afhaaldatum` 09-04-2026, case2 inconsistent, case3
consistent, case3 `meldingsdatum` 13-03-2026, case3 without the characteristic,
case3 without `ter controle`, case3 `uiterste afhaaldatum` 12-04-2026, case4
with the characteristic, case4 `meldingsdatum` 01-03-2026, case5
`meldingsdatum` empty and `uiterste afhaaldatum` empty, case6 `meldingsdatum`
20-03-2026, case7 `meldingsdatum` 01-03-2026 and `uiterste afhaaldatum`
31-03-2026.

Under the no-op guard (gate 7) no case survives.
