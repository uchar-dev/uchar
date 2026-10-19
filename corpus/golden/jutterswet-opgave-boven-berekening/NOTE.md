# jutterswet-opgave-boven-berekening — the opgave marker `(mag opgegeven worden)`

Exercises the RegelSpraak dialect's **opgave marker**
(`frontends/regelspraak/grammar/DIVERGENCES.md` §7): `(mag opgegeven worden)`
as the last element of an attribute declaration, lowered by the frontend to
the hardship pattern of `spec/proposals/PHASE4-DEFAULTS.md` §4.1 on structure
the wire already carries — an input attribute for the opgave, the authored
computation moved to an attribute of its own, one defaults group with the
opgave above the computation, and a characteristic plus a consistency that
record a deviating opgave on the §9.5 channel. Two attributes carry the
marker, one of them with an authored initialisation. Own Jutterswet-flavoured
casus, own numbers, rights-free.

## Casus (own text)

> **Art. 3d (vondstwaarde en bergloon).** 1. De waarde van een aangemelde
> vondst is de taxatie van de strandvoogd, verminderd met vijf euro.
> 2. Het bergloon van een vondst is twee euro per kilogram van het vastgestelde
> gewicht. 3. Is het gewicht niet vastgesteld, dan is het bergloon
> vijfentwintig euro. 4. De jutter kan bij de aanmelding de waarde en het
> bergloon opgeven; een opgegeven bedrag gaat voor het berekende. 5. Wijkt de
> opgegeven waarde af van de berekende, dan stelt de strandvoogd de vondst ter
> controle.

Amounts are in euros; the model computes them without a unit, because the
marker stands on a number without a unit only (DIVERGENCES.md §7).

## Source (`source.rgs`)

- `waarde` and `bergloon` of a Vondst carry the marker (lid 4).
- **waardering** (lid 1): `waarde` = `taxatie` verminderd met 5,00.
- **bergloon naar gewicht** (lid 2): `bergloon` = `gewicht` maal 2, indien
  het gewicht gevuld is.
- **standaardbergloon** (lid 3): `bergloon` geïnitialiseerd op 25,00.
- **controle bij afwijkende waarde** (lid 5): a Vondst is `ter controle`
  indien hij `afwijkende opgave waarde` heeft — a rule reading a
  characteristic the marker generates.

The condition on lid 2 is load-bearing. `maal` counts an empty operand as 0
(`RUNTIME-SEMANTICS.md` ch. 1.2: "5 × leeg = 0"), so without it the
computation gives 0 on a Vondst without a weight, it decides, and the
initialisation below it is never reached. Measured on a first draft without
the condition: ref `MISMATCH: V.bergloon = 0, expected 25` and clingo
`MISMATCH: V.bergloon = [0], expected 2500` on the cases without a weight.
`verminderd met` propagates an empty left operand (same table), so lid 1
needs no condition: without a taxatie `berekende waarde` stays empty.

## Lowering (the frontend's, per marked attribute X)

| Piece | Wire |
|---|---|
| 1 | attribute `opgegeven X` on Vondst, X's datatype, right after X; no writer, the case supplies it |
| 2 | attribute `berekende X` on Vondst, right after `opgegeven X`; the authored computing rule of X (`waardering`, `bergloon naar gewicht`) keeps its name, condition and statute annotation and now targets `berekende X` |
| 3 | `PrioritizedDefaults` `opgave boven berekening X` on X, validity always, from the top: the opgave default (justification the empty `all`, source `altijd`; consequence `opgegeven X`; source `opgegeven X`), the computation default (the same with `berekende X`), and for `bergloon` the initialisation default (`initialisation: true`, consequence 25,00, source the statute annotation `art. 3d lid 3`) — the initialisation stays on X and leaves `rules` |
| 4 | possessive characteristic `afwijkende opgave X`, attributed by the rule `afwijking opgave X` under an `all` of: `opgegeven X` filled, `berekende X` filled, `opgegeven X` notEqual `berekende X` — spelled as the frontend emits the same rule written by hand |
| 5 | consistency `controle opgave X`, assertion the characteristic check of `afwijkende opgave X`, negated; its source is the marked declaration and its line |

The generated characteristics follow `ter controle` in the characteristic
list; the generated rules, groups and consistencies follow the authored ones
in their lists. The reader of `waarde` elsewhere would keep reading `waarde`;
here the only other reader, lid 5, reads the generated characteristic.

## Case derivations (hand-computed, absolute)

