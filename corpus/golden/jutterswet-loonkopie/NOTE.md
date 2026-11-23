# jutterswet-loonkopie — the bare copy of a number timeline

A rule whose value is a read of another timeline of the same subject, without arithmetic: the timeline is read whole
(RUNTIME-SEMANTICS ch. 3.4) and written segment by segment, over all time or in the periods of a condition (ch. 3.10,
3.11). `corpus/golden/jutterswet-initialisatie-per-dag` copies only within a fixed window, and
`corpus/golden/jutterswet-loon-per-periode` computes under its condition; this golden holds the copy itself. Own
Jutterswet-flavoured casus, own dates, rights-free.

## Casus (own text)

> **Art. 4c (kustloon en stormloon).** 1. Het kustloon van een jutter is zijn basisloon. 2. Zolang een jutter
> stormwacht heeft, is zijn stormloon zijn basisloon.

## Source (`source.rgs`)

A Jutter with the characteristic stormwacht and the attributes basisloon, kustloon and stormloon, each `voor elke
maand`, and two rules:

- **kustloon** (lid 1): "Het kustloon van een Jutter moet berekend worden als zijn basisloon", the copy without a
  condition;
- **stormloon** (lid 2): "Het stormloon van een Jutter moet berekend worden als zijn basisloon gedurende de tijd dat
  hij stormwacht is", the copy in the periods of a characteristic timeline.

`ir.json` is the frontend's emission of `source.rgs` (gate 10), and the case files are the lowering of `scenario.rgs`
(gate 16), whose expectations are written by hand.

## Cases (hand-computed, absolute)

A copy carries the segments of its source, the gaps between them included; the condition cuts them to its periods.

| case | basisloon | stormwacht | kustloon | stormloon |
|---|---|---|---|---|
| case1-stormwacht-in-het-voorjaar | 10 on [2025-01-01, 2025-03-01), 12 on [2025-04-01, 2025-07-01) | [2025-02-01, 2025-05-01) | as the basisloon | 10 on [2025-02-01, 2025-03-01), 12 on [2025-04-01, 2025-05-01) |
| case2-zonder-stormwacht | 10 on [2025-01-01, 2025-07-01) | — | 10 on [2025-01-01, 2025-07-01) | no segment |
| case3-stormwacht-langer-dan-het-basisloon | 10 on [2025-03-01, 2025-06-01) | [2025-01-01, 2025-12-01) | 10 on [2025-03-01, 2025-06-01) | 10 on [2025-03-01, 2025-06-01): where the basisloon has no value, the copy has none |

ref, clingo and both branches of the Drools backend play the three cases.
