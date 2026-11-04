# jutterswet-gevolg-buiten-de-beurt — a consequence that is not evaluated raises no fault

A defaults group on a timeline target decides per day
(`spec/RUNTIME-SEMANTICS.md` ch. 7.11), and a default's consequence is
evaluated only where its justification holds on a day of its turn. This golden
gives the consequence a read that would fault where it is evaluated, and keeps
it from being evaluated. Own Jutterswet-flavoured casus, own dates,
rights-free; the document is that of the worked example
`WV-7.11-gevolg-buiten-de-beurt`.

## Casus (own text)

> **Art. 3m (strandloon per maand).** 1. Het strandloon van een jutter bedraagt
> 5. 2. In afwijking van het eerste lid is het strandloon van een jutter die
> kustwacht is gelijk aan zijn kustloon. 3. In afwijking van het tweede lid
> bedraagt het strandloon van een jutter die winterwacht is 60.

## Source (`source.rgs`)

- `strandloon` and `kustloon`, whole numbers for every month; `kustwacht` and
  `winterwacht`, characteristics for every month.
- **strandloon** (lid 1): the base 5.
- **strandloon voor kustwachters** (lid 2): above it, "berekend worden als zijn
  kustloon indien hij kustwacht is".
- **strandloon in de winter** (lid 3): above that, 60 "indien hij winterwacht
  is".

## Case (hand-computed, absolute)

`case1-buiten-de-beurt` (asOfDate 2025-07-01): the case gives the kustloon of
both Jutters as the asOfDate-bucket scalar 40, which carries no period. A form
that reads the timeline whole and finds the bucket scalar faults (ch. 8.4,
EF-037) — where it is evaluated.

- J1, without kustwacht and winterwacht: the strandloon is **5** on all time.
  The default on the kustloon is justified on no day, so its consequence is not
  evaluated.
- J2, kustwacht and winterwacht in April 2025: **5** on (−∞, 2025-04-01),
  **60** on [2025-04-01, 2025-05-01), **5** on [2025-05-01, +∞). In April the
  winter default decides, so the kustwacht default is justified only on a day
  that is not its turn, and its consequence is not evaluated either.
- No fault, and the channel is consistent.

The same document with kustwacht from June to August 2025 evaluates the
consequence and faults: `corpus/red/jutterswet-emmerscalair-gevolg`.
