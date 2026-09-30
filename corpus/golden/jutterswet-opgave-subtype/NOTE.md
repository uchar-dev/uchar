# jutterswet-opgave-subtype — the opgave marker with writers on subtypes

Exercises the RegelSpraak dialect's **opgave marker**
(`frontends/regelspraak/grammar/DIVERGENCES.md` §7) on a date declared on a
supertype and computed by rules on its subtypes: `(mag opgegeven worden)` on
`meldingsdatum` of a Vondst, while the rules that derive it stand on the
Strandvondst and on the Barnsteenvondst, the types that know their operands.
The frontend moves every computing writer to `berekende meldingsdatum` on its
own type, so the writers of `berekende meldingsdatum` compose per concrete type
by `spec/RUNTIME-SEMANTICS.md` ch. 7.10 as the writers of `meldingsdatum` do
without the marker; the one generated defaults group stands on the Vondst, and
every subtype inherits it whole, so the opgave is the highest default of every
type's stack. A rule on the subtype reads the generated characteristic, another
reads `berekende meldingsdatum`, and a computation on the Vondst reads the
marked date. Own Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 3g (aanmelding van een strandvondst).** 1. Een strandvondst wordt
> aangemeld op de tweede dag na de dag waarop zij is aangespoeld. 2. Is de
> strandvondst beschadigd, dan wordt zij aangemeld op de dag waarop zij is
> aangespoeld. 3. Een barnsteenvondst wordt, in afwijking van het eerste lid,
> aangemeld op de dag van keuring. 4. Is geen dag van aanmelding vastgesteld,
> dan geldt 1 maart 2026, de eerste dag van het jutseizoen. 5. De jutter kan
> bij de aanmelding de dag van aanmelding opgeven; een opgegeven dag gaat voor
> de vastgestelde. 6. Wijkt bij een strandvondst de opgegeven dag af van de
> vastgestelde, dan stelt de strandvoogd de strandvondst ter controle, met de
> vastgestelde dag als controledag. 7. De vondst kan worden afgehaald tot en
> met de dertigste dag na de dag van aanmelding.

## Source (`bron.rgs`)

Three types in one chain: `Barnsteenvondst is een Strandvondst`,
`Strandvondst is een Vondst`.

- `meldingsdatum` of a Vondst, a `Datum in dagen`, carries the marker (lid 5).
- **melding na het aanspoelen** (lid 1), on the Strandvondst: `meldingsdatum` =
  `aanspoeldag` plus 2 dagen.
- **melding bij schade** (lid 2), `Uitzondering op` lid 1, on the Strandvondst:
  `meldingsdatum` = `aanspoeldag` indien hij beschadigd is.
- **melding na de keuring** (lid 3), on the Barnsteenvondst: `meldingsdatum` =
  `keurdag`, no condition.
- **melding bij onbekende dag** (lid 4), on the Vondst: `meldingsdatum`
  geïnitialiseerd op 01-03-2026.
- **controle bij afwijkende meldingsdatum** (lid 6), on the Strandvondst: `ter
  controle` indien hij `afwijkende opgave meldingsdatum` heeft — a subtype rule
  reading the characteristic the marker generates on the Vondst.
- **controledag bij afwijkende meldingsdatum** (lid 6), on the Strandvondst:
  `controledag` = `berekende meldingsdatum` indien hij ter controle is — a
  subtype rule reading the generated attribute.
- **afhaaltermijn** (lid 7), on the Vondst: `uiterste afhaaldatum` =
  `meldingsdatum` plus 30 dagen — it reads the resolved date.

An empty `aanspoeldag` propagates through `plus` (ch. 1.2), and lid 3 without a
`keurdag` evaluates empty: a default whose consequence is empty decides nothing
(ch. 7.1), so lid 1 and lid 3 need no condition.

## Lowering (the frontend's)

