# Golden corpus

One directory per golden: `ir.json` (the `uchar-ir-v1` document),
one or more `case*.json` files, a `NOTE.md` with the hand computation,
and — where a frontend carries the form — a source file (`source.rgs`,
authored against the Jutterswet in `corpus/jutterswet/`; `source.catala_en`;
or `source.flogic`) that must re-emit the committed IR byte-identically
(gates 9, 10, 14). Goldens without a source are hand-written IR for forms
no frontend produces yet. Every golden passes the full gate set: schema
round-trip (gate 2), verifier (gate 3), case schema (gate 4), exact
replay in the reference interpreter and Clingo (gates 5–6), and the
Drools leg (gate 12 — played, or refused with a named reason). See
`corpus/README.md` for the rights policy and `spec/IR-SPEC.md` for the WF
index. The `anchor/` subdirectory holds the hand-written cases with
absolute expected values — its own README carries the charter.

The table below covers the first five cases only; the directory has
grown far past it (inheritance, quantifier folds, defaults, temporal
folds, distribution — the delta register `spec/CONSTRUCTS.md` tracks
what landed when). Each golden's own `NOTE.md` is the reading guide.

| Case | Jutterswet basis | Exercises |
|---|---|---|
| `jutterswet-penningdrempel` | art. 3 jo. 7 (de vondstdrempel) | characteristic attribution, parameter reference, comparison |
| `jutterswet-vondsttelling` | art. 3a–3b (aanmelding en telling) | fact type + roles, count over a role, sum over a navigated attribute, kg units |
| `jutterswet-berging` | art. 9 (gezamenlijke berging) | allocation: proportional by weight, per-recipient maximum, floor rounding, undistributed rest; unit systems (€, kg) |
| `jutterswet-vondstnummer` | art. 3a (vondstnummer en aanmelding) | uniqueness over a key; consistency with a date-ordering comparison |
| `jutterswet-penningjaar` | art. 3b jo. 7 (het penningjaar; jaarlijkse vaststelling) | named timeline + facet adoption, timeline attribute, yearly parameters |

Coverage notes:

- `jutterswet-vondsttelling` exercises the count/sum *shape* of
  art. 3a–3b only: the penningjaar scoping of the telling and the
  stormvloeddag double count are not rendered (they need the timeline
  and day-kind machinery a later case adds), and the gewicht-som rule
  exists to exercise the `sum` aggregation, units, and the empty-source
  fallback — art. 3a records the gewicht but mandates no sum.
- `jutterswet-berging` parks the undistributed rest on the Berging
  itself, where art. 9 sends it to the eilandfonds: the v0 remainder
  target is an attribute of the allocation *source*, and no eilandfonds
  object exists in the case — a deliberate, recorded modelling
  approximation, not a faithful render of the destination.
- `jutterswet-vondstnummer` renders art. 3a's timing discipline as "de
  aanmelddatum is niet eerder dan de vondstdag"; the article's exact
  clause — aanmelding in the *penningjaar* of the vondstdag — needs a
  function-valued comparison (year-of) that the v0 consistency form
  cannot express. Recorded as a v0 limit (`spec/CONSTRUCTS.md` row 19),
  not silently approximated away.
- The v0 count-over-a-role encoding (a pseudo attribute reference naming
  the role, sentinel-typed) is deliberate v0 shape; the WF-034 index
  cell records the verifier exception, and `jutterswet-vondsttelling`
  is its witness.

## Case files

A golden may carry one or more `case*.json` files — the evaluation data
half of the case (inputs, parameter values, evaluation date, expected
outcomes) in the `uchar-case-v0` format (`spec/schema/`, gate 4). The
expectation values trace to the worked examples of
`spec/RUNTIME-SEMANTICS.md`; the reference interpreter (`ref/`) replays
every case file exactly — gate 5, the oracle guard of the plan's §8.

