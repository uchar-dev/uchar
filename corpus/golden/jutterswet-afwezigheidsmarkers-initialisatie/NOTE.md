# jutterswet-afwezigheidsmarkers-initialisatie — an initialisation on a timeline with absence markers

An initialisation writes a timeline of a sum type: an attribute for every month with the absence markers of the
RegelSpraak dialect (`frontends/regelspraak/grammar/DIVERGENCES.md`, "Afwezigheidsmarkeringen";
`spec/RUNTIME-SEMANTICS.md` ch. 9.5), initialised on the marker `onbekend`. The initialisation is the lowest layer of
the target's stack per day (ch. 7.8): it decides the days on which no writer decides and the input reads no value, and
its value is a constructor of a literal on every day. Own Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 6d (opslag- en keurkosten per maand).** 1. De opslagkosten van een vondst bedragen in de maanden maart tot en
> met mei 2025 10; voor de andere maanden zijn zij onbekend, tenzij de strandvoogd ze heeft vastgesteld. 2. De
> keurkosten van een ongekeurde vondst zijn onbekend voor elke maand waarvoor de strandvoogd ze niet heeft vastgesteld.

## Source (`source.rgs`)

- `opslagkosten`, a whole number for every month or `onbekend` or `niet van toepassing`: 10 from 2025-03-01 up to
  2025-06-01, and the initialisation on `onbekend`.
- `keurkosten`, a whole number for every month or `onbekend`: the initialisation on `onbekend` "indien hij ongekeurd
  is", a condition that reads no timeline and gates the initialisation.

## Cases (hand-computed, absolute)

| Case | Input | Pinned |
|---|---|---|
| `case1-zonder-invoer` | V1 ongekeurd | opslagkosten `onbekend` on (−∞, 2025-03-01), `waarde(10)` on [2025-03-01, 2025-06-01), `onbekend` on [2025-06-01, +∞); keurkosten `onbekend` on all time |
| `case2-invoer` | V2 not ongekeurd; opslagkosten `niet van toepassing` on [2025-01-01, 2025-04-01); keurkosten `waarde(4)` on 2025 | opslagkosten `onbekend` on (−∞, 2025-01-01), the input `niet van toepassing` on [2025-01-01, 2025-03-01), `waarde(10)` in spring, `onbekend` from June; keurkosten the input `waarde(4)` on 2025 alone |
| `case3-ongekeurd-naast-invoer` | V3 ongekeurd; keurkosten `waarde(4)` on 2025 | opslagkosten as in case 1; keurkosten `onbekend` on (−∞, 2025-01-01), the input `waarde(4)` on 2025, `onbekend` on [2026-01-01, +∞) |

ref and the ASP backend play every case: the backend reads the opslagkosten through the ch. 7.7 desugaring as a
defaults group per day (ch. 7.11) and the keurkosten natively. The Drools backend refuses the golden on both branches
(DROOLS-103), and gate 12 lists it in its channel.
