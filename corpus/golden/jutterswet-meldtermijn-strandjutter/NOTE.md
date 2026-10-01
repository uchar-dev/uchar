# jutterswet-meldtermijn-strandjutter — a deviation for a subtype under the base of its supertype

Exercises the RegelSpraak dialect's `Afwijking van`
(`frontends/regelspraak/grammar/DIVERGENCES.md` §4) written for a subtype
under a base rule of its supertype: the base computes `meldtermijn` for every
Jutter, and the deviation sets it for a Strandjutter with a vergunningseinde.
The frontend lowers the deviation as the Strandjutter's own exception without
an own base: a defaults group on the Strandjutter whose one default is a
group-default (`spec/RUNTIME-SEMANTICS.md` ch. 7.9). A group-default is no
base, so the composition of ch. 7.10 keeps the Jutter's base below the
Strandjutter's own exception, and a Strandjutter without a vergunningseinde
falls through to it. Own Jutterswet-flavoured casus and own dates.

## Casus (own text)

> **Art. 3h (meldtermijn van een vondst).** 1. Een jutter meldt zijn vondst
> binnen zes maanden na de dag van de vondst. 2. In afwijking van het eerste
> lid eindigt de meldtermijn van een strandjutter op de dag waarop zijn
> vergunning eindigt.

## Source (`bron.rgs`)

`Strandjutter is een Jutter`; `meldtermijn` and `vinddag` are declared on the
Jutter, `vergunningseinde` on the Strandjutter.

- **meldtermijn** (lid 1), on the Jutter: `meldtermijn` = `vinddag` plus 6 mnd
  indien zijn vinddag gevuld is.
- **meldtermijn tot het vergunningseinde** (lid 2), `Afwijking van meldtermijn`,
  on the Strandjutter: `meldtermijn` = `vergunningseinde` indien zijn
  vergunningseinde gevuld is. An `Afwijking van` widens: its default carries its
  own condition only.

## Lowering (the frontend's)

| Group | Subject | Defaults, highest first | Source |
|---|---|---|---|
| `meldtermijn` | Jutter | the base (lid 1) | `art. 3h lid 1` |
| `meldtermijn voor Strandjutter` | Strandjutter | one group-default (no justification, no consequence, source `Afwijking van meldtermijn`) holding one ordered exception group (source `Afwijking van meldtermijn`) with lid 2 (source `art. 3h lid 2`) | `eigen uitzonderingen van Strandjutter; de basis 'meldtermijn' is geërfd van Jutter` |

Both groups carry the validity of the base (always). No rule stays in
`rules`.

## The stacks per concrete type (ch. 7.10)

| concrete type | stack, highest first | reason |
|---|---|---|
| Jutter | lid 1 | its own group |
| Strandjutter | the group-default (lid 2), lid 1 | the own group-default is block 1 (own exceptions); it is no base, so block 3 is the inherited base lid 1 |

Written as one ordinary default on the Strandjutter, lid 2 would be the
Strandjutter's own base and would replace lid 1 (ch. 7.10, block 3): T below
would stay empty. Measured 2026-10-01 on that IR, verifier-clean: ref
`MISMATCH: T.meldtermijn = leeg, expected 2026-09-10`, clingo `T.meldtermijn
is underived (leeg)`, the Drools exec branch `werkelijk leeg`.

## Case derivations (hand-computed, absolute)

Per instance the highest default that decides wins; a default whose
consequence is empty falls through (ch. 7.1). The date sum: 10-03-2026 plus
6 maanden = 10-09-2026 (the day of the month exists in September).

| Case | Instance and input | `meldtermijn` | why |
|---|---|---|---|
| case1-meldtermijn-over-de-erflijn | Jutter J, vinddag 10-03-2026 | **10-09-2026** | lid 1 |
| | Strandjutter S, vinddag 10-03-2026, vergunningseinde 01-06-2026 | **01-06-2026** | lid 2, the own exception above the inherited base |
| | Strandjutter T, vinddag 10-03-2026 | **10-09-2026** | lid 2 decides nothing (no vergunningseinde); the inherited base lid 1 decides |
| case2-vergunningseinde-zonder-vinddag | Strandjutter S2, vergunningseinde 01-06-2026 | **01-06-2026** | lid 2: an `Afwijking van` carries its own condition only, so it decides without a vinddag |
| | Jutter J2, nothing | **empty** | lid 1 decides nothing; the input holds nothing |

Both cases expect `consistent`. Each case pins a value the input does not
carry, so each refutes the do-nothing executor.

## Scenario document (`scenario.rgs`)

The two cases are written as two scenarios. The frontend's `--cases` mode
lowers `bron.rgs` with `scenario.rgs` to the two case files and to the
`ir.json` of the production route, and gate 16 regenerates them byte for
byte; `bron.rgs` alone emits the same `ir.json` (gate 10).

## Executors

ref and clingo replay both cases exactly; the Drools exec branch plays both
(`PLAYED 2`); the DRL-text branch refuses the document on its defaults
sentence (`EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has
no defaults lowering`), as it refuses every defaults document (class (A) of
`DRL_CHANNEL` in `scripts/check-drools.sh`). Four flipped counter-cases, each
one pinned expectation turned and kept outside the repository, fail in all
three: T empty (the deviation as an own base), S 10-09-2026 (the base above
the deviation), J empty (the base moved to the subtype), S2 empty (the
restrictive reading of an `Uitzondering op`).