`jutterswet-penningjaar` carries the timeline declarations plus three
rules: the temporal total (the one v0-mirrored bridge from a timeline
to a scalar, `RUNTIME-SEMANTICS.md` ch. 3.6), the drempel comparison
over that scalar total, and a negated reading ("aanmeldplichtig indien
niet penninggerechtigd") — the corpus's first negative dependency edge,
witnessing WF-046's accept path (ch. 6). Its case observes the collapse
itself (totaal 14 and 3), the absent-timeline rule (a jutter without
the temporal attribute totals to leeg, so the ordering comparison is
false), and the non-transient negation outcomes. Case convention for
temporal data at this golden's admission: a `uchar-case-v0` value for a
timeline-valued attribute is the value of the `asOfDate` bucket. The
format has since gained multi-segment timelines — the later temporal
goldens (`jutterswet-getijdenwacht`, `jutterswet-muntbewaring` and
others) carry full segment lists. A temporal attribute read in a scalar
condition is still *refused* (ch. 3.3) in all executors; the
`Parameterset` layer this note once deferred to has since landed in the
spec (ch. 8). The
`jutterswet-vondsttelling` case exercises the count-over-a-role wart
(the weightless vondst still counts) and the declared empty-source
fallback ("of 0 als die er niet zijn", both for a jutter without
vondsten and for one whose only vondst has no gewicht). Note that a
*sum* cannot observably distinguish contributor-skipping from
coalescing-to-0 — that distinction is pinned by the min/max unit tests
of `ref/test/` (WV-1.2e), not by this case.

## The witness cabinet

`jutterswet-vormenkabinet` is a **verifier-only** golden: it carries no
case files (gate 5 lists it explicitly as not replayed) and exists to
witness, positively and verifier-clean, the well-formedness rules whose
constructs no executing document carries yet. The rule at admission:
when one of these constructs becomes executable, its witness moves into
an executing document and the cabinet shrinks — it is scaffolding for
the WF index, never a place for semantics.

That rule was applied for the first time on 2026-09-19, over the ten rows
the cabinet then held. **Four stayed**: the dimensioned attribute with its
cell read (WF-004/014/040), the variable-start timeline with its start
point (WF-016/032), the time window (WF-042), and the day-kind spelling
registry — WF-017's positive binding and WF-034 arm (s)'s identity half,
both of which read the declaration table and need no executor at all.
**Six left**, each to a named executing successor: the enumeration domain
with its label assignment (WF-006/039, the enum slice of WF-023) to
`jutterswet-keurstaat`; the sign-restricted literal (WF-039) to
`anchor/jutterswet-taxatie` and five others; the counting quantifier
(WF-041) to `jutterswet-wachtploeg` and eight further composite goldens,
with `jutterswet-reddingsvloot` on the quantifier fold; the two dated
versions of one rule name (WF-021) to `anchor/jutterswet-jutheffing`;
the whole-period qualifier over a time-dependent operand (WF-043) to
`jutterswet-keurploeg`, `jutterswet-ploegpremie`,
`jutterswet-reddingsvloot` and `jutterswet-sleepvloot`; and the day-kind
definition (WF-031) to `jutterswet-schouwkalender`. The cabinet's own
`NOTE.md` carries the measurement per row, including the one the card
that shrank it had expected to move and could not: the dimensioned
attribute's only other carrier, `jutterswet-seizoenvondst`, is itself
case-less, so that row has no *executing* successor yet.

Gate 5 currently lists **three** goldens as not replayed: the cabinet,
`jutterswet-seizoenvondst` and `jutterswet-seizoenvouw`.

`jutterswet-seizoenvondst` is a UNIT-bearing dimensioned attribute read
through a dimension cell (`DimensionCellRead`). Until L2 it was the
positive witness WF-091's cell-read arm had nowhere (`IR-SPEC.md` ch. 4.1,
honesty note 3); that arm left the wire with the cell read's own `type`
(B13). It carries no case for the same measured reason the cabinet does —
`ref`, clingo and the Drools backend each decline a cell read in their own
words, and `uchar-case-v0` has no per-cell input variant. Its `NOTE.md`
carries the three refusals verbatim.

