# jutterswet-dringende-melding — a quantifier over a written path

Exercises the quantifier fold over a range of two relations or more, written
in RegelSpraak and lowered by the frontend to a `QuantifierFold` with a
`sourcePath` (`IR-SPEC.md` ch. 4.5, WF-108 (c); the flat union of
`RUNTIME-SEMANTICS.md` ch. 3.4b, which ch. 3.9a shares;
`frontends/regelspraak/grammar/DIVERGENCES.md`, "A quantifier over a written
path"). Every quantifier here names its members two or three steps away from
the rule subject, to-one and to-many steps in four orders, with a
characteristic, a nominal characteristic, a comparison and a filled test as
the matrix, in an ordinary rule and in the exception of a defaults group. Own
Jutterswet-flavoured casus, own values, rights-free. `jutterswet-ketenkwantor`
pins the same node hand-written over a many-to-many relation; this golden
writes it from RegelSpraak.

## Casus (own text)

> **Art. 3l (dringende melding).** 1. Een vondst is zwaar als haar gewicht
> groter is dan 10. 2. Een melding is dringend als ten minste één vondst van de
> melder van de melding zwaar is. 3. Een melding heeft voorrang, tenzij ten
> minste één vondst van de melder een wrakstuk is. 4. Een melding is gewogen als
> van ten minste één weging van een vondst van de melder de weegdatum bekend is.
> 5. Een ploeg is zwaarbeladen als het gewicht van elke vondst van elke jutter
> van de ploeg groter is dan 10. 6. Een jutter is kistplichtig als ten minste
> twee kisten waarin zijn vondsten zijn ingepakt vol zijn; een kist waarin meer
> van zijn vondsten zijn ingepakt, telt één keer.

## Source (`source.rgs`)

- **zware vondst** (lid 1): the characteristic the quantifier of lid 2 tests,
  derived, so the fold reads a derived member.
- **dringende melding** (lid 2): "ten minste één van de vondsten van de melder
  van de Melding zwaar is" — to-one (`melder`), then to-many (`vondsten`); a
  characteristic as the matrix. R1 of the card.
- **voorrang** (lid 3): a defaults group. The base "Een Melding heeft
  voorrang" and the exception "Een Melding heeft geen voorrang indien ten
  minste één van de vondsten van de melder van de Melding een wrakstuk is" —
  the shape of bridge K6 of the card: a possessive characteristic concluded
  negatively, a quantifier over to-one then to-many in the justification, and a
  nominal characteristic ("het wrakstuk kenmerk", read as "is een wrakstuk") as
  the matrix.
- **gewogen melding** (lid 4): "de weegdatum van ten minste één van de wegingen
  van alle vondsten van de melder van de Melding gevuld is" — three steps,
  to-one, to-many, to-many (the shape of the startperiode of a person's
  zelfstandigen); a filled test as the matrix.
- **zwaarbeladen ploeg** (lid 5): "het gewicht van alle vondsten van alle
  jutters van de Ploeg groter is dan 10" — to-many then to-many, the quantifier
  `all`, a comparison as the matrix.
- **kistplicht** (lid 6): "ten minste twee van de kisten van de vondsten van de
  Jutter vol zijn" — to-many then to-one, a counting quantifier. Two vondsten
  of one jutter in one kist reach that kist twice; the union holds it once.

## Lowering (the frontend's)

Each quantifier becomes a `QuantifierFold` whose `sourcePath` is the written
walk, subject-outward, one `RoleBinding` per step naming the reached role:
`MeldingVanJutter.melder`, `JutterHeeftVondst.vondst` (lid 2 and lid 3), the
same and `VondstHeeftWeging.weging` (lid 4), `PloegHeeftJutter.jutter`,
`JutterHeeftVondst.vondst` (lid 5), and `JutterHeeftVondst.vondst`,
`KistBevatVondst.kist` (lid 6). The `elementType` is the type the walk lands
on, and the `test` reads that element as its subject.

## Hand computation (before any executor ran)

The contributors are the flat union of the reach, each element once
(RUNTIME-SEMANTICS ch. 3.4b). `atLeast` *n* over fewer than *n* contributors is
onwaar; `all` over no contributor is waar (ch. 3.9a). An ordering with a leeg
operand is onwaar (ch. 1.2). A characteristic the rules do not derive is
absent, which the case reads as "niet". Beside the pinned value, each table
gives the answer of a reading an executor could fall into: *range-free* folds
every element of the type in the world, ignoring the path; *occurrences*
counts an element once per route that reaches it.

`case1-dringend-en-voorrang.json` — lid 2 and lid 3. Vondsten: V1 (gewicht 12)
and V2 (5) of J1; V3 (3) and V4 (20, wrakstuk) of J2; V5 (4, wrakstuk) of J3;
J4 has none. Zwaar: V1 and V4.

| melding | melder → vondsten | zwaar among them | dringend | wrakstuk among them | voorrang | range-free dringend | range-free voorrang |
|---|---|---|---|---|---|---|---|
| M1 | J1 → {V1, V2} | V1 | **waar** | — | **waar** | waar | onwaar |
| M2 | J2 → {V3, V4} | V4 | **waar** | V4 | **onwaar** | waar | onwaar |
| M3 | J3 → {V5} | — | **onwaar** | V5 | **onwaar** | waar | onwaar |
| M4 | J4 → {} | — | **onwaar** | — | **waar** | waar | onwaar |
| M5 | no melder → {} | — | **onwaar** | — | **waar** | waar | onwaar |

M3 is the cell that separates the range from the population: J3 reaches a
wrakstuk but no zwaar vondst, so it is not dringend and has no voorrang. M4 and
M5 are the empty reach, at the second step and at the first: `atLeast` 1 is
onwaar there, and the exception does not fire, so the base gives voorrang.

`case2-gewogen-over-drie-stappen.json` — lid 4. Wegingen: W1 (03-05-2026) and
W2 (no weegdatum) of V1; W3 (no weegdatum) of V3; W4 (11-05-2026) of V6.

| melding | melder → vondsten → wegingen | filled weegdatum | gewogen | range-free |
|---|---|---|---|---|
| M1 | J1 → {V1, V2} → {W1, W2} | W1 | **waar** | waar |
| M2 | J2 → {V3} → {W3} | — | **onwaar** | waar |
| M3 | J3 → {V4} → {} | — | **onwaar** | waar |
| M4 | J4 → {} → {} | — | **onwaar** | waar |
| M5 | J5 → {V5, V6} → {W4} | W4 | **waar** | waar |

M5's weging hangs off the SECOND vondst of its melder, so a walk that stops at
the first intermediate answers onwaar there. M2 is the filled test failing on
a reached element; M3 and M4 are the empty reach at the third and the second
step.

`case3-zwaarbeladen-ploeg.json` — lid 5. Jutters: J1 and J2 of P1, J3 and J4 of
P2, J5 of P3, J6 of P5; P4 has none. Vondsten: V1 (12) and V2 (15) of J1, V3 (20)
of J2, V4 (12) of J3, V5 (4) of J4, V6 (no gewicht) of J6.

| ploeg | jutters → vondsten (gewicht) | all > 10 | zwaarbeladen | range-free |
|---|---|---|---|---|
| P1 | {J1, J2} → {V1 12, V2 15, V3 20} | yes | **waar** | onwaar |
| P2 | {J3, J4} → {V4 12, V5 4} | V5 fails | **onwaar** | onwaar |
| P3 | {J5} → {} | vacuous | **waar** | onwaar |
| P4 | {} → {} | vacuous | **waar** | onwaar |
| P5 | {J6} → {V6 leeg} | V6 fails (leeg) | **onwaar** | onwaar |

P3 and P4 are the empty union, reached at the second and at the first step:
both read waar, as an `all` over no contributor does. A reading that took the
empty set for onwaar turns both.

`case4-kistplicht-telt-elke-kist-eenmaal.json` — lid 6. Kisten: K1, K3 and K4
vol, K2 not. Vondsten: V1, V2 (both in K1) and V3 (in K2) of J1; V4 (in K1) and
V5 (in K3) of J2; V6 (in K4) and V7 (no kist) of J3; J4 has none.

| jutter | vondsten → kisten | union | vol in the union | kistplichtig (≥ 2) | occurrences | range-free |
|---|---|---|---|---|---|---|
| J1 | {V1, V2, V3} → K1, K1, K2 | {K1, K2} | 1 | **onwaar** | 2 → waar | 3 → waar |
| J2 | {V4, V5} → K1, K3 | {K1, K3} | 2 | **waar** | 2 → waar | 3 → waar |
| J3 | {V6, V7} → K4 | {K4} | 1 | **onwaar** | 1 → onwaar | 3 → waar |
| J4 | {} | {} | 0 | **onwaar** | 0 → onwaar | 3 → waar |

J1 is the separating cell for the union: K1 is reached along V1 and along V2
and counts once, so the count is 1 and the threshold of 2 is not met; an
executor that counted occurrences would answer 2. K1 is shared with J2 as
well, so the count is a claim about each jutter's own range: per jutter the
union gives 1 and 2, on opposite sides of the threshold, in one world.

Every case carries an expectation a do-nothing executor fails (gate 7): M1
dringend and with voorrang, M1 gewogen, P1 zwaarbeladen, J2 kistplichtig.

## Executors

Measured 2026-10-20 on the card branch, after the hand computation above was
committed: the frontend writes `ir.json` and the four case files from
`source.rgs` and `scenario.rgs` with no diagnostic (gate 16; `ir.json` is the
same from `source.rgs` alone, gate 10), the verifier passes the document, and
ref, clingo and both Drools branches answer every expectation as computed
(ref and clingo PASS on 10, 5, 5 and 4 expectations; the exec-model and the
DRL-text branch PLAYED 4). The reference runner's `--noop` mode fails every
case, on M5's voorrang, M5's gewogen, P4's zwaarbeladen and J2's kistplichtig.