| Piece | Wire |
|---|---|
| 1 | attribute `opgegeven meldingsdatum` on Vondst, `Date` with precision `day`, right after `meldingsdatum`; no writer, the case supplies it |
| 2 | attribute `berekende meldingsdatum` on Vondst, the same datatype, right after it. Lid 1 and lid 2 become the defaults group `melding na het aanspoelen` on **Strandvondst**, lid 3 stays a rule on **Barnsteenvondst**; both now write `berekende meldingsdatum`, each keeping its type, its name, its shape and its statute annotation |
| 3 | `PrioritizedDefaults` `opgave boven berekening meldingsdatum` on **Vondst**.`meldingsdatum`, validity always, from the top: the opgave default (justification the empty `all`, consequence `opgegeven meldingsdatum`), the computation default (the same with `berekende meldingsdatum`), and the initialisation default (`initialisation: true`, the date literal 2026-03-01, source `art. 3g lid 4`); lid 4 leaves `rules` |
| 4 | possessive characteristic `afwijkende opgave meldingsdatum` on Vondst, attributed by the rule `afwijking opgave meldingsdatum` on Vondst under an `all` of: `opgegeven meldingsdatum` filled, `berekende meldingsdatum` filled, the two `notEqual` |
| 5 | consistency `controle opgave meldingsdatum` on Vondst, assertion the characteristic check of `afwijkende opgave meldingsdatum`, negated; its source is the marked declaration and its line |

No subtype writes `meldingsdatum` after the lowering, so no subtype declares
anything on it: the Strandvondst and the Barnsteenvondst inherit the group on
the Vondst whole.

## The stacks per concrete type (ch. 7.10)

`meldingsdatum`, every type: the opgave, then `berekende meldingsdatum`, then
the initialisation 01-03-2026 (justified where `meldingsdatum` is empty in the
input), then the virtual input default.

`berekende meldingsdatum`, highest first:

| concrete type | stack | reason |
|---|---|---|
| Vondst | — | no writer: always empty |
| Strandvondst | lid 2 (`aanspoeldag` if beschadigd), lid 1 (`aanspoeldag` plus 2) | its own group |
| Barnsteenvondst | lid 2 (inherited), lid 3 (`keurdag`) | lid 3 is the Barnsteenvondst's own base and replaces the inherited base lid 1; the inherited exception lid 2 still outranks it |

Each default is read on the level that declared it: lid 2 reads the
`aanspoeldag` and `beschadigd` of the Barnsteenvondst instance as a
Strandvondst.

## Case derivations (hand-computed, absolute)

Per case the highest default that decides wins. A default whose consequence is
empty falls through (ch. 7.1). When nothing decides, `meldingsdatum` keeps what
the input holds (ch. 7.8, Reading A). The characteristic needs both dates
filled and unequal; the consistency deposits one fact `controle opgave
meldingsdatum` × the instance exactly when the characteristic holds. `ter
controle` follows the characteristic; `controledag` is `berekende
meldingsdatum` where the instance is ter controle.

The date sums, counted in days: 10-03-2026 plus 2 = 12-03-2026; plus 30:
12-03 → 11-04-2026 (19 days to 31 March, 11 into April), 10-03 → 09-04-2026,
15-03 → 14-04-2026, 20-03 → 19-04-2026, 18-03 → 17-04-2026, 01-03 →
31-03-2026, 07-03 → 06-04-2026.