`jutterswet-seizoenvouw` is the dimension fold on the same kind of
attribute: a sum, a maximum and a minimum whose `dimensionSelection` picks
the labels they fold, emitted by the RegelSpraak frontend since the card
frontend-emitterrestanten. It carries no case for the same reason, measured
per executor in its `NOTE.md`: `ref` faults on the selection, clingo has no
arm for the fold, and the Drools backend, which does carry the fold, refuses
any case that supplies the dimensioned attribute.

`jutterswet-tempototaal` is the route-split witness of `het totaal van`:
the set reading and the temporal reading of one operator over one unit
(`€/mnd`), side by side, so neither route can move unnoticed. It joined
the cabinet caseless — `backends/clingo` refused the set fold at the
time — and left it when clingo gained the form: it now carries its case
files and replays through gates 5–6. Its `NOTE.md` keeps the history.

A verifier-only golden is admitted ONLY when that measurement is in its
NOTE: silence there would make an unexecuted document look like an
executed one.

`jutterswet-postwacht` is the second pin bump's emitter golden
(toolchain `fe46e49`, 2026-08-18): a re-emittable source whose dump
carries all four extension families the emitter produced at that pin bump
(the ch. 4.10 `Let`/`BinderRef` cluster joined them at the let-lowering
card, 2026-09-01, and is not in this dump) — navigation
bindings on the scalar read and the navigated characteristic check
(and, witness by absence, NOT on the fold sources), `scopeRol` on the
parent-scoped uniqueness, and the whole-or-not symbolic fact-creation
block — executed end to end by gates 5–6 (navigated reads 15/7, the
navigated check, folds 2/10, the created vakbeseining relation counted
back as 2).

`jutterswet-strandgoed` (2026-08-19) is the aggregation-filter emitter
golden: a comparison-filtered sum ("die groter zijn dan 5", with the 0
fallback firing for the fold with no contributors) and a
characteristic-filtered count over a DERIVED characteristic (the filter
reads what another rule writes — the strata witness), executed by both
executors; a contributor with an empty filter operand drops out
(ch. 1.2d), which the clingo join encodes for free.

`jutterswet-getijdenwacht` (2026-08-19) is the gedurende-clip emitter
golden (WF-055): the v0 71-pattern verbatim — 10/month through 2023 and
11/month January–June 2024, clipped to the wachtplichtig periods
[2023-07-01, 2024-02-01) → 6×10 + 1×11 = 71 — beside the per-segment
comparison clip (> 10 keeps the 11-segment: 66) and the three zero
shapes (no periods, no timeline, nothing above the bound). Case
convention, recorded: a temporal characteristic's true-periods ride the
`characteristicTimelines` instance field (half-open, grid-aligned); a
gap means the characteristic does not hold there.

`jutterswet-penningrecht` (2026-08-24) is the frontend-diff emitter
golden (toolchain `332db4c`) for Jutterswet art. 3 jo. 5 — the
RegelSpraak-toolchain render of the defeasibility defaults that
`jutterswet-penningrecht-catala` renders through the Catala plugin and
`anchor/jutterswet-hardheid` pins by hand. This render carries no
prioritized defaults: the source writes two independent flat
`KENMERKTOEKENNING` rules
(`penninggerechtigd bij drempel`, >=12; `penninggerechtigd bij
hardheid`, the compound hardheid AND >=6), so no `prioritizedDefaults`
field appears — where the Catala dump carries it populated. At this
golden's admission the RegelSpraak emitter could not produce the field
at all; it has since learned the layered form
(`jutterswet-wrakrecht` carries populated `prioritizedDefaults` from
`source.rgs`), while this pair deliberately keeps the flat render: the
form difference over identical inputs and outcomes is what it pins. The
render is faithful on every input, not just the five cases: both rules
decide the same value (`waar`), so the ordered defaults collapse to a
disjunction and no default conflict is reachable — the pair thus isolates
a pure IR-form difference (two rules vs one `PrioritizedDefaults`) over
identical inputs and outcomes. Its five cases mirror the anchor's and
the Catala render's one-for-one; case3-stilte and case4-hardheidsgrens
are the two silence cases (no ground → nothing attributed), pinned as
no-op survivors beside their anchor and Catala twins.
