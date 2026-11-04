# jutterswet-opgave-per-periode — the opgave marker on a timeline

Exercises the RegelSpraak dialect's **opgave marker**
(`frontends/regelspraak/grammar/DIVERGENCES.md` §7) on two attributes for every
month. The frontend lowers each to an input attribute for the opgave, the
authored writers moved to an attribute of their own, and one defaults group with
the opgave above the computation and, for the stormtoelage, the authored
initialisation below it. On a timeline target the group decides per day
(`spec/RUNTIME-SEMANTICS.md` ch. 7.11): on every day the highest default that
decides that day wins, a default whose consequence reads leeg on a day falls
through on that day, and the virtual input default reads the input of that day.
Own Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 3g (strandloon en stormtoelage per maand).** 1. Het strandloon van een
> jutter bedraagt in de maanden maart tot en met mei 2025 20. 2. De
> stormtoelage van een jutter bedraagt 30 in elke maand waarin hij stormwacht
> is, en anders 0. 3. De strandvoogd kan het strandloon en de stormtoelage voor
> een maand vaststellen; een vastgesteld bedrag gaat in die maand voor het
> berekende.

## Source (`source.rgs`)

- `strandloon` and `stormtoelage`, whole numbers for every month, carry the
  marker (lid 3); `stormwacht` is a characteristic for every month.
- **strandloon in het voorjaar** (lid 1): 20 from 2025-03-01 up to 2025-06-01,
  moved to `berekende strandloon`.
- **stormtoelage** (lid 2): 30 gedurende de tijd dat hij stormwacht is, moved
  to `berekende stormtoelage`.
- **geen stormtoelage** (lid 2): the initialisation on 0, the lowest default of
  the group on `stormtoelage`.

On a timeline the marker generates no deviation characteristic and no
consistency (`DIVERGENCES.md` §7): their conditions compare two timelines and
test a timeline for filled, which have no reading per period
(RUNTIME-SEMANTICS ch. 3.12 row 5).

## Cases (hand-computed, absolute)

| Case | Input | Pinned |
|---|---|---|
| `case1-opgave-heel-2025` | opgegeven strandloon 10 on 2025 | strandloon **10** on [2025-01-01, 2026-01-01); berekende strandloon 20 on [2025-03-01, 2025-06-01) |
| `case2-opgave-januari-februari` | opgegeven strandloon 10 on January and February 2025 | strandloon **10** on [2025-01-01, 2025-03-01), **20** on [2025-03-01, 2025-06-01), leeg elsewhere |
| `case3-zonder-opgave` | — | strandloon **20** on [2025-03-01, 2025-06-01); stormtoelage **0** on all time |
| `case4-invoer-op-het-doel` | strandloon 99 on 2025, opgegeven strandloon 10 on January and February | strandloon **10**, **20**, and **99** on [2025-06-01, 2026-01-01): the input of the target stands where no default above the virtual input default decides (ch. 7.8) |
| `case5-stormtoelage-per-dag` | stormwacht on [2025-10-01, 2026-01-01), opgegeven stormtoelage 12 in November 2025, stormtoelage 7 on January and February 2025 | stormtoelage **0** before 2025, **7** on [2025-01-01, 2025-03-01), **0** on [2025-03-01, 2025-10-01), **30** in October, **12** in November, **30** in December, **0** from 2026 |

Case1 and case2 are the worked example of RUNTIME-SEMANTICS ch. 7.11
(`ref/test` WV-7.11-opgave-per-dag). In case5 the initialisation default is
justified on the days the input leaves empty, so it does not decide January and
February, where the input 7 stands.

Every case plays in ref and in the ASP backend. The Drools backend refuses the
golden on both branches (DROOLS-271), and gate 12 lists it in its channel.
