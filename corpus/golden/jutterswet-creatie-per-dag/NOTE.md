# jutterswet-creatie-per-dag — the value of a making on a timeline, per day

The value a creation gives a time-dependent attribute of the object it makes is that object's input per day
(`spec/RUNTIME-SEMANTICS.md` ch. 5.5, 7.8, rank 2 of the stack): a scalar holds on every day, a timeline per period; a
rule that decides a day goes before it, and an initialisation fills the days it leaves leeg. ALEF's typesystem admits
the form and ALEF does not execute it, so the reading is uchar's own (decided by the owner, 2026-11-05). Own
Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 5c (meldingen per maand).** 1. Elke jutter doet een melding. 2. De melding vermeldt voor elke maand een
> meldwaarde en een seizoenswaarde van 7, en een meldloon, een seizoensloon en een aanvulloon gelijk aan het strandloon
> van de jutter in die maand. 3. De seizoenswaarde en het seizoensloon bedragen in de maanden maart tot en met augustus
> 2025 20. 4. Het aanvulloon bedraagt 5 in een maand waarvoor het strandloon niets vermeldt. 5. De totalen van een
> jutter zijn per maand de som van die bedragen over zijn meldingen.

## Source (`source.rgs`)

- **meld de jutter**: a Jutter has a Melding with meldwaarde 7, meldloon his strandloon, seizoenswaarde 7,
  seizoensloon his strandloon and aanvulloon his strandloon; every member is a whole number for every month.
- **seizoenswaarde** and **seizoensloon**: 20 from 2025-03-01 up to 2025-09-01, writers beside the value of the making.
- **standaardaanvulloon**: the initialisation of the aanvulloon on 5.
- Five rules write the totals of a Jutter, each the sum of one member over his meldingen, per month (ch. 3.5).

## Cases (hand-computed, absolute)

| Case | Input | Pinned |
|---|---|---|
| `case1-strandloon-tot-juli` | strandloon 10 on [2025-01-01, 2025-07-01) | meldwaardetotaal **7** on all time; meldloontotaal **10** on [2025-01-01, 2025-07-01); seizoenswaardetotaal **7** on (−∞, 2025-03-01), **20** on [2025-03-01, 2025-09-01), **7** on [2025-09-01, +∞); seizoensloontotaal **10** on [2025-01-01, 2025-03-01), **20** on [2025-03-01, 2025-09-01); aanvulloontotaal **5** on (−∞, 2025-01-01), **10** on [2025-01-01, 2025-07-01), **5** on [2025-07-01, +∞) |
| `case2-zonder-strandloon` | — | meldwaardetotaal **7**; meldloontotaal leeg on every day; seizoenswaardetotaal **7**, **20**, **7**; seizoensloontotaal **20** on [2025-03-01, 2025-09-01); aanvulloontotaal **5** on all time |

The meldwaarde, the meldloon, the seizoenswaarde and the seizoensloon of case 1 are the four creation cases of the
measurement in ALEF (a scalar value, a value per month, and each beside a rule that writes part of the year).

ref and the ASP backend play every case. The Drools backend refuses the golden on both branches (DROOLS-118), and gate
12 lists it in its channel.
