# jutterswet-tempototaal — the two routes of "het totaal van", side by side

The **route-split witness** of `het totaal van`. One document carries all
three readings of one operator over one unit (`€/mnd`), so the emitter can
no longer move either route unnoticed (gate 10 re-emits `bron.rgs`
byte-identically):

| rule | operator | operand | operand type | result type |
|---|---|---|---|---|
| `bewaarlast` | `het totaal van` | `het bewaartarief van alle vondsten van de Jutter` (`set = true`, **not** timeline-declared) | `€/mnd` | **`€/mnd`** |
| `bewaarsom`  | `de som van`     | the same set read | `€/mnd` | `€/mnd` |
| `bewaargeld` | `het totaal van` | `zijn bewaarvergoeding per maand` (`set = false`, timeline-declared) | `€/mnd` | **`€`** |

Read the first two rows together: **the set reading of "het totaal van" is
an alias of "de som van"** — same operand, same result type, on the wire.
The alias covers the **result type only**, not the §4.20 operand contract:
`het totaal van` still demands a per-tijdseenheid operand where `de som
van` takes any unit. Measured on the same shape with a plain `€`
bewaartarief — `de som van` resolves clean, `het totaal van` says
`totaal van vereist een eenheid per tijdseenheid (§4.20 Tabel 30), kreeg
'EUR'`.

Read the first and third together: **the §4.20 noemer strip belongs to the
TIME AGGREGATION alone** — the same operator over the subject's own
timeline still drops the `/tijdseenheid` (Tabel 30 row 2, result
`Eenheid1`). The dividing line the predicate draws is the **non-temporal SET
reading**: only there is the strip suppressed. A non-set, non-temporal
rate read keeps the strip and is refused loudly by both executors (the
shape the toolchain's own rate-operand test pins), and a set read over
a **temporal** attribute is the §7.1 per-element
timeline fold (each element's tijdreeks summed over time first, then over
the set — the Drools transpilers' `rsTijdTotaal` path), so it integrates
too and keeps the strip. That fourth form is deliberately not in this
document; it is the subject of `backlog/new/20260904-ref-tijdtotaal.md`,
which has to close its value divergence first.

That is the maintainer decision of 2026-09-04, and this golden is what
makes it machine-checkable.

## The flank this closed

Before the decision the emitter stripped the noemer for `TOTAAL`
*unconditionally*, so the set reading was typed as something no executor
can produce. Measured on the minimal source of `bewaarlast` (one `€/mnd`
attribute, one set-fold), both targets, before and after:

```
target €/mnd, BEFORE:  ERROR (line 17): berekening: toekenning:
                       eenheden 'EUR/mnd' en 'EUR' zijn niet in elkaar om te rekenen
target €,     BEFORE:  emits; ref then says
                       EVALUATION FAULT: incompatible units: EUR vs EUR

target €/mnd, AFTER:   emits; ref says  oracle: PASS
target €,     AFTER:   ERROR (line 17): berekening: toekenning:
                       eenheden 'EUR' en 'EUR/mnd' zijn niet in elkaar om te rekenen
```

The fix moved a runtime fault to a compile-time diagnostic and made the
honest declaration the accepted one. On the wire it is one field:
`Functie{kind: TOTAAL}.type.eenheid.noemer` is now **held** when the
operand is a set read, and still **dropped** when it is not.

The strip itself is untouched, and so is every temporal golden: all 28
`bron.rgs` goldens re-emit byte-identically (gate 10). No corpus
`TOTAAL` node before this one carried a unit at all, which is exactly why
the flank was reachable in valid source and pinned nowhere.

## Why there is no case file

This is a **caseless golden** (see the "witness cabinet" section of
`corpus/golden/README.md`): gate 5 lists it as "NOT replayed, no case
file yet". That is a true report, and here is the measurement that makes
it true. Both routes were replayed through all three executors before the
golden was admitted.

**The set route** — `bewaarlast` + `bewaarsom`, with the hand-computed
absolutes below:

* `ref` — `oracle: PASS`.
* Drools backend — `PLAYED` on exactly those expectations.
* `backends/clingo` — refuses, verbatim:
  `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`.
  The ASP translator carries `Som` with `set = true` (the instance fold)
  and carries `Totaal` **only** for a temporal attribute read; a set-fold
  `Totaal` falls through to the catch-all in
  `backends/clingo/lib/translate.ml`. Isolated to one rule at a time, the
  `som` half passes (`clingo: PASS`) and the `totaal` half is the fault
  above — so the refusal is precisely the set-fold `Totaal`, nothing else
  in the document.

Gate 6 has no refusal channel: every case under `corpus/golden` must
replay through clingo. So a case file here would take gate 6 red, and
weakening the expectations to suit an executor is not on the table. The
case waits for the executor, not the other way round.

**The temporal route** — `bewaargeld` — is refused by `ref` as well, and
for a different reason worth recording:

```
EVALUATION FAULT: incompatible units: EUR vs EUR
```

`Timeline.totaal` multiplies each segment's value by a **unitless** whole-unit
count, so the `€/mnd` unit survives the collapse and the `€` target then
rejects it. In other words the §4.20 strip is, today, a static claim of the
frontend that `ref` implements on neither route. No gate saw
this before, because every temporal `TOTAAL` in the corpus is unitless (in
`jutterswet-getijdenwacht`, `jutterswet-penningjaar`, `synth2-clip-over-prorata`,
`synth2-clip-cond-over-prorata` and `anchor/jutterswet-dubbelwacht` the
operand carries no unit at all, so the strip is a no-op).

This is a `ref` **defect**, not an open question about the strip: the
Drools backend plays the same rule off the same wire. On `J1` with a
`bewaarvergoeding per maand` timeline of 10 €/mnd across 2024, Drools
answers `bewaargeld = 120` (10 × 12 months, `PLAYED 1`) — the integration
the strip's `€` result type describes. This golden makes the gap visible on
the wire; closing it belongs to `ref`, and is carded as
`backlog/new/20260904-ref-tijdtotaal.md`.

## Hand-computed absolutes, ready for the case

`J1` bewaart `V1` (bewaartarief 12), `V2` (7) and `V3` (no bewaartarief);
`J2` bewaart nothing.

| Jutter | `bewaarlast` (`het totaal van`) | `bewaarsom` (`de som van`) |
|---|---|---|
| J1 | 12 + 7 = **19** €/mnd | 12 + 7 = **19** €/mnd |
| J2 | **0** €/mnd — §4.20: an empty totaal is unconditionally 0 | **leeg** — §4.10: an empty som without a fallback |

`V3` contributes to neither: an absent element value drops out of both
folds. And note what the alias does *not* extend to: the empty-source rule
still differs between the two operators, which is why `J2` pins both. Those
four values plus `consistent` are the case this golden gets the day
`backends/clingo` carries a set-fold `Totaal`; `ref` and Drools agree on
them today.
