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
document; `ref` learned it separately (`backlog/done/20260904-ref-tijdtotaal.md`,
which closed its 0-vs-150 value divergence). **It has its own golden since
2026-09-05: `corpus/golden/jutterswet-tempovouw`**, the same operator and the
same `€/mnd` operand with the attribute declared `voor elke maand`, pinned at
48 € over three members. The two goldens are each other's counterexample on the
strip: here the non-temporal set reading keeps `€/mnd`, there the temporal one
drops to `€`.

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
`Aggregation{kind: TOTAAL}.type.unit.denominator` is now **held** when the
operand is a set read, and still **dropped** when it is not.

The strip itself is untouched, and so is every temporal golden: all 28
`bron.rgs` goldens re-emit byte-identically (gate 10). No corpus
`TOTAAL` node before this one carried a unit at all, which is exactly why
the flank was reachable in valid source and pinned nowhere.

## Why there was no case file — and when it arrived

This was a **caseless golden** (the "witness cabinet" section of
`corpus/golden/README.md`) from its admission until **2026-09-05**, when
`backends/clingo` learned the set fold and `case.json` landed. The
subsections below are the measurement that made the caselessness true —
recorded as it stood, in the past tense it now belongs to — and the
closing one records what changed.

**The set route** — `bewaarlast` + `bewaarsom`, with the hand-computed
absolutes below:

* `ref` — `oracle: PASS`.
* Drools backend — `PLAYED` on exactly those expectations.
* `backends/clingo` — refused, verbatim:
  `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`.
  The ASP translator carries `Som` with `set = true` (the instance fold)
  and carries `Totaal` **only** for a temporal attribute read; a set-fold
  `Totaal` falls through to the catch-all in
  `backends/clingo/lib/translate.ml`. Isolated to one rule at a time, the
  `som` half passes (`clingo: PASS`) and the `totaal` half is the fault
  above — so the refusal is precisely the set-fold `Totaal`, nothing else
  in the document.

Gate 6 has no refusal channel: every case under `corpus/golden` must
replay through clingo. So a case file would have taken gate 6 red, and
weakening the expectations to suit an executor was never on the table.
The case waited for the executor, not the other way round.

**The temporal route** — `bewaargeld` — was refused by `ref` as well when
this golden was admitted, and that refusal is what carded the fix:

```
EVALUATION FAULT: incompatible units: EUR vs EUR
```

`Timeline.totaal` multiplied each segment's value by a **unitless**
whole-unit count, so the `€/mnd` unit survived the collapse and the `€`
target rejected it. The §4.20 strip was a static claim of the frontend
that `ref` performed on neither route. No gate saw it, because every
other temporal `TOTAAL` in the corpus is unitless (in
`jutterswet-getijdenwacht`, `jutterswet-penningjaar`, `synth2-clip-over-prorata`,
`synth2-clip-cond-over-prorata` and `anchor/jutterswet-dubbelwacht` the
operand carries no unit at all, so the strip is a no-op).

**Closed 2026-09-04.** `ref` now performs the integration on both
timeline folds: the whole-unit count is a **duration in the grid's own
unit**, so a denominator the document's unit systems connect to that
grid cancels through the declared factors and the total carries the
numerator (RUNTIME-SEMANTICS ch. 3.6, "Units, the explicit rate
operand"). Here denominator and grid are the same `mnd`, so the factor
is 1 and the number is the plain whole-month count; a foreign but
convertible denominator (`€/jr` where `jr = 12 mnd`) scales instead, and
a denominator connected to nothing month-sized is refused loudly, naming
both units. On `J1` with a per-month `bewaarvergoeding per maand` of
10 €/mnd, `bewaargeld` is `10 €` over a one-month asOfDate bucket and
**120 €** over a full 2024 timeline — the value the Drools backend plays
off the same wire (`PLAYED 1`). The unit fault above is gone; the
non-unit fence on a case-supplied full timeline under the *no-filter*
route (the asOfDate-bucket convention carries that total) is older than
this golden and still stands.

**`ref` and clingo still disagreed on which forms they carry**, which is
why the case file kept waiting. Measured on this document after the
fix:

* the whole document, `bewaargeld` bucket case — `ref`: `oracle: PASS`;
  `backends/clingo`:
  `TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)`
  (still the set-fold `Totaal` of `bewaarlast`).
* `bewaargeld` isolated into its own document — `ref`: `oracle: PASS`;
  `backends/clingo`:
  `TRANSLATION FAULT: non-identical units under the write to "bewaargeld"
  are not translated (ref restates with an exact conversion factor this
  integer encoding does not carry)`. The ASP layer carries the temporal
  `Totaal` only where the write's unit is identical to the value's, and
  the whole point of this fold is that it is not.

So clingo refused **both** routes of this document, each for its own
reason, and the golden stayed caseless.

### Closed 2026-09-05: the case arrived

`backends/clingo` now carries both routes, and `case.json` sits beside
this file. What had to land together, because either alone leaves the
golden caseless:

* **the set fold**, in one branch with `de som van` — the alias of the
  first table is an alias in the translator too, and the empty set is
  the only difference (a totaal is 0 unconditionally, which is the ASP
  `#sum` over no tuples; a som without a fallback stays underivable, ref's
  leeg). The same branch carries the **§7.1 per-element timeline fold**
  for a temporal element attribute: each member's timeline collapses over
  time first — segment value × whole grid units, the calendar entering as
  data from the shared timeline algebra — and only those scalars fold over
  the set. A member that brings only an asOfDate-bucket scalar is refused
  by name, exactly as `ref` refuses it: the integration runs per member,
  so every member must bring its own horizon. One residual edge, and it is
  a **class**, not a case: any narrowing that is undecidable at translation
  time. Measured 2026-09-05 on two of them — a `die/dat` filter that drops
  the bucket-scalar member, and a rule condition that excludes every
  subject — both verifier-PASS, both `ref: oracle: PASS`, and both refused
  by the ASP translator, which cannot know at translation time that the
  member never reaches the fold. The refusal is conservative, not a verdict
  on the document.
* **the noemer strip at the write**, which is what made the third row
  translate. The effective unit of a time aggregation is the operand's
  unit *without its denominator* — the §4.20 Tabel 30 result — so the
  existing write fence sees `€` against a declared `€` and lets it
  through, with no unit conversion added anywhere. The strip follows the
  INTEGRATION, not the operator: the non-temporal set fold of row 1 keeps
  its `€/mnd`, which is exactly the alias this document pins. Only a
  denominator identical to the grid unit cancels in the integer encoding;
  a convertible-but-foreign one (`€/jr` over a month grid, factor 1/12) is
  refused loudly, naming both units, never rounded.

The case pins the four hand-computed absolutes below plus the temporal
route this NOTE also measures — `J1.bewaargeld = 10 €` over the
asOfDate month bucket of a `10 €/mnd` rate, `J2.bewaargeld` leeg (an
absent timeline totals to leeg) — and `consistent`. Seven expectations,
replayed identically by `ref`, `backends/clingo` and the Drools backend.

What stays refused, in both executors: a `minimum`/`maximum` over a
temporal set attribute — a ranking fold has no defined moment to rank at.

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
four values plus `consistent` are what `case.json` pins: `backends/clingo`
gained the set-fold `Totaal` on 2026-09-05 and the case landed with it, so
all three executors now replay them.
