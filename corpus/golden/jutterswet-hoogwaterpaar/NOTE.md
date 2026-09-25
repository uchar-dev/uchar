# jutterswet-hoogwaterpaar — hand-derivation of expected values

Own-authored Jutterswet material (strandjutten / getijden domain). A
`Getijdendag` carries two datum-tijd inputs — `ochtendhoogwater` and
`avondhoogwater` (morning and evening high water) — and two computed
datum-tijd targets:

- `eerstehoogwater` = **de eerste van** zijn ochtendhoogwater en zijn
  avondhoogwater  (`first`, the earliest instant);
- `laatstehoogwater` = **de laatste van** zijn ochtendhoogwater en zijn
  avondhoogwater  (`last`, the latest instant).

## The ordering (RUNTIME-SEMANTICS ch. 3.8 + ch. 1.2e)

A datum-tijd is one instant: its millisecond count since
1970-01-01T00:00:00 in zoneless civil time, split into the pair
`(D, MSOD)` — the epoch civil day `D` and the ms-of-day `MSOD` with
`0 <= MSOD < 86 400 000`. Total ms `= D·86 400 000 + MSOD`. Because
`MSOD` never reaches a full day, ordering by total ms **is** the
lexicographic ordering `(D, MSOD)`: the civil day dominates, the
ms-of-day only breaks ties on the same day. `first` returns the minimum
instant, `last` the maximum (`agg_eerste`/`agg_laatste` over
`scalar_cmp`; the fold returns the winning **value**, so ties are
value-neutral). An empty operand is skipped (ch. 1.2e); all-empty
derives nothing (leeg).

Epoch civil days (proleptic Gregorian era algorithm, ch. 3.8):

```
D(2026-03-15) = 20527
D(2026-03-16) = 20528
```

Ms-of-day = ((hh·3600 + mm·60 + ss)·1000):

```
06:12:00 -> 22 320 000
18:47:00 -> 67 620 000
23:50:00 -> 85 800 000
00:20:00 ->  1 200 000
```

## case1-zelfde-dag — the tie-break leg (same civil day, different ms-of-day)

Both inputs land on D = 20527 (2026-03-15); only the ms-of-day differs.

```
ochtendhoogwater 2026-03-15T06:12:00 -> (20527, 22 320 000)  total 1 773 555 120 000
avondhoogwater   2026-03-15T18:47:00 -> (20527, 67 620 000)  total 1 773 600 420 000
```

Same D, so stage 1 (extreme over the civil day) is a tie; stage 2 (the
extreme over the ms-of-day among that day's operands) decides:

- `first` = min = **2026-03-15T06:12:00**
- `last` = max = **2026-03-15T18:47:00**

This leg is load-bearing: without the ms-of-day tie-break the pair would
be ambiguous on the shared civil day.

## case2-andere-dag — the civil-day-first leg (different civil day)

Here the **earlier** civil day carries the **later** ms-of-day, so a
ms-of-day-first fold would be wrong.

```
ochtendhoogwater 2026-03-15T23:50:00 -> (20527, 85 800 000)  total 1 773 618 600 000
avondhoogwater   2026-03-16T00:20:00 -> (20528,  1 200 000)  total 1 773 620 400 000
```

Total ms: ochtend (1 773 618 600 000) < avond (1 773 620 400 000), so:

- `first` = min = **2026-03-15T23:50:00** (day 20527)
- `last` = max = **2026-03-16T00:20:00** (day 20528)

Load-bearing: the ochtend ms-of-day (85 800 000) is *larger* than the
avond ms-of-day (1 200 000); a fold that compared ms first would pick
avond as the earliest — wrong. The civil day must dominate.

## case3-leeg — the empty corners in one file

- Instance **H1**: `ochtendhoogwater` = 2026-03-15T06:12:00,
  `avondhoogwater` empty. The empty operand contributes no tuple, so the
  single present operand is both the min and the max:
  `first` = `last` = **2026-03-15T06:12:00**.
- Instance **H2**: both operands empty. No tuple derives; the fold is
  underivable — `eerstehoogwater` and `laatstehoogwater` stay **leeg**.

## no-op refutation (gate 7)

Every case writes at least one computed target that is absent from the
raw input (case1/case2: both targets on H1; case3: H1's two targets), so
a do-nothing executor fails each case. No survivor entry is needed.