Per target the highest default that decides wins: the opgave, else the
computation, else (for `bergloon`) the initialisation, whose justification is
"the target is empty in the input facts" (ch. 7.3). A default whose
consequence is empty falls through (ch. 7.1). When nothing decides, the target
keeps what the input holds (ch. 7.8, Reading A). The characteristic needs both
amounts filled and unequal; the consistency deposits one fact
`controle opgave X` × V exactly when the characteristic holds.

The lowering on `waarde` (no initialisation). On the `bergloon` side nothing
is given, so its computation is empty (no weight) and the initialisation
fills 25.00 in every case below.

| Case | Input | opgave | computation | waarde | berekende waarde | afwijkende opgave waarde / ter controle | channel |
|---|---|---|---|---|---|---|---|
| case1-geen-opgave | taxatie 40.00 | empty | 40.00 − 5.00 = 35.00 | **35.00** | 35.00 | absent / absent | consistent |
| case2-opgave-gelijk | taxatie 40.00, opgegeven waarde 35.00 | 35.00 | 35.00 | **35.00** | 35.00 | absent (equal) / absent | consistent |
| case3-opgave-wijkt-af | taxatie 40.00, opgegeven waarde 50.00 | 50.00 | 35.00 | **50.00** | 35.00 | **present / present** | `controle opgave waarde` × V |
| case4-opgave-zonder-feiten | opgegeven waarde 50.00 | 50.00 | empty (verminderd met propagates) | **50.00** | empty | absent (no computation) / absent | consistent |
| case5-niets | — | empty | empty | **empty** | empty | absent | consistent |
| case6-doel-zelf-in-invoer | taxatie 40.00, waarde 60.00 | empty | 35.00 | **35.00** (the group decides; the input is displaced) | 35.00 | absent | consistent |
| case7-doel-zelf-zonder-feiten | waarde 60.00 | empty | empty | **60.00** (nothing decides; the input stands) | empty | absent | consistent |

case5 also pins `bergloon` 25.00 with `berekende bergloon` empty and
`afwijkende opgave bergloon` absent: the initialisation proof's empty case.

The lowering on `bergloon`, whose initialisation stays on `bergloon` as the
lowest default. On the `waarde` side nothing is given, so `waarde` stays
empty.

| Case | Input | opgave | computation | initialisation | bergloon | berekende bergloon | afwijkende opgave bergloon | channel |
|---|---|---|---|---|---|---|---|---|
| case8-bergloon-berekend | gewicht 20.00 | empty | 20.00 × 2 = 40.00 | not reached | **40.00** | 40.00 | absent | consistent |
| case9-bergloon-opgave-wijkt-af | gewicht 20.00, opgegeven bergloon 50.00 | 50.00 | 40.00 | not reached | **50.00** | 40.00 | **present** | `controle opgave bergloon` × V |
| case10-bergloon-opgave-zonder-feiten | opgegeven bergloon 50.00 | 50.00 | empty (condition fails) | not reached | **50.00** | empty | absent | consistent |
| case11-bergloon-zelf-staat | bergloon 60.00 | empty | empty | justification fails: `bergloon` is filled in the input | **60.00** (the input stands) | empty | absent | consistent |

case9 pins `ter controle` absent: lid 5 reads the deviation of `waarde`
only, so a deviating bergloon reaches the channel and not the reader.
case12-beide-wijken-af combines case3 and case9 with the opgave 30.00 for
`bergloon`: `waarde` 50.00 (computed 35.00), `bergloon` 30.00 (computed
40.00), both characteristics and `ter controle` present, and two channel
facts, `controle opgave waarde` × V and `controle opgave bergloon` × V — the
two lowerings are independent.

Had the initialisation moved to `berekende bergloon` with the computation,
its justification would read an attribute that is never input and always
hold: case11 would give 25.00 over the input 60.00, and case10 would report a
deviation between the opgave 50.00 and 25.00.

## Executors

ref and clingo replay all twelve cases exactly; the Drools exec branch plays
all twelve (`PLAYED 12`) with the same values; the DRL-text branch refuses
the document on its defaults sentence, as it refuses every defaults document
(class (A) of `DRL_CHANNEL` in `scripts/check-drools.sh`). Thirteen flipped
counter-cases, each one pinned expectation of a case turned to its opposite
and kept outside the repository, fail in all three: case3 consistent, case3
`waarde` 35.00, case3 without the characteristic, case3 without `ter
controle`, case2 and case4 inconsistent, case5 `bergloon` empty, case6
`waarde` 60.00, case7 `waarde` empty, case9 `bergloon` 40.00, case10 and
case11 `bergloon` 25.00, and case12 consistent.

Under the no-op guard (gate 7) case11 alone survives: nothing decides and
the input stands, which is what it asserts. Every other case pins a computed
or initialised value that the input does not hold.