| Case | Instance and input | berekende meldingsdatum | meldingsdatum | afwijkende opgave / ter controle / controledag | uiterste afhaaldatum | channel |
|---|---|---|---|---|---|---|
| case1-strandvondst-berekend | Strandvondst, aanspoeldag 10-03-2026 | 12-03-2026 (lid 1) | **12-03-2026** | absent / absent / empty | **11-04-2026** | consistent |
| case2-strandvondst-beschadigd | Strandvondst, aanspoeldag 10-03-2026, beschadigd | 10-03-2026 (lid 2) | **10-03-2026** | absent | **09-04-2026** | consistent |
| case3-strandvondst-opgave-wijkt-af | Strandvondst, aanspoeldag 10-03-2026, opgegeven 15-03-2026 | 12-03-2026 | **15-03-2026** (the opgave) | **present / present / 12-03-2026** | **14-04-2026** | `controle opgave meldingsdatum` × S |
| case4-strandvondst-opgave-gelijk | Strandvondst, aanspoeldag 10-03-2026, opgegeven 12-03-2026 | 12-03-2026 | **12-03-2026** | absent (equal) / absent / empty | **11-04-2026** | consistent |
| case5-barnsteenvondst-eigen-basis | Barnsteenvondst, aanspoeldag 10-03-2026, keurdag 20-03-2026 | 20-03-2026 (lid 3; lid 1 replaced) | **20-03-2026** | absent | **19-04-2026** | consistent |
| case6-barnsteenvondst-geerfde-uitzondering | Barnsteenvondst, aanspoeldag 10-03-2026, keurdag 20-03-2026, beschadigd | 10-03-2026 (lid 2 above lid 3) | **10-03-2026** | absent | **09-04-2026** | consistent |
| case7-barnsteenvondst-zonder-keurdag | Barnsteenvondst, aanspoeldag 10-03-2026 | empty (lid 3 empty; lid 1 replaced) | **01-03-2026** (the initialisation) | absent | **31-03-2026** | consistent |
| case8-barnsteenvondst-opgave-wijkt-af | Barnsteenvondst, aanspoeldag 10-03-2026, keurdag 20-03-2026, opgegeven 18-03-2026 | 20-03-2026 | **18-03-2026** (the opgave) | **present / present / 20-03-2026** | **17-04-2026** | `controle opgave meldingsdatum` × B |
| case9-vondst-zonder-berekening | Vondst, nothing | empty (no writer) | **01-03-2026** (the initialisation) | absent | **31-03-2026** | consistent |
| case10-vondst-met-opgave | Vondst, opgegeven 07-03-2026 | empty | **07-03-2026** (the opgave) | absent (no computation) | **06-04-2026** | consistent |
| case11-doel-zelf-in-invoer | Strandvondst, aanspoeldag 10-03-2026, meldingsdatum 20-03-2026 | 12-03-2026 | **12-03-2026** (the group decides; the input is displaced) | absent | **11-04-2026** | consistent |
| case12-doel-zelf-zonder-feiten | Strandvondst, meldingsdatum 20-03-2026 | empty | **20-03-2026** (nothing decides: the initialisation's justification fails on the filled input; the input stands) | absent | **19-04-2026** | consistent |

case5, case6 and case7 are the ch. 7.10 composition on `berekende
meldingsdatum`: the Barnsteenvondst's own base replaces the inherited base
(case5, and case7, where it decides nothing and lid 1 does not come back), and
the inherited exception outranks the own base (case6). Every case pins
`uiterste afhaaldatum`, which no case supplies, so every case refutes the
do-nothing executor; for case12 that pin is the only one the raw input does not
already satisfy.

The same document without the marker (the marker and the two lid-6 rules left
out, the eight cases without an opgave, their pins on `berekende meldingsdatum`,
the characteristics and `controledag` left out) gives the same `meldingsdatum`
and `uiterste afhaaldatum` in all eight, in ref, clingo and the Drools exec
branch (`PLAYED 8`), measured 2026-09-30: `berekende meldingsdatum` carries per
type what `meldingsdatum` carries without the marker.

## Scenario document (`scenario.rgs`)

The twelve cases are written as twelve scenarios, one instance each, and the
expectations above are their expectation bullets. The frontend's `--cases` mode
lowers `bron.rgs` with `scenario.rgs` to the twelve case files and to the
`ir.json` of the production route, and gate 16 regenerates them byte for byte.

## Executors

ref and clingo replay all twelve cases exactly; the Drools exec branch plays all
twelve (`PLAYED 12`); the DRL-text branch refuses the document on its defaults
sentence (`EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no
defaults lowering`), as it refuses every defaults document (class (A) of
`DRL_CHANNEL` in `scripts/check-drools.sh`). Fifteen flipped counter-cases, each
one pinned expectation of a case turned to another value and kept outside the
repository, fail in all three: case1 `meldingsdatum` 10-03-2026, case2
12-03-2026, case3 12-03-2026, case3 consistent, case3 `controledag` empty, case4
inconsistent, case5 12-03-2026 (lid 1 not replaced), case6 20-03-2026 (the own
base above the inherited exception), case7 12-03-2026, case8 not ter controle,
case8 `controledag` 12-03-2026, case9 `meldingsdatum` empty, case10 01-03-2026,
case11 20-03-2026, case12 01-03-2026.

Under the no-op guard (gate 7) no case survives.
