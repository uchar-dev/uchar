# jutterswet-vacuumwacht

*Since 2026-10-19 (card `zero-division-channel`) a filled dividend divided by
a numeric zero is leeg with a §9.5 fact of kind EF-005, no fault
(`RUNTIME-SEMANTICS.md` ch. 1.4 point 4). The case pins `consistent` since,
and the too-eager direction is the golden `at-most-reads-until-settled`. The
text below speaks of the fault.*

The **at-most true-stop witness** of `RUNTIME-SEMANTICS.md` ch. 5.10.

`at most d` stops in **two** directions: once the hits have exceeded *d*
(the outcome stands false) and once the unread members can no longer push
them past *d* (the outcome stands **true**). This golden pins the second,
at both of its interesting points.

*Provenance.* This golden replaces the red case `atmost-no-vacuous-true`,
which pinned the opposite reading under an earlier boundary. The
maintainer decision of 2026-09-07 removed that boundary — one rule, no
exception: evaluation stops as soon as the outcome stands — so the very
same document is now a **positive** outcome: the rule fires, the target is
written, and no member is read. A red case that has stopped faulting is
not a red case, so it moved here under a name that says what it now
claims.

## What it pins

```
Het vacuumteken van een Vacuumwacht moet gesteld worden op 1
indien hij aan ten hoogste twee van de volgende voorwaarden voldoet:
- het aandeel is groter dan 0            <- de deling
- zijn ploegomvang is gelijk aan 0.

Het stopteken van een Vacuumwacht moet gesteld worden op 1
indien hij aan ten hoogste twee van de volgende voorwaarden voldoet:
- zijn ploegomvang is groter dan 100
- zijn vangstwaarde is groter dan 100000
- het aandeel is groter dan 0            <- de deling
- zijn ploegomvang is gelijk aan 0.

Daarbij geldt (in beide regels):
    het aandeel is de vangstwaarde van de Vacuumwacht
        gedeeld door de ploegomvang van de Vacuumwacht
        naar beneden afgerond op 0 decimalen.
```

* **`vacuumteken` — the vacuum, *d* ≥ *n*.** Two members, threshold two:
  the hits can never exceed it, so the outcome stands true **before the
  first member**. Nothing is read at all — the division included. This is
  the at-most twin of the `atLeast`/`exactly` degenerate cases that
  `jutterswet-seinwacht` pins above the member count.
* **`stopteken` — the stop below the member count, *d* < *n*.** Four
  members, threshold two, with the division third. Once two members have
  failed, only two remain, so the hits can no longer exceed two: the
  outcome stands true at member 3 and the division is never reached.

## Hand-computed absolutes

| Vacuumwacht | ploegomvang | vangstwaarde | `vacuumteken` | `stopteken` | why |
|-------------|-------------|--------------|---------------|-------------|-----|
| B1 | 0 | 100 | **1** | **1** | leden 1–2 fail (0 ≯ 100; 100 ≯ 100000) → 0 hits, 2 unread, 0+2 ≤ 2 → stands true at member 3; the division is never read |
| B2 | 200 | 200 000 | **1** | **leeg** | leden 1–3 all hold (200 > 100; 200000 > 100000; ⌊200000/200⌋ = 1000 > 0) → 3 hits > 2 → stands **false** at member 4 |
| B3 | 200 | 0 | **1** | **1** | lid 1 holds, lid 2 fails, lid 3 read and fails (⌊0/200⌋ = 0 ≯ 0) → 1 hit, 1 unread, 1+1 ≤ 2 → stands true at member 4 |

`vacuumteken` is **1** for every input — that is exactly what vacuously
true means, and it is the pin: an executor that keeps only the false side
of the at-most trigger reads member 1 on `B1` and faults on the division.

`B2` and `B3` are the controls for `stopteken`. `B2` proves the false
side still fires and that member 3 *is* read when the prefix leaves the
outcome open; `B3` proves the true side can also fire *after* some
members have been read, not only on the empty prefix.

## The differential square

`ref` ≡ `clingo` over all six expectations (gate 5 ≡ gate 6), anchored to
the hand-written absolutes above; gate 12 replays them in Drools. Gate 10
re-emits `source.rgs` to a byte-identical `ir.json`. NOOP-REFUTING: five of
the six expectations are derived values absent from the raw input
(gate 7).
