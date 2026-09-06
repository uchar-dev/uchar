# jutterswet-schouwrooster — a day-kind unit spelling on the wire

The public witness for `dagsoortPerEenheid`, the map that couples a **unit
spelling** to a declared day kind (`spec/IR-SPEC.md` ch. 2.9 DayKind: the
`plural` field is the duration-unit spelling, and the registry is what a
consumer resolves a spelling through).

## What it declares

Two day kinds, `schouwdag` and `stormdag`, each defined by a pure date
condition over the day-kind reference `de dag` — a single date comparison,
so WF-031's purity clause holds (only dates, date components, literals and
other day kinds are read) and the day-kind reference graph is empty, so
WF-085 holds vacuously. Declaring a day kind registers **both** its
spellings, singular and plural, so the map carries four entries:

    schouwdag   -> schouwdag        stormdag   -> stormdag
    schouwdagen -> schouwdag        stormdagen -> stormdag

## Two shapes, both on purpose

- `schouwdagen` is **load-bearing**: it is the declared `eenheid` of the
  three `Strandmeester` attributes and of the `schouwdrempel` parameter, so
  the spelling reaches evaluation as a real unit — the `plus` of the
  berekening and the comparison of the kenmerktoekenning both run with it,
  in all three executors.
- `stormdagen` is **registered and never mentioned** by any expression.
  That is the ordinary shape of a day-kind registry entry, and a witness
  that means to carry the map has to carry it too.

## What is deliberately absent

The day-kind **duration** forms — `tijdsduur van A tot B in hele
<dagsoort>` and `datum ± N <dagsoort>` — are not here, by measurement.
Both are recorded flanks that no executor of gates 5 and 6 runs: the
reference interpreter faults at evaluation ("a day-kind tijdsduur is a
recorded flank, not executed"; likewise for the duration operand of date
arithmetic) and the ASP backend faults at translation with the same
reading. A golden carrying either form could not replay, so the executed
half of this witness is the unit spelling in ordinary arithmetic, and the
duration half stays a flank until the semantics chapter fixes day-by-day
day-kind reckoning for every executor.

## The pins, hand-computed

`schouwplicht = vaste schouwlast + extra schouwlast` (both in
`schouwdagen`); `schouwvrij` iff `schouwplicht < schouwdrempel`, strictly.
With `schouwdrempel = 20 schouwdagen`:

| case1 | vaste | extra | schouwplicht | `< 20` | schouwvrij |
|---|---|---|---|---|---|
| S1 | 12 | 3 | **15** | yes | present |
| S2 | 18 | 6 | **24** | no | absent |
| S3 | 20 | 0 | **20** | no (strict) | absent |

S3 is the boundary: `20 < 20` is false, so the kenmerk stays absent.

`case2` pins the coalescing half of `plus` (`RUNTIME-SEMANTICS.md` ch. 1.2:
"5 + leeg = 5") at a day-kind-unit position: S4 supplies only its vaste
schouwlast of 9, the extra one is leeg, so `schouwplicht = 9 + leeg = 9`
schouwdagen — a filled value, not leeg — and `9 < 20` makes S4 schouwvrij.
