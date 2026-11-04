# jutterswet-gevolg-buiten-de-beurt-som — the operand of a sum that faults in a reading

The document of `jutterswet-gevolg-buiten-de-beurt`, with the consequence of
the kustwacht default "zijn kustloon plus 1" instead of "zijn kustloon". The
session computes the same: the consequence is not evaluated
(`spec/RUNTIME-SEMANTICS.md` ch. 7.11). The reading of ch. 5.12 reads it all
the same, and there the sum passes on the fault of its left operand. Own
Jutterswet-flavoured casus, own dates, rights-free; the document is that of the
`ref/` test `WV-5.12-default-per-dag-gevolg-faalt-som`.

## Casus (own text)

> **Art. 3m (strandloon per maand).** 1. Het strandloon van een jutter bedraagt
> 5. 2. In afwijking van het eerste lid is het strandloon van een jutter die
> kustwacht is gelijk aan zijn kustloon vermeerderd met 1. 3. In afwijking van
> het tweede lid bedraagt het strandloon van een jutter die winterwacht is 60.

## Case (hand-computed, absolute)

`case1-buiten-de-beurt` is the case of `jutterswet-gevolg-buiten-de-beurt`: the
kustloon of both Jutters is the asOfDate-bucket scalar 40, J2 is kustwacht and
winterwacht in April 2025.

- J1: the strandloon is **5** on all time.
- J2: **5** on (−∞, 2025-04-01), **60** on [2025-04-01, 2025-05-01), **5** on
  [2025-05-01, +∞). The kustwacht default is justified in April only, where the
  winter default decides, so its consequence is not evaluated.
- No fault, and the channel is consistent.

## The reading (ch. 5.12)

The kustwacht default is at its turn outside April and justified in April, so
its consequence is read on J2 and faults on the bucket scalar (EF-037). The sum
passes the fault on and its right operand is not reached:

```
/rules/prioritizedDefaults/0/defaults/1 "J2" 0 CharacteristicCheck [[2025-04-01 2025-05-01 empty]]
/rules/prioritizedDefaults/0/defaults/1 "J2" 1 Binary fault EF-037
/rules/prioritizedDefaults/0/defaults/1 "J2" 2 AttributeReference fault EF-037
/rules/prioritizedDefaults/0/defaults/1 "J2" 3 Literal unreached
/rules/prioritizedDefaults/0/defaults/1 "J2" HOLDS
```

Gate 17 holds the trace of `ref/` and the readings of both branches of the
Drools backend to these lines.
