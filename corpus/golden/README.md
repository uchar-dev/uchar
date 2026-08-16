# Golden corpus

One directory per case: `bron.rgs` (the RegelSpraak source, authored
against the Jutterswet — `corpus/jutterswet/`) and `ir.json` (its
`regelspraak-ir-v0` dump). Admission: gate 2 (schema-valid, byte-identical
round-trip) and gate 3 (verifier-clean) — see `corpus/README.md` for the
rights policy and `spec/IR-SPEC.md` for the WF index the cases witness.
The `anchor/` subdirectory holds the hand-written cases with absolute
expected values (no `bron.rgs`; extension fields admitted) — its own
README carries the charter.

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
  exists to exercise SOM, units, and the empty-source fallback — art. 3a
  records the gewicht but mandates no sum.
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
temporal data, recorded: a `uchar-case-v0` value for a timeline-valued
attribute is the value of the `asOfDate` bucket — the format carries no
multi-segment timelines yet. Deliberately absent, recorded: a temporal
attribute or a year-granular parameter read in a scalar condition is
*refused* (ch. 3.3, the §8.2 marker family — v0's backends refuse it
and `ref/` faults identically), so a vintage-selection case (a
2025/2026 drempel flip) is not expressible until ch. 8 specifies how a
rule reads a vintage; parameter-set data is ch. 8 (`Parameterset`)
scope. The
`jutterswet-vondsttelling` case exercises the count-over-a-role wart
(the weightless vondst still counts) and the declared empty-source
fallback ("of 0 als die er niet zijn", both for a jutter without
vondsten and for one whose only vondst has no gewicht). Note that a
*sum* cannot observably distinguish contributor-skipping from
coalescing-to-0 — that distinction is pinned by the min/max unit tests
of `ref/test/` (WV-1.2e), not by this case.
